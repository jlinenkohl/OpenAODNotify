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
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import android.service.notification.NotificationListenerService;

public class MainActivity extends AppCompatActivity {

    private Button btnOverlay, btnNotify, btnBattery, btnAppInfo, btnTestOverlay, btnAccessibility;
    private ImageButton btnSettings;
    private TextView tvVersion;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        bindViews();
        displayVersion();
        setupListeners();
        ensureListenerRunning();
    }

    private void bindViews() {
        btnOverlay = findViewById(R.id.btnOverlay);
        btnNotify = findViewById(R.id.btnNotify);
        btnBattery = findViewById(R.id.btnBattery);
        btnAppInfo = findViewById(R.id.btnAppInfo);
        btnSettings = findViewById(R.id.btnSettings);
        btnTestOverlay = findViewById(R.id.btnTestOverlay);
        btnAccessibility = findViewById(R.id.btnAccessibility);
        tvVersion = findViewById(R.id.tvVersion);
    }

    private void displayVersion() {
        try {
            PackageInfo pInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
            tvVersion.setText("Version: " + pInfo.versionName);
        } catch (PackageManager.NameNotFoundException e) {
            tvVersion.setText("v1.1-dev");
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
            intent.putExtra("shape", PreferenceUtils.getInt(this, "shape", 0));
            intent.putExtra("color", PreferenceUtils.getString(this, "color", "0066ff"));
            intent.putExtra("size", PreferenceUtils.getInt(this, "size", 60));
            intent.putExtra("duration", PreferenceUtils.getInt(this, "duration", 2500));
            intent.putExtra("min_alpha", PreferenceUtils.getFloat(this, "min_alpha", 0.1f));
            intent.putExtra("max_alpha", PreferenceUtils.getFloat(this, "max_alpha", 1.0f));
            intent.putExtra("rounded", PreferenceUtils.getBoolean(this, "rounded", true));
            
            startService(intent);
            Toast.makeText(this, "Testing AOD Dot...", Toast.LENGTH_SHORT).show();
            
            new android.os.Handler().postDelayed(() -> {
                Intent stopIntent = new Intent(this, OpenAODOverlayService.class);
                stopIntent.setAction(OpenAODOverlayService.ACTION_STOP);
                startService(stopIntent);
            }, 5000);
        });
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