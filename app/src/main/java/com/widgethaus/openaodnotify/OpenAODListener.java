package com.widgethaus.openaodnotify;

import android.app.Notification;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.util.Log;

import java.util.HashSet;
import java.util.Set;

public class OpenAODListener extends NotificationListenerService {
    private static final String TAG = "OpenAODListener";
    public static final String ACTION_REFRESH = "com.widgethaus.openaodnotify.REFRESH_LISTENER";
    
    private static boolean isConnected = false;
    public static boolean hasNotification = false;
    private boolean isOverlayRunning = false;
    private String lastColorSent = null;
    
    private BroadcastReceiver screenReceiver;
    private long lastNotificationTime = 0;

    // Cached Preferences for Battery Efficiency
    private boolean prefGlobalDefault = true;
    private boolean prefIgnoreNonClearable = true;
    private int prefMaxAgeMinutes = 60;
    private Set<String> prefEnabledCats = new HashSet<>();
    private Set<String> prefEnabledApps = new HashSet<>();
    private boolean isPowerStatusEnabled = false;
    private String currentPowerColor = null;
    private boolean isPowerStatusActive = false;

    public static boolean isServiceConnected() {
        return isConnected;
    }

    @Override
    public void onListenerConnected() {
        super.onListenerConnected();
        Log.d(TAG, "✅ Listener Connected");
        isConnected = true;
        loadPreferences();
        updateNotificationState();
    }

    @Override
    public void onListenerDisconnected() {
        super.onListenerDisconnected();
        Log.d(TAG, "❌ Listener Disconnected");
        isConnected = false;
    }

