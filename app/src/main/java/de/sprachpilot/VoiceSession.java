package de.sprachpilot;

import android.annotation.SuppressLint;
import android.content.Context;
import android.media.*;
import android.media.audiofx.*;
import android.os.*;
import android.speech.tts.*;
import org.json.JSONObject;
import org.vosk.Model;
import org.vosk.Recognizer;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

/** One explicit microphone session. No network, no audio files, no durable transcripts. */
final class VoiceSession implements Runnable {
    private final VoiceService host;
    final boolean background;
    private final int pauseMs;
    private final Handler main = new Handler(Looper.getMainLooper());
    private final CountDownLatch voiceReady = new CountDownLatch(1);
    private final AtomicLong generation = new AtomicLong();
    private final Object outputLock = new Object();
    private final TextToSpeech tts;
    private final Thread worker;
    private volatile boolean running = true, speaking, turnActive, pendingReply, voiceOk;
    private volatile AudioRecord recorder;
    private String voiceName = "";
    private volatile int interruptions;
    private volatile long stopCallMs = -1;
    private volatile long stopCallbackMs = -1;
    private volatile long stopRequestedAt;
    private volatile String stoppedId = "";
    private String aecInfo = "noch nicht geprüft";

    VoiceSession(VoiceService host, boolean background, int pauseMs) {
        this.host = host; this.background = background; this.pauseMs = pauseMs;
        tts = new TextToSpeech(host, status -> main.post(() -> initializeVoice(status)));
        tts.setOnUtteranceProgressListener(new UtteranceProgressListener() {
            @Override public void onStart(String id) { }
            @Override public void onDone(String id) { main.post(() -> outputDone(id, false)); }
            @Override public void onError(String id) { main.post(() -> outputDone(id, true)); }
            @Override public void onStop(String id, boolean interrupted) {
                if (id.equals(stoppedId)) stopCallbackMs = SystemClock.elapsedRealtime() - stopRequestedAt;
            }
        });
        worker = new Thread(this, "local-voice");
    }
    void start() { worker.start(); }
    private void initializeVoice(int status) {
        if (!running) return;
        if (status == TextToSpeech.SUCCESS && tts.getVoices() != null) {
            List<Voice> voices = new ArrayList<>();
            for (Voice voice : tts.getVoices()) {
                if (voice.getLocale().getLanguage().equals("de") && !voice.isNetworkConnectionRequired()
                    && !voice.getFeatures().contains(TextToSpeech.Engine.KEY_FEATURE_NOT_INSTALLED)) voices.add(voice);
            }
            voices.sort(Comparator.comparingInt(Voice::getQuality).reversed().thenComparing(Voice::getName));
            if (!voices.isEmpty() && tts.setVoice(voices.get(0)) == TextToSpeech.SUCCESS) {
                voiceOk = true; voiceName = voices.get(0).getName();
                tts.setAudioAttributes(new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_VOICE_COMMUNICATION)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build());
            }
        }
        voiceReady.countDown();
    }
    private void update(String message) { main.post(() -> host.sessionUpdate(this, message)); }
    @SuppressLint("MissingPermission") // Activity grants RECORD_AUDIO before explicit service start.
    @Override public void run() {
        AudioRecord audio = null;
        AcousticEchoCanceler aec = null;
        NoiseSuppressor ns = null;
        AudioManager manager = (AudioManager)host.getSystemService(Context.AUDIO_SERVICE);
        int oldMode = manager.getMode();
        boolean modeChanged = false;
        boolean routeChanged = false;
        AudioFocusRequest focus = new AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
            .setAudioAttributes(new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_VOICE_COMMUNICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build())
            .setOnAudioFocusChangeListener(change -> {
                if (change < 0) { update("Audiofokus verloren · Mikrofon wird gestoppt"); main.post(() -> host.stopIfCurrent(this)); }
            },main).build();
        boolean focusHeld = false;
        try (Model model = new Model(Assets.model(host).getAbsolutePath());
             SileroVad vad = new SileroVad(Assets.bytes(host.getAssets(),"silero.onnx"));
             Recognizer recognizer = new Recognizer(model, 16000)) {
            if (!voiceReady.await(20, TimeUnit.SECONDS) || !voiceOk) {
                throw new IllegalStateException("Keine installierte deutsche Offline-Stimme. In den Android-Sprachausgabe-Einstellungen installieren und erneut starten.");
            }
            if (!running) return;
            focusHeld = manager.requestAudioFocus(focus) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED;
            if (!focusHeld) throw new IllegalStateException("Audiofokus nicht verfügbar (z. B. während eines Anrufs)");
            manager.setMode(AudioManager.MODE_IN_COMMUNICATION); modeChanged = true;
            AudioDeviceInfo route = manager.getCommunicationDevice();
            if (route == null || route.getType() == AudioDeviceInfo.TYPE_BUILTIN_EARPIECE || route.getType() == AudioDeviceInfo.TYPE_BUILTIN_SPEAKER) {
                for (AudioDeviceInfo device : manager.getAvailableCommunicationDevices()) {
                    if (device.getType() == AudioDeviceInfo.TYPE_BUILTIN_SPEAKER) {
                        routeChanged = manager.setCommunicationDevice(device); break;
                    }
                }
            }
            int min = AudioRecord.getMinBufferSize(16000, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);
            if (min <= 0) throw new IllegalStateException("16-kHz-Mikrofonformat nicht verfügbar");
            audio = new AudioRecord.Builder().setAudioSource(MediaRecorder.AudioSource.VOICE_COMMUNICATION)
                .setAudioFormat(new AudioFormat.Builder().setSampleRate(16000)
                .setChannelMask(AudioFormat.CHANNEL_IN_MONO).setEncoding(AudioFormat.ENCODING_PCM_16BIT).build())
                .setBufferSizeInBytes(Math.max(min, 8192)).build();
            recorder = audio;
            if (!running) return;
            if (audio.getState() != AudioRecord.STATE_INITIALIZED) throw new IllegalStateException("Mikrofon konnte nicht initialisiert werden");
            audio.registerAudioRecordingCallback(host.getMainExecutor(),new AudioManager.AudioRecordingCallback() {
                @Override public void onRecordingConfigChanged(List<AudioRecordingConfiguration> configurations) {
                    if (running && configurations.stream().anyMatch(AudioRecordingConfiguration::isClientSilenced)) {
                        host.sessionUpdate(VoiceSession.this,"Android hat den Mikrofonzugriff stummgeschaltet · Sitzung beendet");
                        host.stopIfCurrent(VoiceSession.this);
                    }
                }
            });
            try {
                if (AcousticEchoCanceler.isAvailable()) {
                    aec = AcousticEchoCanceler.create(audio.getAudioSessionId());
                    if (aec != null) aec.setEnabled(true);
                }
                aecInfo = aec != null && aec.getEnabled() ? "aktiv (Wirksamkeit ungemessen)" : "nicht verfügbar/aktiv";
                if (NoiseSuppressor.isAvailable()) {
                    ns = NoiseSuppressor.create(audio.getAudioSessionId());
                    if (ns != null) ns.setEnabled(true);
                }
            } catch (RuntimeException e) { aecInfo = "Effekt nicht steuerbar"; }
            audio.startRecording();
            update("Mikrofon aktiv · Deutsch lokal · Stimme: " + voiceName + "\nAEC: " + aecInfo);
            TurnDetector detector = new TurnDetector(pauseMs);
            ArrayDeque<short[]> preroll = new ArrayDeque<>();
            StringBuilder recognized = new StringBuilder();
            short[] frame = new short[512];
            long token = 0, lastUi = 0, sessionStart = SystemClock.elapsedRealtime();
            while (running) {
                int offset = 0;
                while (running && offset < frame.length) {
                    int count = audio.read(frame, offset, frame.length-offset, AudioRecord.READ_BLOCKING);
                    if (count < 0) throw new IllegalStateException("Mikrofon-Lesefehler " + count);
                    if (count == 0) throw new IllegalStateException("Mikrofon liefert keine Daten");
                    offset += count;
                }
                if (!running) break;
                // One-shot sessions cannot retain an idle microphone indefinitely.
                if (!background && !turnActive && !speaking && !pendingReply &&
                    SystemClock.elapsedRealtime() - sessionStart > 30000) {
                    update("Nach 30 Sekunden ohne Eingabe beendet"); break;
                }
                float probability = vad.probability(frame);
                boolean wasActive = detector.active();
                TurnDetector.Event event = detector.accept(probability >= 0.60f);
                if (!wasActive) {
                    preroll.addLast(frame.clone());
                    while (preroll.size() > 10) preroll.removeFirst();
                }
                if (event == TurnDetector.Event.START) {
                    synchronized (outputLock) {
                    turnActive = true; pendingReply = false; token = generation.incrementAndGet();
                    if (speaking) {
                        long before = SystemClock.elapsedRealtime();
                        stoppedId = Long.toString(token-1); stopRequestedAt = before;
                        tts.stop(); stopCallMs = SystemClock.elapsedRealtime()-before;
                        speaking = false; interruptions++;
                        update("Spracheinsatz erkannt · Antwort abgebrochen · stop()-Aufruf " + stopCallMs + " ms");
                    } else update("Sprache erkannt …");
                    }
                    recognizer.reset(); recognized.setLength(0);
                    for (short[] samples : preroll) feed(recognizer, samples, recognized);
                    preroll.clear();
                } else if (wasActive) feed(recognizer, frame, recognized);
                if (event == TurnDetector.Event.END || event == TurnDetector.Event.LIMIT) {
                    String tail = new JSONObject(recognizer.getFinalResult()).optString("text");
                    if (!tail.isBlank()) recognized.append(tail).append(' ');
                    String text = recognized.toString().trim();
                    synchronized (outputLock) { turnActive = false; pendingReply = true; }
                    long completedToken = token;
                    main.post(() -> respond(text, completedToken));
                }
                if (SystemClock.elapsedRealtime()-lastUi > 1000) {
                    lastUi = SystemClock.elapsedRealtime();
                    main.post(() -> host.sessionMetrics(this, diagnostics()));
                }
            }
        } catch (Exception e) {
            if (running) update("Fehler: " + e.getClass().getSimpleName() + " · " + e.getMessage());
        } catch (LinkageError e) {
            if (running) update("Native Sprachbibliothek kann nicht geladen werden: " + e.getClass().getSimpleName());
        } finally {
            running = false;
            if (audio != null) {
                try { audio.stop(); } catch (IllegalStateException ignored) { }
                if (aec != null) aec.release();
                if (ns != null) ns.release();
                audio.release();
            }
            recorder = null;
            try {
                if (routeChanged) manager.clearCommunicationDevice();
                if (modeChanged && manager.getMode() == AudioManager.MODE_IN_COMMUNICATION) manager.setMode(oldMode);
            }
            finally { if (focusHeld) manager.abandonAudioFocusRequest(focus); }
            main.post(() -> host.sessionFinished(this));
        }
    }
    private static void feed(Recognizer recognizer, short[] samples, StringBuilder text) throws Exception {
        if (recognizer.acceptWaveForm(samples, samples.length)) {
            String segment = new JSONObject(recognizer.getResult()).optString("text");
            if (!segment.isBlank()) text.append(segment).append(' ');
        }
    }
    private void respond(String text, long token) {
        synchronized (outputLock) {
        if (!running || generation.get() != token) return;
        pendingReply = false;
        if (text.isBlank()) {
            update("Keine Wörter erkannt. Bitte erneut sprechen.");
            if (!background) host.stopSession();
            return;
        }
        String answer = text.matches(".*\\b(hallo|guten tag)\\b.*")
            ? "Hallo! Ich bin dein lokaler Sprachprototyp. Du kannst mich beim Sprechen unterbrechen."
            : "Ich habe verstanden: " + text + ". Dies ist eine lokale Testantwort. Du kannst jetzt dazwischen sprechen.";
        host.sessionTranscript(this, text, answer);
        speaking = true;
        if (tts.speak(answer, TextToSpeech.QUEUE_FLUSH, new Bundle(), Long.toString(token)) == TextToSpeech.ERROR) {
            outputDone(Long.toString(token), true);
        }
        }
    }
    private void outputDone(String id, boolean error) {
        synchronized (outputLock) {
        if (!running || !id.equals(Long.toString(generation.get()))) return;
        speaking = false;
        if (error) { update("Fehler bei lokaler Sprachausgabe"); host.stopSession(); return; }
        if (!background && !turnActive && !pendingReply) {
            // Let a voice onset at the end of the last syllable reach the 3-frame VAD trigger.
            long completed = generation.get();
            main.postDelayed(() -> {
                synchronized (outputLock) {
                    if (running && generation.get() == completed && !turnActive && !pendingReply) host.stopSession();
                }
            },250);
        } else update("Mikrofon aktiv · Bereit für die nächste Eingabe");
        }
    }
    String diagnostics() {
        try {
            return new JSONObject().put("asr","vosk-small-de-0.15").put("vad","silero-v6.2.3")
                .put("voice",voiceName).put("aec",aecInfo).put("silence_ms",pauseMs)
                .put("frame_ms",32).put("speech_trigger_positive_frames",3).put("interruptions",interruptions)
                .put("stop_call_ms",stopCallMs).put("stop_callback_ms",stopCallbackMs)
                .put("acoustic_stop_latency_measured",false)
                .put("manufacturer",Build.MANUFACTURER).put("device",Build.MODEL)
                .put("android_api",Build.VERSION.SDK_INT).toString(2);
        } catch (Exception e) { return "{}"; }
    }
    void stop() {
        synchronized (outputLock) {
        running = false; generation.incrementAndGet(); voiceReady.countDown();
        tts.stop(); tts.shutdown();
        }
        AudioRecord audio = recorder;
        if (audio != null) try { audio.stop(); } catch (IllegalStateException ignored) { }
        worker.interrupt();
    }
}
