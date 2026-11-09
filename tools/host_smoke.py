#!/usr/bin/env python3
"""Offline model smoke test on a HOST CPU; never a Samsung device benchmark."""
import argparse
import json
import platform
import re
import resource
import time
import wave
from pathlib import Path

import numpy as np
import onnxruntime as ort
import vosk


def distance(a, b):
    row = list(range(len(b) + 1))
    for i, x in enumerate(a, 1):
        nxt = [i]
        for j, y in enumerate(b, 1):
            nxt.append(min(nxt[-1] + 1, row[j] + 1, row[j-1] + (x != y)))
        row = nxt
    return row[-1]


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("wav", type=Path)
    parser.add_argument("--output", type=Path, required=True)
    args = parser.parse_args()
    root = Path(__file__).resolve().parents[1]
    with wave.open(str(args.wav)) as wav:
        assert (wav.getframerate(), wav.getnchannels(), wav.getsampwidth()) == (16000, 1, 2)
        samples = np.frombuffer(wav.readframes(wav.getnframes()), dtype="<i2")
    options = ort.SessionOptions()
    options.intra_op_num_threads = options.inter_op_num_threads = 1
    vad = ort.InferenceSession(str(root / "app/src/main/assets/silero.onnx"), options,
                               providers=["CPUExecutionProvider"])
    state = np.zeros((2, 1, 128), dtype=np.float32)
    context = np.zeros((1, 64), dtype=np.float32)
    def probability(frame):
        nonlocal state, context
        x = np.concatenate([context, frame.reshape(1,512).astype(np.float32)/32768], axis=1)
        p, state = vad.run(None, {"input": x, "state": state, "sr": np.array(16000, dtype=np.int64)})
        context = x[:, -64:]
        return float(p[0,0])
    silence_probabilities = [probability(np.zeros(512,dtype=np.int16)) for _ in range(100)]
    assert max(silence_probabilities) < .6, "Silence falsely detected as speech"
    model = vosk.Model(str(root / "app/src/main/assets/model-de"))
    recognizer = vosk.KaldiRecognizer(model,16000)
    padded = np.concatenate([np.zeros(16000,dtype=np.int16),samples,np.zeros(16000,dtype=np.int16)])
    probabilities, vad_ms, asr_ms, results = [], [], [], []
    started = time.perf_counter()
    for start in range(0,len(padded)-511,512):
        frame = padded[start:start+512]
        before = time.perf_counter(); probabilities.append(probability(frame))
        vad_ms.append((time.perf_counter()-before)*1000)
        before = time.perf_counter()
        if recognizer.AcceptWaveform(frame.astype("<i2").tobytes()):
            results.append(json.loads(recognizer.Result())["text"])
        asr_ms.append((time.perf_counter()-before)*1000)
    results.append(json.loads(recognizer.FinalResult())["text"])
    elapsed = time.perf_counter()-started
    text = " ".join(x for x in results if x)
    assert sum(p >= .6 for p in probabilities) >= 10, "German speech not detected"
    assert len(text.split()) >= 6, "German ASR produced too few words"
    reference = (root / "tests/fixtures/de-reference.txt").read_text().strip()
    normalize = lambda s: re.findall(r"\w+",s.lower())
    report = {
        "scope":"HOST CPU, synthetic clean German voice; no Android, speaker echo or S24 measurement",
        "host":platform.machine(), "onnxruntime":ort.__version__, "vosk_python":"0.3.45",
        "vad":"Silero 6.2.3", "asr_model":"vosk-model-small-de-0.15",
        "audio_seconds":round(len(samples)/16000,3), "transcript":text,
        "reference":reference,
        "synthetic_fixture_word_error_rate":round(distance(normalize(reference),normalize(text))/len(normalize(reference)),4),
        "silence_max_probability":round(max(silence_probabilities),4),
        "speech_frames":sum(p >= .6 for p in probabilities),
        "vad_p95_ms":round(float(np.percentile(vad_ms,95)),3),
        "asr_frame_p95_ms":round(float(np.percentile(asr_ms,95)),3),
        "pipeline_real_time_factor":round(elapsed/(len(padded)/16000),3),
        "peak_process_rss_mib":round(resource.getrusage(resource.RUSAGE_SELF).ru_maxrss/1024,1),
        "checks_passed":["silence is not speech","German speech activates VAD","ASR emits German words"],
    }
    args.output.write_text(json.dumps(report,indent=2,ensure_ascii=False)+"\n")
    print(json.dumps(report,indent=2,ensure_ascii=False))


if __name__ == "__main__":
    main()
