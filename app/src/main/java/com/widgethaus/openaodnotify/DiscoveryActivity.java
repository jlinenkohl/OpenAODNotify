package com.widgethaus.openaodnotify;

import android.app.Notification;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.switchmaterial.SwitchMaterial;

import java.util.HashSet;
import java.util.Set;

public class DiscoveryActivity extends AppCompatActivity {
    private SwitchMaterial swGlobalDefault;
    private LinearLayout layoutCategories, layoutApps;
    private Button btnSave;
    private View scrollDiscovery;

    private static final String[] ALL_CATEGORIES = {
        Notification.CATEGORY_MESSAGE,
        Notification.CATEGORY_EMAIL,
        Notification.CATEGORY_SOCIAL,
        Notification.CATEGORY_CALL,
        Notification.CATEGORY_ALARM,
        Notification.CATEGORY_EVENT,
        Notification.CATEGORY_PROMO,
        Notification.CATEGORY_RECOMMENDATION,
        Notification.CATEGORY_SERVICE,
        Notification.CATEGORY_SYSTEM
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        applyUITheme();
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_discovery);

        // Stop any active overlay (like the 'move handle' preview) when entering discovery
        stopOverlayService();

        swGlobalDefault = findViewById(R.id.swGlobalDefault);
        layoutCategories = findViewById(R.id.layoutCategories);
        layoutApps = findViewById(R.id.layoutApps);
        btnSave = findViewById(R.id.btnSaveDiscovery);
        scrollDiscovery = findViewById(R.id.scrollDiscovery);

        loadSettings();
        setupListeners();
    }

    private void stopOverlayService() {
        Intent stopIntent = new Intent(this, OpenAODOverlayService.class);
        stopIntent.setAction(OpenAODOverlayService.ACTION_STOP);
        startService(stopIntent);
    }

    private void applyUITheme() {
        String theme = PreferenceUtils.getPrefs(this).getString(PreferenceUtils.KEY_UI_THEME, PreferenceUtils.UI_THEME_SYSTEM);
        if (PreferenceUtils.UI_THEME_SYSTEM.equalsIgnoreCase(theme)) {
            setTheme(R.style.Theme_OpenAODNotify_System);
        } else {
            setTheme(R.style.Theme_OpenAODNotify_Classic);
        }
    }

    private void loadSettings() {
        SharedPreferences prefs = PreferenceUtils.getPrefs(this);
        boolean isGlobal = prefs.getBoolean("filter_global_default", true);
        swGlobalDefault.setChecked(isGlobal);

        Set<String> enabledCats = prefs.getStringSet("filter_enabled_categories", new HashSet<>());
        Set<String> enabledApps = prefs.getStringSet("filter_enabled_apps", new HashSet<>());

        // Populate Categories
        layoutCategories.removeAllViews();
        for (String cat : ALL_CATEGORIES) {
            CheckBox cb = new CheckBox(this);
            cb.setText(cat);
            cb.setTag(cat);
            cb.setChecked(enabledCats.contains(cat));
            layoutCategories.addView(cb);
        }

        // Populate Apps from Discovered Rules
        layoutApps.removeAllViews();
        Set<String> discoveredRules = PreferenceUtils.getDiscoveredRules(this);
        Set<String> uniquePkgs = new HashSet<>();
        for (String rule : discoveredRules) {
            String pkg = rule.split("\\|")[0];
            uniquePkgs.add(pkg);
        }

        for (String pkg : uniquePkgs) {
            CheckBox cb = new CheckBox(this);
            cb.setText(pkg);
            cb.setTag(pkg);
            cb.setChecked(enabledApps.contains(pkg));
            layoutApps.addView(cb);
        }

        updateUiState(isGlobal);
    }

    private void updateUiState(boolean isGlobal) {
        float alpha = isGlobal ? 0.4f : 1.0f;
        scrollDiscovery.setAlpha(alpha);
        
        for (int i = 0; i < layoutCategories.getChildCount(); i++) {
            layoutCategories.getChildAt(i).setEnabled(!isGlobal);
        }
        for (int i = 0; i < layoutApps.getChildCount(); i++) {
            layoutApps.getChildAt(i).setEnabled(!isGlobal);
        }
    }

    private void setupListeners() {
        swGlobalDefault.setOnCheckedChangeListener((v, isChecked) -> updateUiState(isChecked));

        btnSave.setOnClickListener(v -> {
            Set<String> enabledCats = new HashSet<>();
            for (int i = 0; i < layoutCategories.getChildCount(); i++) {
                CheckBox cb = (CheckBox) layoutCategories.getChildAt(i);
                if (cb.isChecked()) enabledCats.add((String) cb.getTag());
            }

            Set<String> enabledApps = new HashSet<>();
            for (int i = 0; i < layoutApps.getChildCount(); i++) {
                CheckBox cb = (CheckBox) layoutApps.getChildAt(i);
                if (cb.isChecked()) enabledApps.add((String) cb.getTag());
            }

            PreferenceUtils.getPrefs(this).edit()
                    .putBoolean("filter_global_default", swGlobalDefault.isChecked())
                    .putStringSet("filter_enabled_categories", enabledCats)
                    .putStringSet("filter_enabled_apps", enabledApps)
                    .apply();

            // Notify the listener to refresh its state immediately
            sendBroadcast(new Intent(OpenAODListener.ACTION_REFRESH));

            Toast.makeText(this, "Filtering rules applied", Toast.LENGTH_SHORT).show();
            finish();
        });
    }
}