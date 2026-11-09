package de.sprachpilot;

import android.Manifest;
import android.app.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.os.*;
import android.graphics.Color;
import android.widget.*;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

public final class MainActivity extends Activity {
    private VoiceService service;
    private boolean bound, startPending, visible;
    private TextView status, transcript, metrics, pauseLabel;
    private CheckBox background;
    private SeekBar pause;
    private Button start;
    private String exportData = "{}";
    private final ServiceConnection connection = new ServiceConnection() {
        @Override public void onServiceConnected(ComponentName name, IBinder binder) {
            service = ((VoiceService.LocalBinder)binder).service(); service.listener = MainActivity.this::render; render();
        }
        @Override public void onServiceDisconnected(ComponentName name) { service = null; start.setEnabled(false); }
    };
    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        ScrollView scroll = new ScrollView(this);
        LinearLayout content = new LinearLayout(this); content.setOrientation(LinearLayout.VERTICAL);
        int padding = (int)(24 * getResources().getDisplayMetrics().density);
        content.setPadding(padding,padding,padding,padding); scroll.addView(content);
        label(content,"Sprachpilot",28);
        label(content,"Lokaler Sprachprototyp · Version 0.1",16);
        label(content,"Deutsch zuhören, antworten, unterbrechen. Antworten sind feste Testantworten; noch kein KI-Dialog.",16);
        status = label(content,"Mikrofon aus",20); status.setTextColor(Color.rgb(0,90,80));
        background = new CheckBox(this); background.setText(R.string.background_mode); content.addView(background);
        background.setChecked(getPreferences(MODE_PRIVATE).getBoolean("background",false));
        label(content,"Ohne Hintergrundmodus: ein Sprachzug per Startknopf; beim Verlassen der App aus. Während der Antwort bleibt das Mikrofon zum Unterbrechen aktiv. Hintergrund: läuft bis Stoppen weiter, auch bei gesperrtem Bildschirm, soweit Android es zulässt.",14);
        pauseLabel = label(content,"",16);
        pause = new SeekBar(this); pause.setMax(10); pause.setProgress(getPreferences(MODE_PRIVATE).getInt("pause",3)); content.addView(pause);
        updatePause();
        pause.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            public void onProgressChanged(SeekBar bar,int value,boolean user) { updatePause(); }
            public void onStartTrackingTouch(SeekBar bar) { }
            public void onStopTrackingTouch(SeekBar bar) { }
        });
        start = button(content,"Zuhören starten",this::requestStart); start.setEnabled(false);
        button(content,"Alles stoppen",() -> { startPending = false; if (service != null) service.stopSession(); });
        button(content,"Android-Sprachausgabe öffnen",() -> {
            try { startActivity(new Intent("com.android.settings.TTS_SETTINGS")); }
            catch (ActivityNotFoundException e) { status.setText(R.string.tts_settings_missing); }
        });
        transcript = label(content,"Noch keine Eingabe",18); transcript.setTextIsSelectable(true);
        label(content,"Diagnose · keine Audioaufnahme, keine gespeicherten Gespräche",16);
        metrics = label(content,"{}",13); metrics.setTextIsSelectable(true);
        button(content,"Diagnose als JSON speichern",() -> {
            exportData = service == null ? "{}" : service.metrics;
            Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT).setType("application/json")
                .addCategory(Intent.CATEGORY_OPENABLE).putExtra(Intent.EXTRA_TITLE,"sprachpilot-diagnose.json");
            startActivityForResult(intent,2);
        });
        label(content,"Datenschutz: Audio wird nur im Arbeitsspeicher verarbeitet. Erkennung und VAD laufen in dieser App. Die gewählte Offline-Stimme läuft im System-TTS-Dienst; dessen Verhalten liegt beim installierten Anbieter. Ein Flugmodus-Test prüft die Offline-Funktion. Kein Cloud-Zugang, kein Autostart, kein Root. Samsung kann Hintergrundprozesse beenden; die App startet sich danach nicht selbst neu.",14);
        setContentView(scroll);
        scroll.setOnApplyWindowInsetsListener((view,insets) -> {
            android.graphics.Insets bars = insets.getInsets(android.view.WindowInsets.Type.systemBars() | android.view.WindowInsets.Type.displayCutout());
            view.setPadding(bars.left,bars.top,bars.right,bars.bottom); return insets;
        });
        scroll.requestApplyInsets();
    }
    private TextView label(LinearLayout parent,String text,int size) {
        TextView view = new TextView(this); view.setText(text); view.setTextSize(size); view.setPadding(0,12,0,12); parent.addView(view); return view;
    }
    private Button button(LinearLayout parent,String text,Runnable action) {
        Button view = new Button(this); view.setText(text); view.setOnClickListener(v -> action.run()); parent.addView(view); return view;
    }
    private void updatePause() { pauseLabel.setText(getString(R.string.pause_duration,400+pause.getProgress()*100)); }
    private void render() {
        if (service == null) return;
        start.setEnabled(service.ready()); background.setEnabled(service.ready()); pause.setEnabled(service.ready());
        status.setText(service.status); metrics.setText(service.metrics);
        transcript.setText(service.transcript.isEmpty() ? "Noch keine Eingabe" : "Du: " + service.transcript + "\n\nAntwort: " + service.answer);
    }
    private void requestStart() {
        startPending = true;
        java.util.ArrayList<String> permissions = new java.util.ArrayList<>();
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) permissions.add(Manifest.permission.RECORD_AUDIO);
        if (background.isChecked() && Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) permissions.add(Manifest.permission.POST_NOTIFICATIONS);
        if (!permissions.isEmpty()) requestPermissions(permissions.toArray(new String[0]),1);
        else startGranted();
    }
    @Override public void onRequestPermissionsResult(int request,String[] permissions,int[] results) {
        super.onRequestPermissionsResult(request,permissions,results);
        if (request == 1 && startPending) startGranted();
    }
    private void startGranted() {
        startPending = false;
        if (!visible) return;
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            status.setText(R.string.no_mic_permission); return;
        }
        if (background.isChecked() && (!getSystemService(NotificationManager.class).areNotificationsEnabled()
            || (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED))) {
            status.setText(R.string.need_notifications); return;
        }
        getPreferences(MODE_PRIVATE).edit().putBoolean("background",background.isChecked()).putInt("pause",pause.getProgress()).apply();
        try {
            start.setEnabled(false);
            startForegroundService(new Intent(this,VoiceService.class).setAction(VoiceService.START)
                .putExtra("background",background.isChecked()).putExtra("pause",400+pause.getProgress()*100));
        } catch (RuntimeException e) { status.setText(getString(R.string.start_error,e.getClass().getSimpleName())); start.setEnabled(true); }
    }
    @Override protected void onStart() {
        super.onStart(); visible = true;
        bound = bindService(new Intent(this,VoiceService.class),connection,BIND_AUTO_CREATE);
    }
    @Override protected void onStop() {
        visible = false;
        if (service != null) {
            service.listener = null;
            if (!service.background()) service.stopSession();
        }
        if (bound) unbindService(connection);
        bound = false; service = null; super.onStop();
    }
    @Override protected void onActivityResult(int request,int result,Intent data) {
        super.onActivityResult(request,result,data);
        if (request != 2 || result != RESULT_OK || data == null || data.getData() == null) return;
        try (OutputStream out = getContentResolver().openOutputStream(data.getData())) {
            if (out == null) throw new java.io.IOException("Kein Dateizugriff");
            out.write(exportData.getBytes(StandardCharsets.UTF_8));
            Toast.makeText(this,"Diagnose gespeichert",Toast.LENGTH_SHORT).show();
        } catch (Exception e) { Toast.makeText(this,"Speichern fehlgeschlagen",Toast.LENGTH_LONG).show(); }
    }
}
