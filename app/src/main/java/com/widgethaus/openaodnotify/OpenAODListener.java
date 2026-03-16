package com.widgethaus.openaodnotify;

import android.app.Notification;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.util.Log;

public class OpenAODListener extends NotificationListenerService {
    private static final String TAG = "OpenAODListener";
    private static boolean isConnected = false;
    public static boolean hasNotification = false;
    private BroadcastReceiver screenReceiver;
    private long lastNotificationTime = 0;

    public static boolean isServiceConnected() {
        return isConnected;
    }

    @Override
    public void onListenerConnected() {
        super.onListenerConnected();
        Log.d(TAG, "✅ Listener Connected");
        isConnected = true;
        updateNotificationState();
    }

    @Override
    public void onListenerDisconnected() {
        super.onListenerDisconnected();
        Log.d(TAG, "❌ Listener Disconnected");
        isConnected = false;
    }

    private void updateNotificationState() {
        if (!isConnected) return;
        try {
            StatusBarNotification[] active = getActiveNotifications();
            boolean stillHasNotif = false;
            if (active != null) {
                for (StatusBarNotification n : active) {
                    if (isValidNotification(n)) {
                        stillHasNotif = true;
                        break;
                    }
                }
            }
            hasNotification = stillHasNotif;
            Log.d(TAG, "State check: hasNotification=" + hasNotification);
        } catch (Exception e) {
            Log.e(TAG, "Error checking notifications", e);
        }
    }

    private boolean isValidNotification(StatusBarNotification sbn) {
        if (sbn == null) return false;
        String pkg = sbn.getPackageName();
        
        // Log deep metadata for rule discovery
        Notification notification = sbn.getNotification();
        Bundle extras = notification.extras;
        String title = String.valueOf(extras.get(Notification.EXTRA_TITLE));
        String category = notification.category;
        int priority = notification.priority;
        String channelId = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O ? notification.getChannelId() : "N/A";

        // 1. Check if it's clearable (if setting is enabled)
        if (PreferenceUtils.shouldIgnoreNonClearable(this) && !sbn.isClearable()) {
            return false;
        }

        // 2. Check notification age (TTL)
        long postTime = sbn.getPostTime();
        long now = System.currentTimeMillis();
        long ageMs = now - postTime;
        long maxAgeMs = (long) PreferenceUtils.getMaxNotifAgeMinutes(this) * 60 * 1000;

        if (ageMs > maxAgeMs) {
            Log.d(TAG, "Ignoring old notification: " + pkg + " (Age: " + (ageMs / 60000) + " mins)");
            return false;
        }

        Log.d(TAG, String.format("🔍 Inspecting: Pkg=%s, Title=%s, Cat=%s, Chan=%s, Priority=%d, Ongoing=%b, Clearable=%b", 
                pkg, title, category, channelId, priority, sbn.isOngoing(), sbn.isClearable()));

        // Filter out system packages that have persistent/ambient notifications
        return !sbn.isOngoing() && 
               !pkg.equals("android") && 
               !pkg.equals("com.android.systemui") && 
               !pkg.equals("com.android.settings") &&
               !pkg.equals("com.google.android.gms");
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "Listener Created");
        screenReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                if (Intent.ACTION_SCREEN_OFF.equals(action)) {
                    updateNotificationState();
                    if (hasNotification) startOverlayService();
                } else if (Intent.ACTION_SCREEN_ON.equals(action)) {
                    long timeSinceNotif = System.currentTimeMillis() - lastNotificationTime;
                    if (timeSinceNotif < 5000) return;
                    stopOverlayService();
                } else if (Intent.ACTION_USER_PRESENT.equals(action)) {
                    stopOverlayService();
                }
            }
        };
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_SCREEN_OFF);
        filter.addAction(Intent.ACTION_SCREEN_ON);
        filter.addAction(Intent.ACTION_USER_PRESENT);
        registerReceiver(screenReceiver, filter, Context.RECEIVER_EXPORTED);
    }

    private void startOverlayService() {
        Log.d(TAG, "Starting Overlay Service");
        Intent serviceIntent = new Intent(this, OpenAODOverlayService.class);
        serviceIntent.setAction(OpenAODOverlayService.ACTION_START);
        startForegroundService(serviceIntent);
    }

    private void stopOverlayService() {
        Log.d(TAG, "Stopping Overlay Service (via Action)");
        Intent serviceIntent = new Intent(this, OpenAODOverlayService.class);
        serviceIntent.setAction(OpenAODOverlayService.ACTION_STOP);
        startService(serviceIntent);
    }

    @Override
    public void onDestroy() {
        if (screenReceiver != null) unregisterReceiver(screenReceiver);
        super.onDestroy();
    }

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        // Discovery: Track unique packages that post notifications
        PreferenceUtils.addDiscoveredPackage(this, sbn.getPackageName());

        if (!isValidNotification(sbn)) return;
        Log.d(TAG, "✅ Valid Notification Posted: " + sbn.getPackageName());
        
        hasNotification = true;
        lastNotificationTime = System.currentTimeMillis();
        
        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        if (pm != null && !pm.isInteractive()) {
            startOverlayService();
        }
    }

    @Override
    public void onNotificationRemoved(StatusBarNotification sbn) {
        updateNotificationState();
        if (!hasNotification) {
            stopOverlayService();
        }
    }
}