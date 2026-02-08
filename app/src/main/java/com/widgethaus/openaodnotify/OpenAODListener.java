package com.widgethaus.openaodnotify;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;

public class OpenAODListener extends NotificationListenerService {
    public static boolean hasNotification = false;
    private BroadcastReceiver screenReceiver;

    @Override
    public void onCreate() {
        super.onCreate();
        screenReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (Intent.ACTION_SCREEN_OFF.equals(intent.getAction()) && hasNotification) {
                    context.startForegroundService(new Intent(context, OpenAODOverlayService.class));
                } else if (Intent.ACTION_SCREEN_ON.equals(intent.getAction())) {
                    context.stopService(new Intent(context, OpenAODOverlayService.class));
                    // Optional: decide if you want to reset here or keep it until cleared
                }
            }
        };
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_SCREEN_OFF);
        filter.addAction(Intent.ACTION_SCREEN_ON);
        registerReceiver(screenReceiver, filter, Context.RECEIVER_EXPORTED);
    }

    @Override
    public void onDestroy() {
        if (screenReceiver != null) {
            unregisterReceiver(screenReceiver);
        }
        super.onDestroy();
    }

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        if (!sbn.isOngoing()) {
            hasNotification = true;
        }
    }

    @Override
    public void onNotificationRemoved(StatusBarNotification sbn) {
        // Simple logic: if no active non-ongoing notifications, reset
        StatusBarNotification[] active = getActiveNotifications();
        boolean stillHasNotif = false;
        if (active != null) {
            for (StatusBarNotification n : active) {
                if (!n.isOngoing()) {
                    stillHasNotif = true;
                    break;
                }
            }
        }
        hasNotification = stillHasNotif;
        if (!hasNotification) {
            stopService(new Intent(this, OpenAODOverlayService.class));
        }
    }
}
