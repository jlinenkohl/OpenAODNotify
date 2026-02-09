package com.widgethaus.openaodnotify;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
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
        Log.d(TAG, "Checking notification validity from " + pkg);
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
        startService(serviceIntent); // Use startService to send the action to the persistent accessibility service
    }

    @Override
    public void onDestroy() {
        if (screenReceiver != null) unregisterReceiver(screenReceiver);
        super.onDestroy();
    }

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        if (!isValidNotification(sbn)) return;
        hasNotification = true;
        lastNotificationTime = System.currentTimeMillis();
        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        if (pm != null && !pm.isInteractive()) startOverlayService();
    }

    @Override
    public void onNotificationRemoved(StatusBarNotification sbn) {
        updateNotificationState();
        if (!hasNotification) stopOverlayService();
    }
}