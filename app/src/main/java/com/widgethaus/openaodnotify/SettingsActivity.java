package com.widgethaus.openaodnotify;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class SettingsActivity extends AppCompatActivity {

    private EditText etTimeout, etColor, etX, etY, etSize, etDuration, etMinAlpha, etMaxAlpha;
    private Spinner spnShape;
    private Button btnSave;
    private LinearLayout layoutPosition;
    private boolean isUpdatingFromBroadcast = false;

    private static final String[] SHAPES = {
        "Circle", "Square", "Rectangle", "Top Line", "Full Border", "Vertical Edges", "Horizontal Edges"
    };

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

        etTimeout = findViewById(R.id.etTimeout);
        etColor = findViewById(R.id.etColor);
        etX = findViewById(R.id.etX);
        etY = findViewById(R.id.etY);
        etSize = findViewById(R.id.etSize);
        etDuration = findViewById(R.id.etDuration);
        etMinAlpha = findViewById(R.id.etMinAlpha);
        etMaxAlpha = findViewById(R.id.etMaxAlpha);
        spnShape = findViewById(R.id.spnShape);
        btnSave = findViewById(R.id.btnSave);
        layoutPosition = findViewById(R.id.layoutPosition);

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, SHAPES);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spnShape.setAdapter(adapter);

        loadSettings();
        setupLivePreview();

        btnSave.setOnClickListener(v -> saveSettings());
    }

    private void loadSettings() {
        SharedPreferences prefs = getSharedPreferences("AOD_PREFS", MODE_PRIVATE);
        etTimeout.setText(String.valueOf(prefs.getInt("timeout", 5)));
        etColor.setText(prefs.getString("color", "0066ff"));
        etX.setText(String.valueOf(prefs.getInt("x", 25)));
        etY.setText(String.valueOf(prefs.getInt("y", 25)));
        etSize.setText(String.valueOf(prefs.getInt("size", 60)));
        etDuration.setText(String.valueOf(prefs.getInt("duration", 2500)));
        etMinAlpha.setText(String.valueOf(prefs.getFloat("min_alpha", 0.1f)));
        etMaxAlpha.setText(String.valueOf(prefs.getFloat("max_alpha", 1.0f)));
        
        int shapeIndex = prefs.getInt("shape", 0);
        spnShape.setSelection(shapeIndex);
        updatePositionVisibility(shapeIndex);
    }

    private void setupLivePreview() {
        TextWatcher watcher = new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable s) { 
                if (!isUpdatingFromBroadcast) {
                    updatePreview(); 
                }
            }
        };

        etColor.addTextChangedListener(watcher);
        etX.addTextChangedListener(watcher);
        etY.addTextChangedListener(watcher);
        etSize.addTextChangedListener(watcher);
        etDuration.addTextChangedListener(watcher);
        etMinAlpha.addTextChangedListener(watcher);
        etMaxAlpha.addTextChangedListener(watcher);

        spnShape.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> parent, View view, int position, long id) { 
                updatePositionVisibility(position);
                updatePreview(); 
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void updatePositionVisibility(int position) {
        // Positions are only relevant for Circle (0), Square (1), and Rectangle (2)
        if (position <= 2) {
            layoutPosition.setVisibility(View.VISIBLE);
        } else {
            layoutPosition.setVisibility(View.GONE);
        }
    }

    private void updatePreview() {
        try {
            Intent intent = new Intent(this, OpenAODOverlayService.class);
            intent.putExtra("preview", true);
            intent.putExtra("color", etColor.getText().toString().trim());
            
            String xStr = etX.getText().toString();
            String yStr = etY.getText().toString();
            intent.putExtra("x", xStr.isEmpty() ? 0 : Integer.parseInt(xStr));
            intent.putExtra("y", yStr.isEmpty() ? 0 : Integer.parseInt(yStr));
            
            intent.putExtra("size", Integer.parseInt(etSize.getText().toString()));
            intent.putExtra("duration", Integer.parseInt(etDuration.getText().toString()));
            intent.putExtra("min_alpha", Float.parseFloat(etMinAlpha.getText().toString()));
            intent.putExtra("max_alpha", Float.parseFloat(etMaxAlpha.getText().toString()));
            intent.putExtra("shape", spnShape.getSelectedItemPosition());
            
            startForegroundService(intent);
        } catch (Exception ignored) {}
    }

    private void saveSettings() {
        try {
            int timeout = Integer.parseInt(etTimeout.getText().toString());
            String color = etColor.getText().toString().trim();
            int x = Integer.parseInt(etX.getText().toString());
            int y = Integer.parseInt(etY.getText().toString());
            int size = Integer.parseInt(etSize.getText().toString());
            int duration = Integer.parseInt(etDuration.getText().toString());
            float minAlpha = Float.parseFloat(etMinAlpha.getText().toString());
            float maxAlpha = Float.parseFloat(etMaxAlpha.getText().toString());
            int shape = spnShape.getSelectedItemPosition();

            SharedPreferences.Editor editor = getSharedPreferences("AOD_PREFS", MODE_PRIVATE).edit();
            editor.putInt("timeout", timeout);
            editor.putString("color", color);
            editor.putInt("x", x);
            editor.putInt("y", y);
            editor.putInt("size", size);
            editor.putInt("duration", duration);
            editor.putFloat("min_alpha", minAlpha);
            editor.putFloat("max_alpha", maxAlpha);
            editor.putInt("shape", shape);
            editor.apply();

            Toast.makeText(this, "Settings Saved", Toast.LENGTH_SHORT).show();
            stopService(new Intent(this, OpenAODOverlayService.class));
            finish();
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Please check your inputs", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        IntentFilter filter = new IntentFilter(OpenAODOverlayService.ACTION_POSITION_UPDATE);
        registerReceiver(positionReceiver, filter, Context.RECEIVER_EXPORTED);
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(positionReceiver);
        if (!isFinishing()) {
            stopService(new Intent(this, OpenAODOverlayService.class));
        }
    }
}
