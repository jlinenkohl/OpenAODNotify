package com.widgethaus.openaodnotify;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
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

    private EditText etX, etY, etColor;
    private View colorPreview;
    private SeekBar seekRed, seekGreen, seekBlue;
    private Spinner spnShape, spnProfile;
    private Button btnSave;
    private ImageButton btnUp, btnDown, btnLeft, btnRight;
    private LinearLayout layoutPosition;
    private SwitchMaterial swRounded;
    private boolean isUpdatingFromBroadcast = false;
    private boolean isUpdatingFromSliders = false;

    private static final String[] SHAPES = {
        "Circle", "Square", "Rectangle", "Ring", "Top Line", "Full Border", "Vertical Edges", "Horizontal Edges"
    };
    private static final String[] PROFILES = {"Default", "Profile 1", "Profile 2"};

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
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        bindViews();
        setupSpinners();
        loadSettings();
        setupListeners();
    }

    private void bindViews() {
        spnProfile = findViewById(R.id.spnProfile);
        etX = findViewById(R.id.etX);
        etY = findViewById(R.id.etY);
        etColor = findViewById(R.id.etColor);
        colorPreview = findViewById(R.id.color_preview);
        seekRed = findViewById(R.id.seek_red);
        seekGreen = findViewById(R.id.seek_green);
        seekBlue = findViewById(R.id.seek_blue);
        spnShape = findViewById(R.id.spnShape);
        btnSave = findViewById(R.id.btnSave);
        layoutPosition = findViewById(R.id.layoutPosition);
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
        ArrayAdapter<String> profileAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, PROFILES);
        profileAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spnProfile.setAdapter(profileAdapter);

        ArrayAdapter<String> shapeAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, SHAPES);
        shapeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spnShape.setAdapter(shapeAdapter);
    }

    private void loadSettings() {
        String currentProfile = PreferenceUtils.getPrefs(this).getString(PreferenceUtils.KEY_CURRENT_PROFILE, PreferenceUtils.PROFILE_DEFAULT);
        int profileIndex = 0;
        for (int i = 0; i < PROFILES.length; i++) {
            if (PROFILES[i].equalsIgnoreCase(currentProfile)) {
                profileIndex = i;
                break;
            }
        }
        spnProfile.setSelection(profileIndex);

        ((EditText) stepperTimeout.findViewById(R.id.etValue)).setText(String.valueOf(PreferenceUtils.getInt(this, "timeout", 5)));
        String color = PreferenceUtils.getString(this, "color", "0066ff");
        etColor.setText(color);
        updateSlidersFromHex(color);
        
        etX.setText(String.valueOf(PreferenceUtils.getInt(this, "x", 64)));
        etY.setText(String.valueOf(PreferenceUtils.getInt(this, "y", 64)));
        ((EditText) stepperSize.findViewById(R.id.etValue)).setText(String.valueOf(PreferenceUtils.getInt(this, "size", 60)));
        ((EditText) stepperDuration.findViewById(R.id.etValue)).setText(String.valueOf(PreferenceUtils.getInt(this, "duration", 2500)));
        ((EditText) stepperMinAlpha.findViewById(R.id.etValue)).setText(String.valueOf(PreferenceUtils.getFloat(this, "min_alpha", 0.1f)));
        ((EditText) stepperMaxAlpha.findViewById(R.id.etValue)).setText(String.valueOf(PreferenceUtils.getFloat(this, "max_alpha", 1.0f)));
        swRounded.setChecked(PreferenceUtils.getBoolean(this, "rounded", true));
        
        int shapeIndex = PreferenceUtils.getInt(this, "shape", 0);
        spnShape.setSelection(shapeIndex);
        updatePositionVisibility(PreferenceUtils.ShapeType.fromId(shapeIndex));
    }

    private void setupListeners() {
        spnProfile.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selectedProfile = PROFILES[position];
                PreferenceUtils.getPrefs(SettingsActivity.this).edit().putString(PreferenceUtils.KEY_CURRENT_PROFILE, selectedProfile).apply();
                loadSettings();
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });

        TextWatcher textWatcher = new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable s) { 
                if (!isUpdatingFromBroadcast) {
                    if (!isUpdatingFromSliders) {
                        updateSlidersFromHex(s.toString());
                    }
                    updatePreview(); 
                }
            }
        };

        etColor.addTextChangedListener(textWatcher);
        etX.addTextChangedListener(textWatcher);
        etY.addTextChangedListener(textWatcher);
        swRounded.setOnCheckedChangeListener((buttonView, isChecked) -> updatePreview());

        spnShape.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> parent, View view, int position, long id) { 
                updatePositionVisibility(PreferenceUtils.ShapeType.fromId(position));
                updatePreview(); 
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });

        setupRGBListeners();
        btnSave.setOnClickListener(v -> saveSettings());
        setupNudgeButtons();
        
        setupStepper(stepperSize, "Size / Stroke (px)", 1, 1, 200, textWatcher);
        setupStepper(stepperDuration, "Duration (ms)", 100, 100, 10000, textWatcher);
        setupStepper(stepperMinAlpha, "Min Opacity (0.0-1.0)", 0.1f, 0.0f, 1.0f, textWatcher);
        setupStepper(stepperMaxAlpha, "Max Opacity (0.0-1.0)", 0.1f, 0.0f, 1.0f, textWatcher);
        setupStepper(stepperTimeout, "Timeout (minutes)", 1, 1, 720, textWatcher);
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
        TextView tvLabel = layout.findViewById(R.id.tvLabel);
        EditText etValue = layout.findViewById(R.id.etValue);
        ImageButton btnMinus = layout.findViewById(R.id.btnMinus);
        ImageButton btnPlus = layout.findViewById(R.id.btnPlus);

        tvLabel.setText(label);
        etValue.addTextChangedListener(watcher);

        btnMinus.setOnClickListener(v -> {
            try {
                float val = Float.parseFloat(etValue.getText().toString());
                val = Math.max(min, val - step);
                etValue.setText(String.valueOf(val));
            } catch (Exception ignored) {}
        });

        btnPlus.setOnClickListener(v -> {
            try {
                float val = Float.parseFloat(etValue.getText().toString());
                val = Math.min(max, val + step);
                etValue.setText(String.valueOf(val));
            } catch (Exception ignored) {}
        });
    }

    private void nudge(int dx, int dy) {
        try {
            int x = Integer.parseInt(etX.getText().toString()) + dx;
            int y = Integer.parseInt(etY.getText().toString()) + dy;
            etX.setText(String.valueOf(x));
            etY.setText(String.valueOf(y));
        } catch (NumberFormatException ignored) {}
    }

    private void updatePositionVisibility(PreferenceUtils.ShapeType shapeType) {
        layoutPosition.setVisibility(shapeType.isDraggable() ? View.VISIBLE : View.GONE);
        swRounded.setVisibility(shapeType == PreferenceUtils.ShapeType.RECTANGLE || shapeType == PreferenceUtils.ShapeType.FULL_BORDER ? View.VISIBLE : View.GONE);
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
            startService(intent);
        } catch (Exception ignored) {}
    }

    private void saveSettings() {
        try {
            PreferenceUtils.saveSettings(
                this,
                (int)Float.parseFloat(((EditText) stepperTimeout.findViewById(R.id.etValue)).getText().toString()),
                etColor.getText().toString(),
                Integer.parseInt(etX.getText().toString()),
                Integer.parseInt(etY.getText().toString()),
                (int)Float.parseFloat(((EditText) stepperSize.findViewById(R.id.etValue)).getText().toString()),
                (int)Float.parseFloat(((EditText) stepperDuration.findViewById(R.id.etValue)).getText().toString()),
                Float.parseFloat(((EditText) stepperMinAlpha.findViewById(R.id.etValue)).getText().toString()),
                Float.parseFloat(((EditText) stepperMaxAlpha.findViewById(R.id.etValue)).getText().toString()),
                spnShape.getSelectedItemPosition(),
                swRounded.isChecked()
            );

            Toast.makeText(this, "Settings Saved", Toast.LENGTH_SHORT).show();
            stopOverlayService();
            finish();
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Please check your inputs", Toast.LENGTH_SHORT).show();
        }
    }

    private void stopOverlayService() {
        Intent intent = new Intent(this, OpenAODOverlayService.class);
        intent.setAction(OpenAODOverlayService.ACTION_STOP);
        startService(intent);
    }

    @Override
    protected void onResume() {
        super.onResume();
        IntentFilter filter = new IntentFilter(OpenAODOverlayService.ACTION_POSITION_UPDATE);
        registerReceiver(positionReceiver, filter, Context.RECEIVER_EXPORTED);
        loadSettings();
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(positionReceiver);
        if (isFinishing()) {
            stopOverlayService();
        }
    }

    @Override
    protected void onDestroy() {
        stopOverlayService();
        super.onDestroy();
    }
}