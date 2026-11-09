package de.sprachpilot;

import android.app.*;
import android.content.*;
import android.content.pm.ServiceInfo;
import android.os.*;

public final class VoiceService extends Service {
    public static final String START = "de.sprachpilot.START", STOP = "de.sprachpilot.STOP";
    private static final String CHANNEL = "microphone";
    private VoiceSession session;
    private VoiceSession stopping;
    private final LocalBinder binder = new LocalBinder();
    Runnable listener;
    String status = "Mikrofon aus", transcript = "", answer = "", metrics = "{}";
    public final class LocalBinder extends Binder { VoiceService service() { return VoiceService.this; } }
    @Override public IBinder onBind(Intent intent) { return binder; }
    boolean active() { return session != null; }
    boolean ready() { return session == null && stopping == null; }
    boolean background() { return session != null && session.background; }
    private void changed() { if (listener != null) listener.run(); }
    @Override public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null || STOP.equals(intent.getAction())) { stopSession(); return START_NOT_STICKY; }
        if (!START.equals(intent.getAction()) || session != null) return START_NOT_STICKY;
        boolean background = intent.getBooleanExtra("background",false);
        NotificationManager nm = getSystemService(NotificationManager.class);
        nm.createNotificationChannel(new NotificationChannel(CHANNEL, "Sichtbarer Mikrofonmodus", NotificationManager.IMPORTANCE_LOW));
        PendingIntent stop = PendingIntent.getService(this, 1, new Intent(this,VoiceService.class).setAction(STOP),PendingIntent.FLAG_IMMUTABLE);
        PendingIntent open = PendingIntent.getActivity(this, 2, new Intent(this,MainActivity.class),PendingIntent.FLAG_IMMUTABLE);
        Notification notification = new Notification.Builder(this,CHANNEL)
            .setSmallIcon(android.R.drawable.ic_btn_speak_now).setContentTitle("Sprachpilot · Mikrofonmodus gestartet")
            .setContentText("Lokale Verarbeitung. Hier jederzeit stoppen.").setOngoing(true)
            .setContentIntent(open).addAction(new Notification.Action.Builder(null,"Stoppen",stop).build()).build();
        try {
            startForeground(1,notification,ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE);
            if (stopping != null) { status = "Mikrofon wird noch freigegeben. Danach erneut starten."; stopSession(false); return START_NOT_STICKY; }
            int pause = Math.max(400,Math.min(1400,intent.getIntExtra("pause",700)));
            status = "Lokale Modelle und deutsche Stimme werden vorbereitet …";
            transcript = answer = ""; metrics = "{}";
            session = new VoiceSession(this,background,pause); session.start();
        } catch (RuntimeException e) {
            status = "Start fehlgeschlagen: " + e.getClass().getSimpleName(); stopSession(false);
        }
        changed(); return START_NOT_STICKY;
    }
    void sessionUpdate(VoiceSession source, String value) { if (source == session) { status = value; changed(); } }
    void sessionMetrics(VoiceSession source, String value) { if (source == session) { metrics = value; changed(); } }
    void sessionTranscript(VoiceSession source,String text,String reply) {
        if (source == session) { transcript = text; answer = reply; status = "Antwort wird lokal gesprochen · Mikrofon bleibt für Unterbrechen aktiv"; changed(); }
    }
    void sessionFinished(VoiceSession source) {
        if (source == session) stopSession(false);
        if (source == stopping) { stopping = null; changed(); }
    }
    void stopIfCurrent(VoiceSession source) { if (source == session) stopSession(false); }
    void stopSession() { stopSession(true); }
    private void stopSession(boolean report) {
        VoiceSession old = session; session = null;
        if (old != null) { stopping = old; metrics = old.diagnostics(); old.stop(); }
        if (report) status = "Mikrofon aus · Sprachausgabe gestoppt";
        stopForeground(STOP_FOREGROUND_REMOVE); stopSelf(); changed();
    }
    @Override public void onTaskRemoved(Intent rootIntent) { stopSession(); super.onTaskRemoved(rootIntent); }
    @Override public void onDestroy() { listener = null; stopSession(false); super.onDestroy(); }
}
