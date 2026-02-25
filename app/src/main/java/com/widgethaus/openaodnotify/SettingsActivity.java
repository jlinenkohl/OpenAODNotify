package com.widgethaus.openaodnotify;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.switchmaterial.SwitchMaterial;

public class SettingsActivity extends AppCompatActivity {
    private static final String TAG = "SettingsActivity";

    private EditText etX, etY, etColor;
    private View colorPreview;
    private SeekBar seekRed, seekGreen, seekBlue;
    private Spinner spnShape, spnProfile, spnUITheme;
    private Button btnSave;
    private ImageButton btnUp, btnDown, btnLeft, btnRight;
    private LinearLayout layoutPosition, layoutLineSides;
    private CheckBox cbTop, cbBottom, cbLeft, cbRight;
    private SwitchMaterial swRounded;
    private boolean isUpdatingFromBroadcast = false;
    private boolean isUpdatingFromSliders = false;
    private boolean isInitializing = true;

    private static final String[] PROFILES = {"Default", "Profile 1", "Profile 2"};
    private static final String[] UI_THEMES = {"System Default (Minimal)", "OpenAOD Classic (Vibrant)"};
    private static final String[] UI_THEME_KEYS = {PreferenceUtils.UI_THEME_SYSTEM, PreferenceUtils.UI_THEME_CLASSIC};

    private View stepperSize, stepperDuration, stepperMinAlpha, stepperMaxAlpha, stepperTimeout;

    private final BroadcastReceiver positionReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (OpenAODOverlayService.ACTION_POSITION_UPDATE.equals(intent.getAction())) {
                int x = intent.getIntExtra("x", 0);
                int y = intent.getIntExtra("y", 0);
                isUpdatingFromBroadcast = true;
                etX.setText(String.valueOf(x));
                etY.setText(String.valueOf(y));
                isUpdatingFromBroadcast = false;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        applyUITheme();
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        bindViews();
        setupSpinners();
        loadGlobalSettings();
        loadShapeSettings(PreferenceUtils.getCurrentShape(this));
        setupListeners();
        
        isInitializing = false;
    }

    private void applyUITheme() {
        String theme = PreferenceUtils.getPrefs(this).getString(PreferenceUtils.KEY_UI_THEME, PreferenceUtils.UI_THEME_SYSTEM);
        if (PreferenceUtils.UI_THEME_SYSTEM.equalsIgnoreCase(theme)) {
            setTheme(R.style.Theme_OpenAODNotify_System);
        } else {
            setTheme(R.style.Theme_OpenAODNotify_Classic);
        }
    }

    private void bindViews() {
        spnUITheme = findViewById(R.id.spnUITheme);
        spnProfile = findViewById(R.id.spnProfile);
        spnShape = findViewById(R.id.spnShape);
        etX = findViewById(R.id.etX);
        etY = findViewById(R.id.etY);
        etColor = findViewById(R.id.etColor);
        colorPreview = findViewById(R.id.color_preview);
        seekRed = findViewById(R.id.seek_red);
        seekGreen = findViewById(R.id.seek_green);
        seekBlue = findViewById(R.id.seek_blue);
        btnSave = findViewById(R.id.btnSave);
        layoutPosition = findViewById(R.id.layoutPosition);
        layoutLineSides = findViewById(R.id.layoutLineSides);
        cbTop = findViewById(R.id.cbTop);
        cbBottom = findViewById(R.id.cbBottom);
        cbLeft = findViewById(R.id.cbLeft);
        cbRight = findViewById(R.id.cbRight);
        swRounded = findViewById(R.id.swRounded);
        btnUp = findViewById(R.id.btnUp);
        btnDown = findViewById(R.id.btnDown);
        btnLeft = findViewById(R.id.btnLeft);
        btnRight = findViewById(R.id.btnRight);

        stepperSize = findViewById(R.id.stepperSize);
        stepperDuration = findViewById(R.id.stepperDuration);
        stepperMinAlpha = findViewById(R.id.stepperMinAlpha);
        stepperMaxAlpha = findViewById(R.id.stepperMaxAlpha);
        stepperTimeout = findViewById(R.id.stepperTimeout);
    }

    private void setupSpinners() {
        ArrayAdapter<String> uiAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, UI_THEMES);
        uiAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spnUITheme.setAdapter(uiAdapter);

