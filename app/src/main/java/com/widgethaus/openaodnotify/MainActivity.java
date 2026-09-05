package com.widgethaus.openaodnotify;

import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
import android.provider.MediaStore;
import android.provider.Settings;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import android.service.notification.NotificationListenerService;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Map;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";

    private Button btnOverlay, btnNotify, btnBattery, btnAppInfo, btnTestOverlay, btnAccessibility, btnExportLogs, btnReset, btnApplyBreathingFps;
    private ImageButton btnSettings;
    private TextView tvVersion, tvOverlayTelemetry;
    private View stepperBreathingFps;
    private String currentTheme;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Track the theme used for this creation
        currentTheme = PreferenceUtils.getPrefs(this).getString(PreferenceUtils.KEY_UI_THEME, PreferenceUtils.UI_THEME_SYSTEM);
        applyUITheme(currentTheme);
        
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        bindViews();
        displayVersion();
        setupListeners();
        setupBreathingFpsStepper();
        ensureListenerRunning();
        updateUI();
    }

    private void applyUITheme(String theme) {
        if (PreferenceUtils.UI_THEME_SYSTEM.equalsIgnoreCase(theme)) {
            setTheme(R.style.Theme_OpenAODNotify_System);
        } else {
            setTheme(R.style.Theme_OpenAODNotify_Classic);
        }
    }

    private void bindViews() {
        btnOverlay = findViewById(R.id.btnOverlay);
        btnNotify = findViewById(R.id.btnNotify);
        btnBattery = findViewById(R.id.btnBattery);
        btnAppInfo = findViewById(R.id.btnAppInfo);
        btnSettings = findViewById(R.id.btnSettings);
        btnTestOverlay = findViewById(R.id.btnTestOverlay);
        btnAccessibility = findViewById(R.id.btnAccessibility);
        btnExportLogs = findViewById(R.id.btnExportLogs);
        btnReset = findViewById(R.id.btnReset);
        btnApplyBreathingFps = findViewById(R.id.btnApplyBreathingFps);
        stepperBreathingFps = findViewById(R.id.stepperBreathingFps);
        tvVersion = findViewById(R.id.tvVersion);
        tvOverlayTelemetry = findViewById(R.id.tvOverlayTelemetry);

        // Hide advanced debug tools in Release builds
        if (!BuildConfig.DEBUG) {
            findViewById(R.id.layoutDebugTools).setVisibility(View.GONE);
            // We keep btnTestOverlay visible if it's outside layoutDebugTools, 
            // but in your XML it's inside. Let's make sure it's accessible for users.
        }
    }

    private void displayVersion() {
        try {
            // minSdk is 34, so we can use the modern API directly
            PackageInfo pInfo = getPackageManager().getPackageInfo(
                    getPackageName(), 
                    PackageManager.PackageInfoFlags.of(0)
            );
            tvVersion.setText("Version: " + pInfo.versionName);
        } catch (PackageManager.NameNotFoundException e) {
            // Fallback to BuildConfig if PackageManager fails
            tvVersion.setText("Version: " + BuildConfig.VERSION_NAME);
        }
    }

    private void setupListeners() {
        btnAppInfo.setOnClickListener(v -> {
            Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            intent.setData(Uri.parse("package:" + getPackageName()));
            startActivity(intent);
        });

        btnSettings.setOnClickListener(v -> {
            Intent intent = new Intent(this, SettingsActivity.class);
            startActivity(intent);
        });

        btnTestOverlay.setOnClickListener(v -> {
            if (!Settings.canDrawOverlays(this)) {
                Toast.makeText(this, "Please grant Overlay Permission first", Toast.LENGTH_SHORT).show();
                return;
            }
            if (!PreferenceUtils.isAccessibilityServiceEnabled(this)) {
                Toast.makeText(this, "Please grant Accessibility Permission first", Toast.LENGTH_SHORT).show();
                return;
            }
            Intent intent = new Intent(this, OpenAODOverlayService.class);
            intent.setAction(OpenAODOverlayService.ACTION_START);
            intent.putExtra("preview", true);
            
            PreferenceUtils.ShapeType shape = PreferenceUtils.getCurrentShape(this);
            intent.putExtra("shape", shape.getId());
            intent.putExtra("color", PreferenceUtils.getColor(this));
            intent.putExtra("size", PreferenceUtils.getShapeSize(this, shape));
            intent.putExtra("duration", PreferenceUtils.getDuration(this));
            intent.putExtra("min_alpha", PreferenceUtils.getMinAlpha(this));
            intent.putExtra("max_alpha", PreferenceUtils.getMaxAlpha(this));
            intent.putExtra("rounded", PreferenceUtils.isShapeRounded(this, shape));
            intent.putExtra("x", PreferenceUtils.getShapeX(this, shape));
            intent.putExtra("y", PreferenceUtils.getShapeY(this, shape));
            intent.putExtra("sides", PreferenceUtils.getLineSides(this));
            
            startService(intent);
            Toast.makeText(this, "Testing AOD Dot...", Toast.LENGTH_SHORT).show();
            
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                Intent stopIntent = new Intent(this, OpenAODOverlayService.class);
                stopIntent.setAction(OpenAODOverlayService.ACTION_STOP);
                startService(stopIntent);
            }, 5000);
        });

        btnExportLogs.setOnClickListener(v -> exportLogs());
        btnReset.setOnClickListener(v -> resetAndInitialize());
    }

    private void setupBreathingFpsStepper() {
        ((TextView) stepperBreathingFps.findViewById(R.id.tvLabel)).setText("Breathing FPS Target (debug)");
        EditText etFps = stepperBreathingFps.findViewById(R.id.etValue);
        etFps.setText(String.valueOf(PreferenceUtils.getBreathingFps(this)));

        stepperBreathingFps.findViewById(R.id.btnMinus).setOnClickListener(v -> {
            try {
                int val = Integer.parseInt(etFps.getText().toString());
                etFps.setText(String.valueOf(Math.max(PreferenceUtils.MIN_BREATHING_FPS, val - 1)));
            } catch (Exception ignored) {}
        });
        stepperBreathingFps.findViewById(R.id.btnPlus).setOnClickListener(v -> {
            try {
                int val = Integer.parseInt(etFps.getText().toString());
                etFps.setText(String.valueOf(Math.min(PreferenceUtils.MAX_BREATHING_FPS, val + 1)));
            } catch (Exception ignored) {}
        });

        btnApplyBreathingFps.setOnClickListener(v -> {
            try {
                int fps = Integer.parseInt(etFps.getText().toString());
                PreferenceUtils.setBreathingFps(this, fps);
                etFps.setText(String.valueOf(PreferenceUtils.getBreathingFps(this)));

                // Force any currently-running breathing animation to stop; the next time
                // the overlay is shown (test button or a real notification) it will pick up
                // the newly saved FPS target since it's read fresh in startBreathing().
                Intent stopIntent = new Intent(this, OpenAODOverlayService.class);
                stopIntent.setAction(OpenAODOverlayService.ACTION_STOP);
                startService(stopIntent);

                Toast.makeText(this, "Breathing FPS target set to " + PreferenceUtils.getBreathingFps(this) + ". Applies on next overlay display.", Toast.LENGTH_LONG).show();
            } catch (Exception e) {
                Toast.makeText(this, "Invalid FPS value", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void resetAndInitialize() {
        Log.d(TAG, "🚀 Full Reset & Initialization Triggered");
        dumpDebugInfo();

        Intent stopIntent = new Intent(this, OpenAODOverlayService.class);
        stopIntent.setAction(OpenAODOverlayService.ACTION_STOP);
        startService(stopIntent);

        if (PreferenceUtils.isNotificationAccessGranted(this)) {
            ComponentName componentName = new ComponentName(this, OpenAODListener.class);
            getPackageManager().setComponentEnabledSetting(componentName, 
                    PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP);
            getPackageManager().setComponentEnabledSetting(componentName, 
                    PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP);
            
            NotificationListenerService.requestRebind(componentName);
        }

        Toast.makeText(this, "Reset complete. Checking Logcat for debug dump...", Toast.LENGTH_LONG).show();
    }

    private void dumpDebugInfo() {
        DisplayMetrics metrics = getResources().getDisplayMetrics();
        Log.d(TAG, "--- DEBUG DUMP ---");
        Log.d(TAG, "Version: " + tvVersion.getText());
        Log.d(TAG, "Screen: " + metrics.widthPixels + "x" + metrics.heightPixels + " (Density: " + metrics.density + ")");
        Log.d(TAG, "Overlay Permission: " + Settings.canDrawOverlays(this));
        Log.d(TAG, "Accessibility Enabled: " + PreferenceUtils.isAccessibilityServiceEnabled(this));
        Log.d(TAG, "Notification Access: " + PreferenceUtils.isNotificationAccessGranted(this));
        Log.d(TAG, "Has Notification State: " + OpenAODListener.hasNotification);
        
        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        Log.d(TAG, "Is Screen On (Interactive): " + (pm != null && pm.isInteractive()));
        
        Log.d(TAG, "Current Settings: " + PreferenceUtils.getPrefs(this).getAll());
        Log.d(TAG, "-------------------");
    }

    private void exportLogs() {
        try {
            Process process = Runtime.getRuntime().exec("logcat -d");
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(process.getInputStream()));

            StringBuilder log = new StringBuilder();
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                if (line.contains("OpenAOD") || line.contains("Notification") || line.contains(TAG)) {
                    log.append(line).append("\n");
                }
            }
            final String logData = log.toString();

            new AlertDialog.Builder(this)
                    .setTitle("Export Debug Logs")
                    .setMessage("Choose how to export the captured debug logs:")
                    .setPositiveButton("Share", (dialog, which) -> shareLogData(logData))
                    .setNeutralButton("Save to Downloads", (dialog, which) -> saveLogToDownloads(logData))
                    .setNegativeButton("Cancel", null)
                    .show();

        } catch (Exception e) {
            Toast.makeText(this, "Export failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void saveLogToDownloads(String data) {
        try {
            String fileName = "openaod_debug_" + System.currentTimeMillis() + ".txt";
            ContentValues values = new ContentValues();
            values.put(MediaStore.Downloads.DISPLAY_NAME, fileName);
            values.put(MediaStore.Downloads.MIME_TYPE, "text/plain");
            values.put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS);

            Uri uri = getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);
            if (uri != null) {
                try (OutputStream os = getContentResolver().openOutputStream(uri)) {
                    if (os != null) os.write(data.getBytes());
                }
                Toast.makeText(this, "Logs saved to Downloads: " + fileName, Toast.LENGTH_LONG).show();
            }
        } catch (Exception e) {
            Toast.makeText(this, "Save failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void shareLogData(String data) {
        try {
            File logFile = new File(getExternalFilesDir(null), "openaod_debug_logs.txt");
            try (FileOutputStream fos = new FileOutputStream(logFile)) {
                fos.write(data.getBytes());
            }

            Uri contentUri = FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", logFile);
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("text/plain");
            shareIntent.putExtra(Intent.EXTRA_STREAM, contentUri);
            shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(Intent.createChooser(shareIntent, "Share Debug Logs"));
        } catch (Exception e) {
            Toast.makeText(this, "Share failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void ensureListenerRunning() {
        if (PreferenceUtils.isNotificationAccessGranted(this)) {
            ComponentName componentName = new ComponentName(this, OpenAODListener.class);
            NotificationListenerService.requestRebind(componentName);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        
        // If theme has changed while in Settings, refresh this activity
        String savedTheme = PreferenceUtils.getPrefs(this).getString(PreferenceUtils.KEY_UI_THEME, PreferenceUtils.UI_THEME_SYSTEM);
        if (!savedTheme.equalsIgnoreCase(currentTheme)) {
            recreate();
        }

        updateUI();
    }

    private void updateUI() {
        updateOverlayTelemetryDisplay();
        if (!Settings.canDrawOverlays(this)) {
            btnOverlay.setVisibility(View.VISIBLE);
            btnOverlay.setOnClickListener(v -> {
                Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:" + getPackageName()));
                startActivity(intent);
            });
        } else {
            btnOverlay.setVisibility(View.GONE);
        }

        if (!PreferenceUtils.isNotificationAccessGranted(this)) {
            btnNotify.setVisibility(View.VISIBLE);
            btnNotify.setOnClickListener(v -> {
                Intent intent = new Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS");
                startActivity(intent);
            });
        } else {
            btnNotify.setVisibility(View.GONE);
        }

        if (!PreferenceUtils.isAccessibilityServiceEnabled(this)) {
            btnAccessibility.setVisibility(View.VISIBLE);
            btnAccessibility.setOnClickListener(v -> {
                Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
                startActivity(intent);
            });
        } else {
            btnAccessibility.setVisibility(View.GONE);
        }

        if (isBatteryOptimized()) {
            btnBattery.setVisibility(View.VISIBLE);
            btnBattery.setOnClickListener(v -> {
                Intent intent = new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
                intent.setData(Uri.parse("package:" + getPackageName()));
                startActivity(intent);
            });
        } else {
            btnBattery.setVisibility(View.GONE);
        }
    }

    private boolean isBatteryOptimized() {
        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        return !pm.isIgnoringBatteryOptimizations(getPackageName());
    }

    private void updateOverlayTelemetryDisplay() {
        if (tvOverlayTelemetry == null) return;
        Map<LocalDate, Long> telemetry = PreferenceUtils.getRecentOverlayTelemetry(this, 7);
        Map<LocalDate, Long> notifCounts = PreferenceUtils.getRecentNotificationCounts(this, 7);
        DateTimeFormatter labelFormat = DateTimeFormatter.ofPattern("EEE M/d", Locale.getDefault());
        LocalDate today = LocalDate.now();
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<LocalDate, Long> entry : telemetry.entrySet()) {
            LocalDate date = entry.getKey();
            long totalSeconds = entry.getValue() / 1000;
            long mins = totalSeconds / 60;
            long secs = totalSeconds % 60;
            long notifs = notifCounts.getOrDefault(date, 0L);
            String label = date.equals(today) ? "Today    " : date.format(labelFormat);
            sb.append(String.format(Locale.getDefault(), "%-9s %3dm %02ds  |  %3d notifs%n", label, mins, secs, notifs));
        }
        tvOverlayTelemetry.setText(sb.toString().trim());
    }
}
