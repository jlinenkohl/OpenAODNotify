package com.widgethaus.openaodnotify;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

public class BootReceiver extends BroadcastReceiver {
    private static final String CHANNEL_ID = "PermissionAlertChannel";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            checkPermissionsAndNotify(context);
        }
    }

    private void checkPermissionsAndNotify(Context context) {
        boolean accessibilityEnabled = PreferenceUtils.isAccessibilityServiceEnabled(context);
        boolean notificationGranted = PreferenceUtils.isNotificationAccessGranted(context);

        if (!accessibilityEnabled || !notificationGranted) {
            showPermissionNotification(context, accessibilityEnabled, notificationGranted);
        }
    }

    private void showPermissionNotification(Context context, boolean acc, boolean notif) {
        createNotificationChannel(context);

        String message = "OpenAODNotify requires setup: ";
        if (!acc && !notif) message += "Accessibility & Notification access.";
        else if (!acc) message += "Accessibility access.";
        else message += "Notification access.";

        Intent intent = new Intent(context, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, 
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_alert)
                .setContentTitle("Permission Required")
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true);

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
        try {
            notificationManager.notify(1001, builder.build());
        } catch (SecurityException ignored) {
            // Might happen on API 33+ if POST_NOTIFICATIONS is not granted
        }
    }

    private void createNotificationChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Service Alerts",
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription("Notifications for missing permissions");
            NotificationManager manager = context.getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }
}