    private void loadPreferences() {
        SharedPreferences prefs = PreferenceUtils.getPrefs(this);
        prefGlobalDefault = prefs.getBoolean("filter_global_default", true);
        prefIgnoreNonClearable = PreferenceUtils.shouldIgnoreNonClearable(this);
        prefMaxAgeMinutes = PreferenceUtils.getMaxNotifAgeMinutes(this);
        prefEnabledCats = prefs.getStringSet("filter_enabled_categories", new HashSet<>());
        prefEnabledApps = prefs.getStringSet("filter_enabled_apps", new HashSet<>());
        isPowerStatusEnabled = PreferenceUtils.isPowerStatusEnabled(this);
        Log.d(TAG, "Preferences loaded/refreshed");
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
            
            boolean changed = (hasNotification != stillHasNotif);
            hasNotification = stillHasNotif;
            
            Log.d(TAG, "State check: hasNotification=" + hasNotification);
            
            updatePowerStatus(null);
            handleOverlayUpdate();
        } catch (Exception e) {
            Log.e(TAG, "Error checking notifications", e);
        }
    }

    private void updatePowerStatus(Intent intent) {
        if (!isPowerStatusEnabled) {
            isPowerStatusActive = false;
            currentPowerColor = null;
            return;
        }

        if (intent == null) {
            intent = registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        }
        if (intent == null) return;

        int status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
        int level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
        int scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
        int plugged = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1);
        
        float batteryPct = level * 100 / (float)scale;
        
        boolean isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING;
        boolean isPlugged = plugged > 0;
        boolean isLow = batteryPct <= 15; // Standard 15% threshold

        if (isCharging) {
            isPowerStatusActive = true;
            currentPowerColor = PreferenceUtils.getPowerStatusColorCharging(this);
        } else if (isPlugged) {
            isPowerStatusActive = true;
            currentPowerColor = PreferenceUtils.getPowerStatusColorPlugged(this);
        } else if (isLow) {
            isPowerStatusActive = true;
            currentPowerColor = PreferenceUtils.getPowerStatusColorLow(this);
        } else {
            isPowerStatusActive = false;
            currentPowerColor = null;
        }
    }

    private void handleOverlayUpdate() {
        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        boolean isScreenOff = pm != null && !pm.isInteractive();

        boolean shouldShow = (hasNotification || isPowerStatusActive) && isScreenOff;

        if (shouldShow) {
            String powerColor = isPowerStatusActive ? currentPowerColor : null;
            Integer powerShape = isPowerStatusActive ? PreferenceUtils.getPowerStatusShape(this).getId() : null;
            startOverlayService(false, hasNotification, powerColor, powerShape);
        } else {
            stopOverlayService();
        }
    }

    private boolean isValidNotification(StatusBarNotification sbn) {
        if (sbn == null) return false;
        String pkg = sbn.getPackageName();
        
        // 1. Static Filters (Fastest)
        if (sbn.isOngoing() || 
            pkg.equals("android") || 
            pkg.equals("com.android.systemui") || 
            pkg.equals("com.android.settings") ||
            pkg.equals("com.google.android.gms")) {
            return false;
        }

        // 2. Clearable Filter
        if (prefIgnoreNonClearable && !sbn.isClearable()) {
            return false;
        }

        // 3. Age Filter
        long ageMs = System.currentTimeMillis() - sbn.getPostTime();
        if (ageMs > (long) prefMaxAgeMinutes * 60 * 1000) {
            return false;
        }

        // 4. Custom Filtering Logic
        if (prefGlobalDefault) {
            return true;
        }

        Notification notification = sbn.getNotification();
        if (notification.category != null && prefEnabledCats.contains(notification.category)) {
            return true;
        }

        if (prefEnabledApps.contains(pkg)) {
            return true;
        }

        return false;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "Listener Created");
        
        // Initialize prefs in case onListenerConnected is delayed
        loadPreferences();

        screenReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                if (Intent.ACTION_SCREEN_OFF.equals(action)) {
                    updateNotificationState();
                } else if (Intent.ACTION_SCREEN_ON.equals(action)) {
                    // Debounce screen on to prevent flicker during quick unlock
                    long timeSinceNotif = System.currentTimeMillis() - lastNotificationTime;
                    if (timeSinceNotif < 2000) return;
                    stopOverlayService();
                } else if (Intent.ACTION_USER_PRESENT.equals(action)) {
                    stopOverlayService();
                } else if (ACTION_REFRESH.equals(action)) {
                    Log.d(TAG, "🔄 Refreshing listener state & prefs");
                    loadPreferences();
                    updatePowerStatus(null);
                    updateNotificationState();
                } else if (Intent.ACTION_BATTERY_CHANGED.equals(action)) {
                    updatePowerStatus(intent);
                    handleOverlayUpdate();
                }
            }
        };
        
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_SCREEN_OFF);
        filter.addAction(Intent.ACTION_SCREEN_ON);
        filter.addAction(Intent.ACTION_USER_PRESENT);
        filter.addAction(Intent.ACTION_BATTERY_CHANGED);
        filter.addAction(ACTION_REFRESH);
        
        int flags = 0;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            flags = Context.RECEIVER_EXPORTED;
        }
        registerReceiver(screenReceiver, filter, flags);
    }

    private void startOverlayService(boolean force, boolean showNotif, String powerColor, Integer powerShape) {
        // Build a unique state string to check if we actually need to restart the service
        String currentState = "notif:" + showNotif + "|pColor:" + powerColor + "|pShape:" + powerShape;
        if (isOverlayRunning && !force && currentState.equals(lastColorSent)) return;

        Log.d(TAG, "Starting Overlay Service: " + currentState);
        Intent serviceIntent = new Intent(this, OpenAODOverlayService.class);
        serviceIntent.setAction(OpenAODOverlayService.ACTION_START);
        if (force) serviceIntent.putExtra("force", true);
        
        if (showNotif) {
            serviceIntent.putExtra("show_notification", true);
        }
        if (powerColor != null) {
            serviceIntent.putExtra("show_power", true);
            serviceIntent.putExtra("power_color", powerColor);
            if (powerShape != null) {
                serviceIntent.putExtra("power_shape", (int)powerShape);
            }
        }
        
        lastColorSent = currentState;
        startService(serviceIntent);
        isOverlayRunning = true;
    }

    private void stopOverlayService() {
        if (!isOverlayRunning) return;

        Log.d(TAG, "Stopping Overlay Service");
        Intent serviceIntent = new Intent(this, OpenAODOverlayService.class);
        serviceIntent.setAction(OpenAODOverlayService.ACTION_STOP);
        startService(serviceIntent);
        isOverlayRunning = false;
    }

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        Notification notification = sbn.getNotification();
        String channelId = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O ? notification.getChannelId() : "default";
        PreferenceUtils.addDiscoveredRule(this, sbn.getPackageName(), channelId, notification.category);

        if (!isValidNotification(sbn)) return;
        
        hasNotification = true;
        lastNotificationTime = System.currentTimeMillis();
        PreferenceUtils.incrementNotificationCountForDate(this, java.time.LocalDate.now(java.time.ZoneId.systemDefault()));
        
        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        if (pm != null && !pm.isInteractive()) {
            // New notification arrived while screen is off, force overlay refresh
            updatePowerStatus(null);
            handleOverlayUpdate();
        }
    }

    @Override
    public void onNotificationRemoved(StatusBarNotification sbn) {
        // We only care about re-checking if the notification removed was a valid one
        // but it's safer and still fairly cheap to just refresh state
        updateNotificationState();
    }

    @Override
    public void onDestroy() {
        if (screenReceiver != null) unregisterReceiver(screenReceiver);
        super.onDestroy();
    }
}
