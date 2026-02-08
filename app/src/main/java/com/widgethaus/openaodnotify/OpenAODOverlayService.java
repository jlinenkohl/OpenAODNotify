package com.widgethaus.openaodnotify;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;

import androidx.core.app.NotificationCompat;

import java.util.ArrayList;
import java.util.List;

public class OpenAODOverlayService extends Service {
    private WindowManager wm;
    private List<View> overlayViews = new ArrayList<>();
    private List<ObjectAnimator> animators = new ArrayList<>();
    private Handler handler = new Handler(Looper.getMainLooper());
    private Runnable timeoutRunnable;
    private static final String CHANNEL_ID = "AOD_Overlay_Channel";
    public static final String ACTION_POSITION_UPDATE = "com.widgethaus.openaodnotify.POSITION_UPDATE";

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("AOD Overlay Active")
                .setContentText("Monitoring notifications...")
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setPriority(NotificationCompat.PRIORITY_MIN)
                .build();
        
        startForeground(1, notification);
        wm = (WindowManager) getSystemService(WINDOW_SERVICE);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            showOverlay(intent);
        }
        return START_STICKY;
    }

    private void showOverlay(Intent intent) {
        cleanupViews();

        Bundle extras = intent.getExtras();
        boolean isPreview = extras != null && extras.getBoolean("preview", false);
        
        SharedPreferences prefs = getSharedPreferences("AOD_PREFS", MODE_PRIVATE);
        
        int shapeType = isPreview ? extras.getInt("shape") : prefs.getInt("shape", 0);
        int size = isPreview ? extras.getInt("size") : prefs.getInt("size", 60);
        String colorHex = isPreview ? extras.getString("color") : prefs.getString("color", "0066ff");
        int duration = isPreview ? extras.getInt("duration") : prefs.getInt("duration", 2500);
        float minAlpha = isPreview ? extras.getFloat("min_alpha") : prefs.getFloat("min_alpha", 0.1f);
        float maxAlpha = isPreview ? extras.getFloat("max_alpha") : prefs.getFloat("max_alpha", 1.0f);

        switch (shapeType) {
            case 0: // Circle
            case 1: // Square
            case 2: // Rectangle
                int x = isPreview ? extras.getInt("x") : prefs.getInt("x", 25);
                int y = isPreview ? extras.getInt("y") : prefs.getInt("y", 25);
                createDraggableShape(shapeType, size, x, y, isPreview);
                break;
            default:
                createComplexShape(shapeType, size);
                break;
        }

        applyAestheticsAndAnimate(colorHex, duration, minAlpha, maxAlpha);
        
        if (!isPreview) {
            startTimeout(prefs.getInt("timeout", 5));
        }
    }

    private void createDraggableShape(int shapeType, int size, int x, int y, boolean draggable) {
        int width = (shapeType == 2) ? size * 2 : size;
        int flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                | WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                | WindowManager.LayoutParams.FLAG_ALLOW_LOCK_WHILE_SCREEN_ON;
        
        if (!draggable) {
            flags |= WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE;
        }

        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                width, size,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                flags, PixelFormat.TRANSLUCENT
        );
        params.gravity = Gravity.TOP | Gravity.LEFT;
        params.x = x;
        params.y = y;

        View view = new View(this);
        GradientDrawable shape = new GradientDrawable();
        shape.setShape((shapeType == 0) ? GradientDrawable.OVAL : GradientDrawable.RECTANGLE);
        view.setBackground(shape);

        if (draggable) {
            view.setOnTouchListener(new View.OnTouchListener() {
                private int initialX, initialY;
                private float initialTouchX, initialTouchY;

                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    switch (event.getAction()) {
                        case MotionEvent.ACTION_DOWN:
                            initialX = params.x;
                            initialY = params.y;
                            initialTouchX = event.getRawX();
                            initialTouchY = event.getRawY();
                            return true;
                        case MotionEvent.ACTION_MOVE:
                            params.x = initialX + (int) (event.getRawX() - initialTouchX);
                            params.y = initialY + (int) (event.getRawY() - initialTouchY);
                            wm.updateViewLayout(view, params);
                            
                            // Send broadcast back to SettingsActivity
                            Intent updateIntent = new Intent(ACTION_POSITION_UPDATE);
                            updateIntent.putExtra("x", params.x);
                            updateIntent.putExtra("y", params.y);
                            sendBroadcast(updateIntent);
                            return true;
                    }
                    return false;
                }
            });
        }

        wm.addView(view, params);
        overlayViews.add(view);
    }

    private void createComplexShape(int shapeType, int thickness) {
        switch (shapeType) {
            case 3: // Top Line
                createLine(thickness, Gravity.TOP);
                break;
            case 4: // Full Border
                createLine(thickness, Gravity.TOP);
                createLine(thickness, Gravity.BOTTOM);
                createLine(thickness, Gravity.LEFT);
                createLine(thickness, Gravity.RIGHT);
                break;
            case 5: // Vertical Edges
                createLine(thickness, Gravity.LEFT);
                createLine(thickness, Gravity.RIGHT);
                break;
            case 6: // Horizontal Edges
                createLine(thickness, Gravity.TOP);
                createLine(thickness, Gravity.BOTTOM);
                break;
        }
    }

    private void createLine(int thickness, int gravity) {
        int width = (gravity == Gravity.TOP || gravity == Gravity.BOTTOM) 
            ? ViewGroup.LayoutParams.MATCH_PARENT : thickness;
        int height = (gravity == Gravity.LEFT || gravity == Gravity.RIGHT) 
            ? ViewGroup.LayoutParams.MATCH_PARENT : thickness;

        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                width, height,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                getFlags(), PixelFormat.TRANSLUCENT
        );
        params.gravity = gravity;

        View line = new View(this);
        wm.addView(line, params);
        overlayViews.add(line);
    }

    private void applyAestheticsAndAnimate(String colorHex, int duration, float minAlpha, float maxAlpha) {
        for (View view : overlayViews) {
            try {
                int color = Color.parseColor("#" + colorHex);
                if (view.getBackground() instanceof GradientDrawable) {
                    ((GradientDrawable) view.getBackground().mutate()).setColor(color);
                } else {
                    view.setBackgroundColor(color);
                }
            } catch (Exception e) {
                view.setBackgroundColor(Color.BLUE);
            }
            
            view.setAlpha(minAlpha);
            view.post(() -> {
                ObjectAnimator animator = ObjectAnimator.ofFloat(view, "alpha", minAlpha, maxAlpha);
                animator.setDuration(duration);
                animator.setRepeatMode(ValueAnimator.REVERSE);
                animator.setRepeatCount(ValueAnimator.INFINITE);
                animator.start();
                animators.add(animator);
            });
        }
    }

    private void startTimeout(int timeoutMinutes) {
        if (timeoutRunnable != null) handler.removeCallbacks(timeoutRunnable);
        timeoutRunnable = this::stopSelf;
        handler.postDelayed(timeoutRunnable, (long) timeoutMinutes * 60 * 1000);
    }

    private void cleanupViews() {
        for (ObjectAnimator animator : animators) animator.cancel();
        animators.clear();
        for (View view : overlayViews) {
            try { wm.removeView(view); } catch (Exception ignored) {}
        }
        overlayViews.clear();
    }

    private int getFlags() {
        return WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                | WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                | WindowManager.LayoutParams.FLAG_ALLOW_LOCK_WHILE_SCREEN_ON
                | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE;
    }

    private void createNotificationChannel() {
        NotificationChannel serviceChannel = new NotificationChannel(
                CHANNEL_ID,
                "AOD Overlay Service Channel",
                NotificationManager.IMPORTANCE_LOW
        );
        NotificationManager manager = getSystemService(NotificationManager.class);
        if (manager != null) {
            manager.createNotificationChannel(serviceChannel);
        }
    }

    @Override
    public void onDestroy() {
        cleanupViews();
        if (timeoutRunnable != null) {
            handler.removeCallbacks(timeoutRunnable);
        }
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
