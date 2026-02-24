package com.widgethaus.openaodnotify;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.PowerManager;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import android.service.notification.NotificationListenerService;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.util.Map;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";

    private Button btnOverlay, btnNotify, btnBattery, btnAppInfo, btnTestOverlay, btnAccessibility, btnExportLogs, btnReset;
    private ImageButton btnSettings;
    private TextView tvVersion;
    private String currentTheme;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Track the theme used for this creation
        currentTheme = PreferenceUtils.getPrefs(this).getString(PreferenceUtils.KEY_UI_THEME, PreferenceUtils.UI_THEME_MINIMAL);
        applyUITheme(currentTheme);
        
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        bindViews();
        displayVersion();
        setupListeners();
        ensureListenerRunning();
    }

    private void applyUITheme(String theme) {
        if (PreferenceUtils.UI_THEME_MINIMAL.equalsIgnoreCase(theme)) {
            setTheme(R.style.Theme_OpenAODNotify_Minimal);
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
        tvVersion = findViewById(R.id.tvVersion);
    }

    private void displayVersion() {
        try {
            PackageInfo pInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
            tvVersion.setText("Version: " + pInfo.versionName);
        } catch (PackageManager.NameNotFoundException e) {
            tvVersion.setText("v1.2-dev");
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
            
            startService(intent);
            Toast.makeText(this, "Testing AOD Dot...", Toast.LENGTH_SHORT).show();
            
            new android.os.Handler().postDelayed(() -> {
                Intent stopIntent = new Intent(this, OpenAODOverlayService.class);
                stopIntent.setAction(OpenAODOverlayService.ACTION_STOP);
                startService(stopIntent);
            }, 5000);
        });

        btnExportLogs.setOnClickListener(v -> exportLogs());
        btnReset.setOnClickListener(v -> resetAndInitialize());
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

            File logFile = new File(getExternalFilesDir(null), "openaod_debug_logs.txt");
            FileOutputStream fos = new FileOutputStream(logFile);
            fos.write(log.toString().getBytes());
            fos.close();

            Uri contentUri = FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", logFile);
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("text/plain");
            shareIntent.putExtra(Intent.EXTRA_STREAM, contentUri);
            shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(Intent.createChooser(shareIntent, "Share Debug Logs"));

        } catch (Exception e) {
            Toast.makeText(this, "Export failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
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
        String savedTheme = PreferenceUtils.getPrefs(this).getString(PreferenceUtils.KEY_UI_THEME, PreferenceUtils.UI_THEME_MINIMAL);
        if (!savedTheme.equalsIgnoreCase(currentTheme)) {
            recreate();
        }

        updateUI();
    }

    private void updateUI() {
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
}