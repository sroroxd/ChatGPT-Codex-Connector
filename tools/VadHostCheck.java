package de.sprachpilot;

import java.nio.file.Files;
import java.nio.file.Path;
import javax.sound.sampled.AudioSystem;

/** Runs the unmodified Android SileroVad wrapper against desktop ORT 1.31.1. */
public final class VadHostCheck {
    public static void main(String[] args) throws Exception {
        try (SileroVad vad = new SileroVad(Files.readAllBytes(Path.of(args[0])))) {
            for (int i=0;i<100;i++) {
                if (vad.probability(new short[512]) >= .6f) throw new AssertionError("Silence triggered speech");
            }
            byte[] pcm;
            try (var audio = AudioSystem.getAudioInputStream(Path.of(args[1]).toFile())) {
                if (audio.getFormat().getSampleRate() != 16000) throw new AssertionError("Expected 16 kHz");
                pcm = audio.readAllBytes();
            }
            int speechFrames = 0;
            for (int offset=0;offset+1024<=pcm.length;offset+=1024) {
                short[] frame = new short[512];
                for (int i=0;i<512;i++) frame[i]=(short)((pcm[offset+i*2]&255)|(pcm[offset+i*2+1]<<8));
                if (vad.probability(frame)>=.6f) speechFrames++;
            }
            if (speechFrames<10) throw new AssertionError("Speech not detected");
            System.out.println("PASS: actual SileroVad.java + desktop ONNX Runtime 1.31.1, 100 silence frames, " + speechFrames + " speech frames; NOT Android/device test");
        }
    }
}
