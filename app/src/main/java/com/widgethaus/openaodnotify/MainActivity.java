package com.widgethaus.openaodnotify;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.PowerManager;
import android.provider.Settings;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    private Button btnOverlay, btnNotify, btnBattery, btnAppInfo, btnTestOverlay;
    private ImageButton btnSettings;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        btnOverlay = findViewById(R.id.btnOverlay);
        btnNotify = findViewById(R.id.btnNotify);
        btnBattery = findViewById(R.id.btnBattery);
        btnAppInfo = findViewById(R.id.btnAppInfo);
        btnSettings = findViewById(R.id.btnSettings);
        btnTestOverlay = findViewById(R.id.btnTestOverlay);

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
            Intent intent = new Intent(this, OpenAODOverlayService.class);
            startForegroundService(intent);
            Toast.makeText(this, "Testing AOD Dot... (Check top corner)", Toast.LENGTH_SHORT).show();
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateUI();
    }

    private void updateUI() {
        // Step 2: Request Overlay Permission
        if (!canDrawOverlays()) {
            btnOverlay.setVisibility(View.VISIBLE);
            btnOverlay.setOnClickListener(v -> {
                Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:" + getPackageName()));
                startActivity(intent);
            });
        } else {
            btnOverlay.setVisibility(View.GONE);
        }

        // Step 3: Request Notification Access
        if (!isNotificationAccessGranted()) {
            btnNotify.setVisibility(View.VISIBLE);
            btnNotify.setOnClickListener(v -> {
                Intent intent = new Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS");
                startActivity(intent);
            });
        } else {
            btnNotify.setVisibility(View.GONE);
        }

        // Step 4: Request Battery Exemption
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

    private boolean isNotificationAccessGranted() {
        String pkgName = getPackageName();
        String flat = Settings.Secure.getString(getContentResolver(), "enabled_notification_listeners");
        return flat != null && flat.contains(pkgName);
    }

    private boolean isBatteryOptimized() {
        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        return !pm.isIgnoringBatteryOptimizations(getPackageName());
    }

    private boolean canDrawOverlays() {
        return Settings.canDrawOverlays(this);
    }
}
