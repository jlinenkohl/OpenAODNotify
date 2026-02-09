package com.widgethaus.openaodnotify;

import android.accessibilityservice.AccessibilityService;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityEvent;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.core.app.NotificationCompat;

import java.util.ArrayList;
import java.util.List;

public class OpenAODOverlayService extends AccessibilityService {
    private static final String TAG = "OpenAODOverlay";
    public static final String ACTION_START = "com.widgethaus.openaodnotify.START";
    public static final String ACTION_STOP = "com.widgethaus.openaodnotify.STOP";
    public static final String ACTION_POSITION_UPDATE = "com.widgethaus.openaodnotify.POSITION_UPDATE";
    private static final String CHANNEL_ID = "AOD_Overlay_Channel";

    private WindowManager wm;
    private List<View> overlayRoots = new ArrayList<>();
    private List<ObjectAnimator> animators = new ArrayList<>();
    private final Handler handler = new Handler(Looper.getMainLooper());
    private Runnable timeoutRunnable;
    private boolean isOverlayVisible = false;

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {}

    @Override
    public void onInterrupt() {}

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        Log.d(TAG, "Accessibility Service Connected");
        wm = (WindowManager) getSystemService(WINDOW_SERVICE);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
        Notification notification = createServiceNotification();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(1, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE);
        } else {
            startForeground(1, notification);
        }
    }

    private Notification createServiceNotification() {
        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("AOD Overlay Active")
                .setContentText("Monitoring notifications...")
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setPriority(NotificationCompat.PRIORITY_MIN)
                .build();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(
                    CHANNEL_ID, "AOD Overlay Service Channel",
                    NotificationManager.IMPORTANCE_LOW
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) manager.createNotificationChannel(serviceChannel);
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            String action = intent.getAction();
            if (ACTION_STOP.equals(action)) {
                Log.d(TAG, "Manual Stop requested");
                cleanupViews();
            } else {
                boolean isPreview = intent.getBooleanExtra("preview", false);
                if (!isPreview && isOverlayVisible) {
                    Log.d(TAG, "Overlay already active, refreshing timeout");
                    startTimeout(PreferenceUtils.getInt(this, "timeout", 5));
                    return START_STICKY;
                }
                showOverlay(intent);
            }
        }
        return START_STICKY;
    }

    private void showOverlay(Intent intent) {
        if (wm == null) wm = (WindowManager) getSystemService(WINDOW_SERVICE);
        cleanupViews();

        Bundle extras = intent.getExtras();
        boolean isPreview = extras != null && extras.getBoolean("preview", false);
        
        int shapeId = isPreview ? extras.getInt("shape") : PreferenceUtils.getInt(this, "shape", 0);
        PreferenceUtils.ShapeType shapeType = PreferenceUtils.ShapeType.fromId(shapeId);
        
        int size = isPreview ? extras.getInt("size") : PreferenceUtils.getInt(this, "size", 60);
        String colorHex = isPreview ? extras.getString("color") : PreferenceUtils.getString(this, "color", "0066ff");
        int duration = isPreview ? extras.getInt("duration") : PreferenceUtils.getInt(this, "duration", 2500);
        float minAlpha = isPreview ? extras.getFloat("min_alpha") : PreferenceUtils.getFloat(this, "min_alpha", 0.1f);
        float maxAlpha = isPreview ? extras.getFloat("max_alpha") : PreferenceUtils.getFloat(this, "max_alpha", 1.0f);
        boolean rounded = isPreview ? extras.getBoolean("rounded") : PreferenceUtils.getBoolean(this, "rounded", true);

        if (shapeType.isDraggable()) {
            int x = isPreview ? extras.getInt("x") : PreferenceUtils.getInt(this, "x", 64);
            int y = isPreview ? extras.getInt("y") : PreferenceUtils.getInt(this, "y", 64);
            createDraggableShape(shapeType, size, x, y, isPreview, colorHex, rounded, minAlpha, maxAlpha, duration);
        } else {
            createComplexShape(shapeType, size, colorHex, rounded, minAlpha, maxAlpha, duration);
        }
        
        isOverlayVisible = true;
        if (!isPreview) {
            startTimeout(PreferenceUtils.getInt(this, "timeout", 5));
        }
    }

    private WindowManager.LayoutParams getBaseParams() {
        WindowManager.LayoutParams params = new WindowManager.LayoutParams();
        params.type = WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY;
        params.format = PixelFormat.RGBA_8888;
        params.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                | WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                | WindowManager.LayoutParams.FLAG_ALLOW_LOCK_WHILE_SCREEN_ON
                | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
                | WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
                | WindowManager.LayoutParams.FLAG_FULLSCREEN
                | WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            params.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            params.setFitInsetsTypes(0);
        }
        params.alpha = 1.0f;
        params.screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE;
        return params;
    }

    private void createDraggableShape(PreferenceUtils.ShapeType shapeType, int size, int x, int y, 
                                     boolean draggable, String colorHex, boolean rounded,
                                     float minAlpha, float maxAlpha, int duration) {
        int width = (shapeType == PreferenceUtils.ShapeType.RECTANGLE) ? size * 2 : size;
        int handleHeight = (int)(30 * getResources().getDisplayMetrics().density);
        int handleWidth = (int)(60 * getResources().getDisplayMetrics().density);
        int gap = (int)(20 * getResources().getDisplayMetrics().density);
        int layoutWidth = Math.max(width, handleWidth);
        int layoutHeight = size + (draggable ? (gap + handleHeight) : 0);

        WindowManager.LayoutParams params = getBaseParams();
        params.width = layoutWidth; params.height = layoutHeight;
        params.gravity = Gravity.TOP | Gravity.LEFT;
        params.x = x; params.y = y;

        if (!draggable) params.flags |= WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE;

        FrameLayout container = new FrameLayout(this);
        View visualView = new View(this);
        visualView.setLayerType(View.LAYER_TYPE_HARDWARE, null);

        int color = Color.parseColor("#" + colorHex);
        GradientDrawable shape = new GradientDrawable();
        if (shapeType == PreferenceUtils.ShapeType.RING) {
            shape.setShape(GradientDrawable.OVAL); shape.setColor(Color.TRANSPARENT);
            shape.setStroke((int)(4 * getResources().getDisplayMetrics().density), color);
        } else {
            shape.setShape((shapeType == PreferenceUtils.ShapeType.CIRCLE) ? GradientDrawable.OVAL : GradientDrawable.RECTANGLE);
            shape.setColor(color);
            if (rounded && shapeType == PreferenceUtils.ShapeType.RECTANGLE) shape.setCornerRadius(size / 4f);
        }
        visualView.setBackground(shape);
        visualView.setAlpha(maxAlpha);

        FrameLayout.LayoutParams shapeParams = new FrameLayout.LayoutParams(width, size);
        shapeParams.gravity = Gravity.TOP | Gravity.CENTER_HORIZONTAL;
        container.addView(visualView, shapeParams);

        if (draggable) {
            setupHandle(container, size, gap, handleWidth, handleHeight, params);
        }

        try {
            wm.addView(container, params);
            overlayRoots.add(container);
            startBreathing(visualView, minAlpha, maxAlpha, duration);
        } catch (Exception e) { Log.e(TAG, "Error adding window", e); }
    }

    private void setupHandle(FrameLayout container, int size, int gap, int handleWidth, int handleHeight, WindowManager.LayoutParams params) {
        View leash = new View(this);
        leash.setBackgroundColor(Color.parseColor("#80FFFFFF"));
        FrameLayout.LayoutParams leashParams = new FrameLayout.LayoutParams((int)(2 * getResources().getDisplayMetrics().density), gap);
        leashParams.gravity = Gravity.TOP | Gravity.CENTER_HORIZONTAL;
        leashParams.topMargin = size;
        container.addView(leash, leashParams);

        TextView handle = new TextView(this);
        handle.setText("MOVE"); handle.setTextSize(10); handle.setTextColor(Color.WHITE); handle.setGravity(Gravity.CENTER);
        GradientDrawable handleBg = new GradientDrawable();
        handleBg.setColor(Color.parseColor("#AA000000"));
        handleBg.setCornerRadius(10 * getResources().getDisplayMetrics().density);
        handleBg.setStroke(2, Color.WHITE);
        handle.setBackground(handleBg);
        FrameLayout.LayoutParams handleParams = new FrameLayout.LayoutParams(handleWidth, handleHeight);
        handleParams.gravity = Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL;
        container.addView(handle, handleParams);

        container.setOnTouchListener(new View.OnTouchListener() {
            private int initialX, initialY;
            private float initialTouchX, initialTouchY;
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        initialX = params.x; initialY = params.y;
                        initialTouchX = event.getRawX(); initialTouchY = event.getRawY();
                        return true;
                    case MotionEvent.ACTION_MOVE:
                        params.x = initialX + (int) (event.getRawX() - initialTouchX);
                        params.y = initialY + (int) (event.getRawY() - initialTouchY);
                        wm.updateViewLayout(container, params);
                        Intent updateIntent = new Intent(ACTION_POSITION_UPDATE);
                        updateIntent.putExtra("x", params.x); updateIntent.putExtra("y", params.y);
                        sendBroadcast(updateIntent);
                        return true;
                }
                return false;
            }
        });
    }

    private void createComplexShape(PreferenceUtils.ShapeType shapeType, int thickness, String colorHex, boolean rounded,
                                   float minAlpha, float maxAlpha, int duration) {
        if (shapeType == PreferenceUtils.ShapeType.FULL_BORDER) {
            createFullBorder(thickness, colorHex, rounded, minAlpha, maxAlpha, duration);
            return;
        }
        switch (shapeType) {
            case TOP_LINE: createLine(shapeType, thickness, Gravity.TOP, colorHex, minAlpha, maxAlpha, duration); break;
            case VERTICAL_EDGES: 
                createLine(shapeType, thickness, Gravity.LEFT, colorHex, minAlpha, maxAlpha, duration);
                createLine(shapeType, thickness, Gravity.RIGHT, colorHex, minAlpha, maxAlpha, duration);
                break;
            case HORIZONTAL_EDGES:
                createLine(shapeType, thickness, Gravity.TOP, colorHex, minAlpha, maxAlpha, duration);
                createLine(shapeType, thickness, Gravity.BOTTOM, colorHex, minAlpha, maxAlpha, duration);
                break;
            default: break;
        }
    }

    private void createFullBorder(int thickness, String colorHex, boolean rounded, float minAlpha, float maxAlpha, int duration) {
        DisplayMetrics metrics = new DisplayMetrics();
        wm.getDefaultDisplay().getRealMetrics(metrics);
        WindowManager.LayoutParams params = getBaseParams();
        params.width = metrics.widthPixels; params.height = metrics.heightPixels;
        params.flags |= WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE;

        View borderView = new View(this);
        borderView.setLayerType(View.LAYER_TYPE_HARDWARE, null);
        int color = Color.parseColor("#" + colorHex);
        GradientDrawable shape = new GradientDrawable();
        shape.setShape(GradientDrawable.RECTANGLE); shape.setColor(Color.TRANSPARENT);
        shape.setStroke(thickness, color);
        if (rounded) shape.setCornerRadius(40 * getResources().getDisplayMetrics().density);
        borderView.setBackground(shape);
        borderView.setAlpha(maxAlpha);
        
        try {
            wm.addView(borderView, params);
            overlayRoots.add(borderView);
            startBreathing(borderView, minAlpha, maxAlpha, duration);
        } catch (Exception e) { Log.e(TAG, "Error adding border", e); }
    }

    private void createLine(PreferenceUtils.ShapeType shapeType, int thickness, int gravity, String colorHex, 
                           float minAlpha, float maxAlpha, int duration) {
        DisplayMetrics metrics = new DisplayMetrics();
        wm.getDefaultDisplay().getRealMetrics(metrics);
        WindowManager.LayoutParams params = getBaseParams();
        params.width = (gravity == Gravity.TOP || gravity == Gravity.BOTTOM) ? metrics.widthPixels : thickness;
        params.height = (gravity == Gravity.LEFT || gravity == Gravity.RIGHT) ? metrics.heightPixels : thickness;
        params.gravity = gravity;
        params.flags |= WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE;

        View line = new View(this);
        line.setLayerType(View.LAYER_TYPE_HARDWARE, null);
        line.setBackgroundColor(Color.parseColor("#" + colorHex));
        line.setAlpha(maxAlpha);

        try {
            wm.addView(line, params);
            overlayRoots.add(line);
            startBreathing(line, minAlpha, maxAlpha, duration);
        } catch (Exception e) { Log.e(TAG, "Error adding line", e); }
    }

    private void startBreathing(View view, float minAlpha, float maxAlpha, int duration) {
        ObjectAnimator animator = ObjectAnimator.ofFloat(view, "alpha", maxAlpha, minAlpha);
        animator.setDuration(duration);
        animator.setRepeatMode(ValueAnimator.REVERSE);
        animator.setRepeatCount(ValueAnimator.INFINITE);
        animator.start();
        animators.add(animator);
    }

    private void startTimeout(int timeoutMinutes) {
        if (timeoutRunnable != null) handler.removeCallbacks(timeoutRunnable);
        timeoutRunnable = this::cleanupViews;
        handler.postDelayed(timeoutRunnable, (long) timeoutMinutes * 60 * 1000);
    }

    private void cleanupViews() {
        Log.d(TAG, "Cleaning up views");
        for (ObjectAnimator animator : animators) animator.cancel();
        animators.clear();
        for (View root : overlayRoots) {
            try { wm.removeView(root); } catch (Exception ignored) {}
        }
        overlayRoots.clear();
        isOverlayVisible = false;
    }

    @Override
    public void onDestroy() {
        cleanupViews();
        if (timeoutRunnable != null) handler.removeCallbacks(timeoutRunnable);
        super.onDestroy();
    }
}