        ArrayAdapter<String> profileAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, PROFILES);
        profileAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spnProfile.setAdapter(profileAdapter);

        PreferenceUtils.ShapeType[] shapes = PreferenceUtils.ShapeType.values();
        String[] labels = new String[shapes.length];
        for(int i=0; i<shapes.length; i++) labels[i] = shapes[i].getLabel();
        ArrayAdapter<String> shapeAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, labels);
        shapeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spnShape.setAdapter(shapeAdapter);
    }

    private void loadGlobalSettings() {
        String currentTheme = PreferenceUtils.getPrefs(this).getString(PreferenceUtils.KEY_UI_THEME, PreferenceUtils.UI_THEME_SYSTEM);
        for (int i = 0; i < UI_THEME_KEYS.length; i++) {
            if (UI_THEME_KEYS[i].equalsIgnoreCase(currentTheme)) {
                spnUITheme.setSelection(i, false);
                break;
            }
        }

        String currentProfile = PreferenceUtils.getPrefs(this).getString(PreferenceUtils.KEY_CURRENT_PROFILE, PreferenceUtils.PROFILE_DEFAULT);
        for (int i = 0; i < PROFILES.length; i++) {
            if (PROFILES[i].equalsIgnoreCase(currentProfile)) {
                spnProfile.setSelection(i, false);
                break;
            }
        }

        etColor.setText(PreferenceUtils.getColor(this));
        updateSlidersFromHex(PreferenceUtils.getColor(this));
        ((EditText) stepperDuration.findViewById(R.id.etValue)).setText(String.valueOf(PreferenceUtils.getDuration(this)));
        ((EditText) stepperMinAlpha.findViewById(R.id.etValue)).setText(String.valueOf(PreferenceUtils.getMinAlpha(this)));
        ((EditText) stepperMaxAlpha.findViewById(R.id.etValue)).setText(String.valueOf(PreferenceUtils.getMaxAlpha(this)));
        ((EditText) stepperTimeout.findViewById(R.id.etValue)).setText(String.valueOf(PreferenceUtils.getTimeout(this)));
        spnShape.setSelection(PreferenceUtils.getCurrentShape(this).getId(), false);
    }

    private void loadShapeSettings(PreferenceUtils.ShapeType shape) {
        ((EditText) stepperSize.findViewById(R.id.etValue)).setText(String.valueOf(PreferenceUtils.getShapeSize(this, shape)));
        etX.setText(String.valueOf(PreferenceUtils.getShapeX(this, shape)));
        etY.setText(String.valueOf(PreferenceUtils.getShapeY(this, shape)));
        swRounded.setChecked(PreferenceUtils.isShapeRounded(this, shape));
        
        if (shape == PreferenceUtils.ShapeType.LINES) {
            int sides = PreferenceUtils.getLineSides(this);
            cbTop.setChecked((sides & PreferenceUtils.SIDE_TOP) != 0);
            cbBottom.setChecked((sides & PreferenceUtils.SIDE_BOTTOM) != 0);
            cbLeft.setChecked((sides & PreferenceUtils.SIDE_LEFT) != 0);
            cbRight.setChecked((sides & PreferenceUtils.SIDE_RIGHT) != 0);
        }
        updateUIVisibility(shape);
    }

    private void updateUIVisibility(PreferenceUtils.ShapeType shape) {
        layoutPosition.setVisibility(shape.isDraggable() ? View.VISIBLE : View.GONE);
        layoutLineSides.setVisibility(shape == PreferenceUtils.ShapeType.LINES ? View.VISIBLE : View.GONE);
        swRounded.setVisibility(shape == PreferenceUtils.ShapeType.RECTANGLE || shape == PreferenceUtils.ShapeType.LINES ? View.VISIBLE : View.GONE);
    }

    private void setupListeners() {
        spnUITheme.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (isInitializing) return;
                String newKey = UI_THEME_KEYS[position];
                String currentKey = PreferenceUtils.getPrefs(SettingsActivity.this).getString(PreferenceUtils.KEY_UI_THEME, PreferenceUtils.UI_THEME_SYSTEM);
                
                if (!newKey.equalsIgnoreCase(currentKey)) {
                    PreferenceUtils.getPrefs(SettingsActivity.this).edit().putString(PreferenceUtils.KEY_UI_THEME, newKey).apply();
                    recreate();
                }
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });

        spnProfile.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (isInitializing) return;
                PreferenceUtils.getPrefs(SettingsActivity.this).edit().putString(PreferenceUtils.KEY_CURRENT_PROFILE, PROFILES[position]).apply();
                loadGlobalSettings();
                loadShapeSettings(PreferenceUtils.getCurrentShape(SettingsActivity.this));
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });

        spnShape.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (isInitializing) return;
                PreferenceUtils.ShapeType newShape = PreferenceUtils.ShapeType.fromId(position);
                loadShapeSettings(newShape);
                updatePreview();
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });

        TextWatcher previewWatcher = new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable s) { 
                if (!isInitializing && !isUpdatingFromBroadcast) updatePreview(); 
            }
        };

        etColor.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable s) {
                if (!isUpdatingFromSliders) updateSlidersFromHex(s.toString());
                if (!isInitializing) updatePreview();
            }
        });

        etX.addTextChangedListener(previewWatcher);
        etY.addTextChangedListener(previewWatcher);
        swRounded.setOnCheckedChangeListener((v, c) -> { if(!isInitializing) updatePreview(); });
        
        View.OnClickListener sideListener = v -> { if(!isInitializing) updatePreview(); };
        cbTop.setOnClickListener(sideListener); cbBottom.setOnClickListener(sideListener);
        cbLeft.setOnClickListener(sideListener); cbRight.setOnClickListener(sideListener);

        setupRGBListeners();
        btnSave.setOnClickListener(v -> saveAll());
        setupNudgeButtons();
        
        setupStepper(stepperSize, "Size / Thickness (px)", 1, 1, 200, previewWatcher);
        setupStepper(stepperDuration, "Duration (ms)", 100, 100, 10000, previewWatcher);
        setupStepper(stepperMinAlpha, "Min Opacity", 0.01f, 0.0f, 1.0f, previewWatcher);
        setupStepper(stepperMaxAlpha, "Max Opacity", 0.01f, 0.0f, 1.0f, previewWatcher);
        setupStepper(stepperTimeout, "Timeout (min)", 1, 1, 720, previewWatcher);
    }

    private void setupRGBListeners() {
        SeekBar.OnSeekBarChangeListener listener = new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    isUpdatingFromSliders = true;
                    String hex = String.format("%02X%02X%02X", seekRed.getProgress(), seekGreen.getProgress(), seekBlue.getProgress());
                    etColor.setText(hex);
                    colorPreview.setBackgroundColor(Color.parseColor("#" + hex));
                    isUpdatingFromSliders = false;
                }
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        };
        seekRed.setOnSeekBarChangeListener(listener);
        seekGreen.setOnSeekBarChangeListener(listener);
        seekBlue.setOnSeekBarChangeListener(listener);
    }

    private void updateSlidersFromHex(String hex) {
        if (PreferenceUtils.isValidColor(hex)) {
            try {
                int color = Color.parseColor("#" + hex);
                seekRed.setProgress(Color.red(color));
                seekGreen.setProgress(Color.green(color));
                seekBlue.setProgress(Color.blue(color));
                colorPreview.setBackgroundColor(color);
            } catch (Exception ignored) {}
        }
    }

    private void setupNudgeButtons() {
        btnUp.setOnClickListener(v -> nudge(0, -1));
        btnDown.setOnClickListener(v -> nudge(0, 1));
        btnLeft.setOnClickListener(v -> nudge(-1, 0));
        btnRight.setOnClickListener(v -> nudge(1, 0));
    }

    private void setupStepper(View layout, String label, float step, float min, float max, TextWatcher watcher) {
        ((TextView) layout.findViewById(R.id.tvLabel)).setText(label);
        EditText et = layout.findViewById(R.id.etValue);
        et.addTextChangedListener(watcher);
        layout.findViewById(R.id.btnMinus).setOnClickListener(v -> {
            try {
                float val = Float.parseFloat(et.getText().toString());
                et.setText(String.valueOf(Math.max(min, val - step)));
            } catch (Exception ignored) {}
        });
        layout.findViewById(R.id.btnPlus).setOnClickListener(v -> {
            try {
                float val = Float.parseFloat(et.getText().toString());
                et.setText(String.valueOf(Math.min(max, val + step)));
            } catch (Exception ignored) {}
        });
    }

    private void nudge(int dx, int dy) {
        try {
            etX.setText(String.valueOf(Integer.parseInt(etX.getText().toString()) + dx));
            etY.setText(String.valueOf(Integer.parseInt(etY.getText().toString()) + dy));
        } catch (Exception ignored) {}
    }

    private int getSelectedSides() {
        int sides = 0;
        if (cbTop.isChecked()) sides |= PreferenceUtils.SIDE_TOP;
        if (cbBottom.isChecked()) sides |= PreferenceUtils.SIDE_BOTTOM;
        if (cbLeft.isChecked()) sides |= PreferenceUtils.SIDE_LEFT;
        if (cbRight.isChecked()) sides |= PreferenceUtils.SIDE_RIGHT;
        return sides;
    }

    private void updatePreview() {
        try {
            Intent intent = new Intent(this, OpenAODOverlayService.class);
            intent.setAction(OpenAODOverlayService.ACTION_START);
            intent.putExtra("preview", true);
            intent.putExtra("shape", spnShape.getSelectedItemPosition());
            intent.putExtra("color", etColor.getText().toString());
            intent.putExtra("x", Integer.parseInt(etX.getText().toString()));
            intent.putExtra("y", Integer.parseInt(etY.getText().toString()));
            intent.putExtra("size", (int)Float.parseFloat(((EditText) stepperSize.findViewById(R.id.etValue)).getText().toString()));
            intent.putExtra("duration", (int)Float.parseFloat(((EditText) stepperDuration.findViewById(R.id.etValue)).getText().toString()));
            intent.putExtra("min_alpha", Float.parseFloat(((EditText) stepperMinAlpha.findViewById(R.id.etValue)).getText().toString()));
            intent.putExtra("max_alpha", Float.parseFloat(((EditText) stepperMaxAlpha.findViewById(R.id.etValue)).getText().toString()));
            intent.putExtra("rounded", swRounded.isChecked());
            intent.putExtra("sides", getSelectedSides());
            startService(intent);
        } catch (Exception ignored) {}
    }

    private void saveAll() {
        try {
            PreferenceUtils.ShapeType currentShape = PreferenceUtils.ShapeType.fromId(spnShape.getSelectedItemPosition());
            PreferenceUtils.saveGlobalSettings(this, etColor.getText().toString(), 
                (int)Float.parseFloat(((EditText) stepperDuration.findViewById(R.id.etValue)).getText().toString()),
                Float.parseFloat(((EditText) stepperMinAlpha.findViewById(R.id.etValue)).getText().toString()),
                Float.parseFloat(((EditText) stepperMaxAlpha.findViewById(R.id.etValue)).getText().toString()),
                (int)Float.parseFloat(((EditText) stepperTimeout.findViewById(R.id.etValue)).getText().toString()),
                currentShape.getId());
            
            PreferenceUtils.saveShapeSettings(this, currentShape,
                (int)Float.parseFloat(((EditText) stepperSize.findViewById(R.id.etValue)).getText().toString()),
                Integer.parseInt(etX.getText().toString()),
                Integer.parseInt(etY.getText().toString()),
                swRounded.isChecked(), getSelectedSides());

            Toast.makeText(this, "Settings Saved", Toast.LENGTH_SHORT).show();
            stopOverlayService();
            finish();
        } catch (Exception e) { Toast.makeText(this, "Check inputs", Toast.LENGTH_SHORT).show(); }
    }

    private void stopOverlayService() {
        Intent intent = new Intent(this, OpenAODOverlayService.class);
        intent.setAction(OpenAODOverlayService.ACTION_STOP);
        startService(intent);
    }

    @Override protected void onResume() {
        super.onResume();
        registerReceiver(positionReceiver, new IntentFilter(OpenAODOverlayService.ACTION_POSITION_UPDATE), Context.RECEIVER_EXPORTED);
    }

    @Override protected void onPause() {
        super.onPause();
        unregisterReceiver(positionReceiver);
        if (isFinishing()) stopOverlayService();
    }

    @Override protected void onDestroy() {
        stopOverlayService();
        super.onDestroy();
    }
}
