package com.widgethaus.openaodnotify;

import android.accessibilityservice.AccessibilityService;
import android.animation.ObjectAnimator;
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
    private Intent lastStartIntent;

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {}

    @Override
    public void onInterrupt() {}

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
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
                cleanupViews();
                lastStartIntent = null;
            } else {
                lastStartIntent = intent;
                boolean isPreview = intent.getBooleanExtra("preview", false);
                boolean force = intent.getBooleanExtra("force", false);

                // Optimization: If already visible and not a preview/force, just refresh timeout
                if (!isPreview && !force && isOverlayVisible) {
                    Log.d(TAG, "Overlay already visible, refreshing timeout");
                    startTimeout(PreferenceUtils.getTimeout(this));
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
        if (extras == null) return;
        boolean isPreview = extras.getBoolean("preview", false);

        if (isPreview) {
            PreferenceUtils.ShapeType shapeType = PreferenceUtils.ShapeType.fromId(extras.getInt("shape"));
            int size = extras.getInt("size");
            String colorHex = extras.getString("color");
            int duration = extras.getInt("duration");
            float minAlpha = extras.getFloat("min_alpha");
            float maxAlpha = extras.getFloat("max_alpha");
            boolean rounded = extras.getBoolean("rounded");
            
            if (shapeType.isDraggable()) {
                int x = extras.getInt("x");
                int y = extras.getInt("y");
                createDraggableShape(shapeType, size, x, y, true, colorHex, rounded, minAlpha, maxAlpha, duration);
            } else {
                int sides = extras.getInt("sides");
                createLineShape(size, colorHex, rounded, sides, minAlpha, maxAlpha, duration);
            }
        } else {
            // Production: Draw Notification if active
            if (extras.getBoolean("show_notification", false)) {
                renderShapeInternal(PreferenceUtils.getCurrentShape(this), PreferenceUtils.getColor(this));
            }
            
            // Production: Draw Power Status if active
            if (extras.getBoolean("show_power", false)) {
                String pColor = extras.getString("power_color");
                int pShapeId = extras.getInt("power_shape", 0);
                renderShapeInternal(PreferenceUtils.ShapeType.fromId(pShapeId), pColor);
            }
        }
        
        isOverlayVisible = true;
        if (!isPreview) {
            startTimeout(PreferenceUtils.getTimeout(this));
        }
    }

    private void renderShapeInternal(PreferenceUtils.ShapeType shapeType, String colorHex) {
        int size = PreferenceUtils.getShapeSize(this, shapeType);
        int duration = PreferenceUtils.getDuration(this);
        float minAlpha = PreferenceUtils.getMinAlpha(this);
        float maxAlpha = PreferenceUtils.getMaxAlpha(this);
        boolean rounded = PreferenceUtils.isShapeRounded(this, shapeType);

        if (shapeType.isDraggable()) {
            int x = PreferenceUtils.getShapeX(this, shapeType);
            int y = PreferenceUtils.getShapeY(this, shapeType);
            createDraggableShape(shapeType, size, x, y, false, colorHex, rounded, minAlpha, maxAlpha, duration);
        } else {
            int sides = PreferenceUtils.getLineSides(this);
            createLineShape(size, colorHex, rounded, sides, minAlpha, maxAlpha, duration);
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

    private void createLineShape(int thickness, String colorHex, boolean rounded, int sides, float min, float max, int dur) {
        // If all 4 sides selected, use the optimized Full Border
        if (sides == 15) {
            createFullBorder(thickness, colorHex, rounded, min, max, dur);
            return;
        }

        if ((sides & PreferenceUtils.SIDE_TOP) != 0) createIndividualLine(thickness, Gravity.TOP, colorHex, min, max, dur);
        if ((sides & PreferenceUtils.SIDE_BOTTOM) != 0) createIndividualLine(thickness, Gravity.BOTTOM, colorHex, min, max, dur);
        if ((sides & PreferenceUtils.SIDE_LEFT) != 0) createIndividualLine(thickness, Gravity.LEFT, colorHex, min, max, dur);
        if ((sides & PreferenceUtils.SIDE_RIGHT) != 0) createIndividualLine(thickness, Gravity.RIGHT, colorHex, min, max, dur);
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

    private void createIndividualLine(int thickness, int gravity, String colorHex, float min, float max, int dur) {
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
        line.setAlpha(max);

        try {
            wm.addView(line, params);
            overlayRoots.add(line);
            startBreathing(line, min, max, dur);
        } catch (Exception e) { Log.e(TAG, "Error adding line", e); }
    }

    private void startBreathing(View view, float minAlpha, float maxAlpha, int duration) {
        ObjectAnimator animator = ObjectAnimator.ofFloat(view, "alpha", maxAlpha, minAlpha);
        animator.setDuration(duration);
        animator.setRepeatMode(ObjectAnimator.REVERSE);
        animator.setRepeatCount(ObjectAnimator.INFINITE);
        animator.start();
        animators.add(animator);
    }

    private void startTimeout(int timeoutMinutes) {
        if (timeoutRunnable != null) handler.removeCallbacks(timeoutRunnable);
        timeoutRunnable = this::cleanupViews;
        handler.postDelayed(timeoutRunnable, (long) timeoutMinutes * 60 * 1000);
    }

    private void cleanupViews() {
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
        super.onDestroy();
    }
}
