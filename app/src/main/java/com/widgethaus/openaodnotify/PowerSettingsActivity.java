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

public class PowerSettingsActivity extends AppCompatActivity {
    private SwitchMaterial swEnabled, swRounded;
    private Spinner spnShape;
    private View viewPlugged, viewCharging, viewLow;
    private TextView tvEditing;
    private SeekBar seekRed, seekGreen, seekBlue;
    private EditText etX, etY;
    private ImageButton btnUp, btnDown, btnLeft, btnRight;
    private CheckBox cbTop, cbBottom, cbLeft, cbRight;
    private LinearLayout layoutPosition, layoutLineSides;
    private View stepperSize;
    
    private String colorPlugged, colorCharging, colorLow;
    private int currentEditingState = 0; // 0: Plugged, 1: Charging, 2: Low
    private boolean isInitializing = true;
    private boolean isUpdatingFromSliders = false;
    private boolean isUpdatingFromBroadcast = false;

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
        setContentView(R.layout.activity_power_settings);

        bindViews();
        setupSpinners();
        loadSettings();
        setupListeners();
        
        isInitializing = false;
        updatePreview();
    }

    private void bindViews() {
        swEnabled = findViewById(R.id.swPowerStatus);
        swRounded = findViewById(R.id.swPowerRounded);
        spnShape = findViewById(R.id.spnPowerShape);
        viewPlugged = findViewById(R.id.viewColorPlugged);
        viewCharging = findViewById(R.id.viewColorCharging);
        viewLow = findViewById(R.id.viewColorLow);
        tvEditing = findViewById(R.id.tvEditingColor);
        seekRed = findViewById(R.id.seekPowerRed);
        seekGreen = findViewById(R.id.seekPowerGreen);
        seekBlue = findViewById(R.id.seekPowerBlue);
        etX = findViewById(R.id.etPowerX);
        etY = findViewById(R.id.etPowerY);
        btnUp = findViewById(R.id.btnPowerUp);
        btnDown = findViewById(R.id.btnPowerDown);
        btnLeft = findViewById(R.id.btnPowerLeft);
        btnRight = findViewById(R.id.btnPowerRight);
        cbTop = findViewById(R.id.cbPowerTop);
        cbBottom = findViewById(R.id.cbPowerBottom);
        cbLeft = findViewById(R.id.cbPowerLeft);
        cbRight = findViewById(R.id.cbPowerRight);
        layoutPosition = findViewById(R.id.layoutPowerPosition);
        layoutLineSides = findViewById(R.id.layoutPowerLineSides);
        stepperSize = findViewById(R.id.stepperPowerSize);

        findViewById(R.id.btnSelectPlugged).setOnClickListener(v -> setEditingState(0));
        findViewById(R.id.btnSelectCharging).setOnClickListener(v -> setEditingState(1));
        findViewById(R.id.btnSelectLow).setOnClickListener(v -> setEditingState(2));
        findViewById(R.id.btnPowerSave).setOnClickListener(v -> saveAll());
    }

    private void setupSpinners() {
        PreferenceUtils.ShapeType[] shapes = PreferenceUtils.ShapeType.values();
        String[] labels = new String[shapes.length];
        for(int i=0; i<shapes.length; i++) labels[i] = shapes[i].getLabel();
        ArrayAdapter<String> shapeAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, labels);
        shapeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spnShape.setAdapter(shapeAdapter);
    }

    private void loadSettings() {
        swEnabled.setChecked(PreferenceUtils.isPowerStatusEnabled(this));
        colorPlugged = PreferenceUtils.getPowerStatusColorPlugged(this);
        colorCharging = PreferenceUtils.getPowerStatusColorCharging(this);
        colorLow = PreferenceUtils.getPowerStatusColorLow(this);
        
        viewPlugged.setBackgroundColor(Color.parseColor("#" + colorPlugged));
        viewCharging.setBackgroundColor(Color.parseColor("#" + colorCharging));
        viewLow.setBackgroundColor(Color.parseColor("#" + colorLow));

        PreferenceUtils.ShapeType shape = PreferenceUtils.getPowerStatusShape(this);
        spnShape.setSelection(shape.getId(), false);
        swRounded.setChecked(PreferenceUtils.isPowerShapeRounded(this, shape));
        etX.setText(String.valueOf(PreferenceUtils.getPowerShapeX(this, shape)));
        etY.setText(String.valueOf(PreferenceUtils.getPowerShapeY(this, shape)));
        ((EditText) stepperSize.findViewById(R.id.etValue)).setText(String.valueOf(PreferenceUtils.getPowerShapeSize(this, shape)));
        
        int sides = PreferenceUtils.getPowerLineSides(this);
        cbTop.setChecked((sides & PreferenceUtils.SIDE_TOP) != 0);
        cbBottom.setChecked((sides & PreferenceUtils.SIDE_BOTTOM) != 0);
        cbLeft.setChecked((sides & PreferenceUtils.SIDE_LEFT) != 0);
        cbRight.setChecked((sides & PreferenceUtils.SIDE_RIGHT) != 0);

        setEditingState(0);
        updateUIVisibility(shape);
    }

    private void setEditingState(int state) {
        currentEditingState = state;
        String hex = "";
        switch (state) {
            case 0: hex = colorPlugged; tvEditing.setText("Editing: Plugged"); break;
            case 1: hex = colorCharging; tvEditing.setText("Editing: Charging"); break;
            case 2: hex = colorLow; tvEditing.setText("Editing: Low"); break;
        }
        updateSlidersFromHex(hex);
        updatePreview();
    }

    private void updateUIVisibility(PreferenceUtils.ShapeType shape) {
        layoutPosition.setVisibility(shape.isDraggable() ? View.VISIBLE : View.GONE);
        layoutLineSides.setVisibility(shape == PreferenceUtils.ShapeType.LINES ? View.VISIBLE : View.GONE);
        swRounded.setVisibility(shape == PreferenceUtils.ShapeType.RECTANGLE || shape == PreferenceUtils.ShapeType.LINES ? View.VISIBLE : View.GONE);
    }

    private void setupListeners() {
        spnShape.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (isInitializing) return;
                PreferenceUtils.ShapeType newShape = PreferenceUtils.ShapeType.fromId(position);
                updateUIVisibility(newShape);
                updatePreview();
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });

        SeekBar.OnSeekBarChangeListener colorListener = new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    isUpdatingFromSliders = true;
                    String hex = String.format("%02X%02X%02X", seekRed.getProgress(), seekGreen.getProgress(), seekBlue.getProgress());
                    updateColorState(hex);
                    isUpdatingFromSliders = false;
                    updatePreview();
                }
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        };
        seekRed.setOnSeekBarChangeListener(colorListener);
        seekGreen.setOnSeekBarChangeListener(colorListener);
        seekBlue.setOnSeekBarChangeListener(colorListener);

        TextWatcher previewWatcher = new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable s) { if (!isInitializing && !isUpdatingFromBroadcast) updatePreview(); }
        };

        etX.addTextChangedListener(previewWatcher);
        etY.addTextChangedListener(previewWatcher);
        swRounded.setOnCheckedChangeListener((v, c) -> { if(!isInitializing) updatePreview(); });
        swEnabled.setOnCheckedChangeListener((v, c) -> { if(!isInitializing) updatePreview(); });

        View.OnClickListener sideListener = v -> { if(!isInitializing) updatePreview(); };
        cbTop.setOnClickListener(sideListener); cbBottom.setOnClickListener(sideListener);
        cbLeft.setOnClickListener(sideListener); cbRight.setOnClickListener(sideListener);

        setupStepper(stepperSize, "Size / Thickness (px)", 1, 1, 200, previewWatcher);
        
        btnUp.setOnClickListener(v -> nudge(0, -1));
        btnDown.setOnClickListener(v -> nudge(0, 1));
        btnLeft.setOnClickListener(v -> nudge(-1, 0));
        btnRight.setOnClickListener(v -> nudge(1, 0));
    }

    private void updateColorState(String hex) {
        switch (currentEditingState) {
            case 0: colorPlugged = hex; viewPlugged.setBackgroundColor(Color.parseColor("#" + hex)); break;
            case 1: colorCharging = hex; viewCharging.setBackgroundColor(Color.parseColor("#" + hex)); break;
            case 2: colorLow = hex; viewLow.setBackgroundColor(Color.parseColor("#" + hex)); break;
        }
    }

    private void updateSlidersFromHex(String hex) {
        try {
            int color = Color.parseColor("#" + hex);
            seekRed.setProgress(Color.red(color));
            seekGreen.setProgress(Color.green(color));
            seekBlue.setProgress(Color.blue(color));
        } catch (Exception ignored) {}
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
        if (!swEnabled.isChecked()) {
            stopOverlayService();
            return;
        }
        try {
            String currentHex = (currentEditingState == 0) ? colorPlugged : (currentEditingState == 1 ? colorCharging : colorLow);
            Intent intent = new Intent(this, OpenAODOverlayService.class);
            intent.setAction(OpenAODOverlayService.ACTION_START);
            intent.putExtra("power_preview", true);
            intent.putExtra("shape", spnShape.getSelectedItemPosition());
            intent.putExtra("color", currentHex);
            intent.putExtra("x", Integer.parseInt(etX.getText().toString()));
            intent.putExtra("y", Integer.parseInt(etY.getText().toString()));
            intent.putExtra("size", (int)Float.parseFloat(((EditText) stepperSize.findViewById(R.id.etValue)).getText().toString()));
            intent.putExtra("duration", PreferenceUtils.getDuration(this));
            intent.putExtra("min_alpha", PreferenceUtils.getMinAlpha(this));
            intent.putExtra("max_alpha", PreferenceUtils.getMaxAlpha(this));
            intent.putExtra("rounded", swRounded.isChecked());
            intent.putExtra("sides", getSelectedSides());
            startService(intent);
        } catch (Exception ignored) {}
    }

    private void saveAll() {
        PreferenceUtils.ShapeType shape = PreferenceUtils.ShapeType.fromId(spnShape.getSelectedItemPosition());
        PreferenceUtils.savePowerStatusSettings(this, swEnabled.isChecked(), shape.getId(), colorPlugged, colorLow, colorCharging);
        PreferenceUtils.savePowerShapeSettings(this, shape, 
            (int)Float.parseFloat(((EditText) stepperSize.findViewById(R.id.etValue)).getText().toString()),
            Integer.parseInt(etX.getText().toString()), Integer.parseInt(etY.getText().toString()),
            swRounded.isChecked(), getSelectedSides());

        Intent refreshIntent = new Intent(OpenAODListener.ACTION_REFRESH);
        refreshIntent.setPackage(getPackageName());
        sendBroadcast(refreshIntent);
        
        Toast.makeText(this, "Power Settings Saved", Toast.LENGTH_SHORT).show();
        finish();
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
}
