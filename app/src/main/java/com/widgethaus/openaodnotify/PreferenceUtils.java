package com.widgethaus.openaodnotify;

import android.content.Context;
import android.content.SharedPreferences;
import android.provider.Settings;
import android.text.TextUtils;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Set;

public class PreferenceUtils {
    public static final String PREFS_NAME = "AOD_PREFS";
    public static final String KEY_CURRENT_PROFILE = "current_profile";
    public static final String PROFILE_DEFAULT = "default";
    public static final String KEY_UI_THEME = "ui_theme";
    public static final String UI_THEME_SYSTEM = "system";
    public static final String UI_THEME_CLASSIC = "classic";

    // Bitmasks for the LINES shape
    public static final int SIDE_TOP = 1;
    public static final int SIDE_BOTTOM = 2;
    public static final int SIDE_LEFT = 4;
    public static final int SIDE_RIGHT = 8;

    // Discovery keys
    public static final String KEY_DISCOVERED_RULES = "discovered_rules";

    // Debug: Breathing animation frame-rate target (not profile-scoped; a device/debug tuning knob)
    private static final String KEY_BREATHING_FPS = "debug_breathing_fps";
    public static final int DEFAULT_BREATHING_FPS = 30;
    public static final int MIN_BREATHING_FPS = 1;
    public static final int MAX_BREATHING_FPS = 120;

    public enum ShapeType {
        CIRCLE(0, "Circle", true),
        SQUARE(1, "Square", true),
        RECTANGLE(2, "Rectangle", true),
        RING(3, "Ring", true),
        LINES(4, "Lines", false);

        private final int id;
        private final String label;
        private final boolean isDraggable;

        ShapeType(int id, String label, boolean isDraggable) {
            this.id = id;
            this.label = label;
            this.isDraggable = isDraggable;
        }

        public int getId() { return id; }
        public String getLabel() { return label; }
        public boolean isDraggable() { return isDraggable; }

        public static ShapeType fromId(int id) {
            for (ShapeType type : values()) {
                if (type.id == id) return type;
            }
            return CIRCLE;
        }
    }

    public static SharedPreferences getPrefs(Context context) {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    private static String getPrefix(Context context) {
        return getPrefs(context).getString(KEY_CURRENT_PROFILE, PROFILE_DEFAULT) + "_";
    }

    // New Shape-Aware Getters with Sane Defaults
    public static int getShapeSize(Context context, ShapeType shape) {
        String key = getPrefix(context) + shape.name() + "_size";
        int def = 35;
        if (shape == ShapeType.RING) def = 80;
        if (shape == ShapeType.LINES) def = 5;
        return getPrefs(context).getInt(key, def);
    }

    public static int getShapeX(Context context, ShapeType shape) {
        return getPrefs(context).getInt(getPrefix(context) + shape.name() + "_x", 600);
    }

    public static int getShapeY(Context context, ShapeType shape) {
        return getPrefs(context).getInt(getPrefix(context) + shape.name() + "_y", 325);
    }

    public static boolean isShapeRounded(Context context, ShapeType shape) {
        return getPrefs(context).getBoolean(getPrefix(context) + shape.name() + "_rounded", true);
    }

    public static int getLineSides(Context context) {
        // Defaults to Full Border (all 4 sides)
        return getPrefs(context).getInt(getPrefix(context) + "LINES_sides", 15);
    }

    // Global Settings
    public static String getColor(Context context) {
        return getPrefs(context).getString(getPrefix(context) + "color", "0066ff");
    }

    public static int getDuration(Context context) {
        return getPrefs(context).getInt(getPrefix(context) + "duration", 2000);
    }

    public static float getMinAlpha(Context context) {
        return getPrefs(context).getFloat(getPrefix(context) + "min_alpha", 0.01f);
    }

    public static float getMaxAlpha(Context context) {
        return getPrefs(context).getFloat(getPrefix(context) + "max_alpha", 0.99f);
    }

    public static int getTimeout(Context context) {
        return getPrefs(context).getInt(getPrefix(context) + "timeout", 5);
    }

    public static ShapeType getCurrentShape(Context context) {
        return ShapeType.fromId(getPrefs(context).getInt(getPrefix(context) + "current_shape", 0));
    }

    // Power Status Settings
    public static boolean isPowerStatusEnabled(Context context) {
        return getPrefs(context).getBoolean(getPrefix(context) + "power_status_enabled", false);
    }

    public static String getPowerStatusColorPlugged(Context context) {
        return getPrefs(context).getString(getPrefix(context) + "power_status_color_plugged", "00FF00"); // Green
    }

    public static String getPowerStatusColorLow(Context context) {
        return getPrefs(context).getString(getPrefix(context) + "power_status_color_low", "FF0000"); // Red
    }

    public static String getPowerStatusColorCharging(Context context) {
        return getPrefs(context).getString(getPrefix(context) + "power_status_color_charging", "FFFF00"); // Yellow
    }

    public static ShapeType getPowerStatusShape(Context context) {
        return ShapeType.fromId(getPrefs(context).getInt(getPrefix(context) + "power_status_shape", 0));
    }

    public static int getPowerShapeSize(Context context, ShapeType shape) {
        String key = getPrefix(context) + "power_" + shape.name() + "_size";
        int def = 35;
        if (shape == ShapeType.RING) def = 80;
        if (shape == ShapeType.LINES) def = 5;
        return getPrefs(context).getInt(key, def);
    }

    public static int getPowerShapeX(Context context, ShapeType shape) {
        return getPrefs(context).getInt(getPrefix(context) + "power_" + shape.name() + "_x", 600);
    }

    public static int getPowerShapeY(Context context, ShapeType shape) {
        return getPrefs(context).getInt(getPrefix(context) + "power_" + shape.name() + "_y", 400);
    }

    public static boolean isPowerShapeRounded(Context context, ShapeType shape) {
        return getPrefs(context).getBoolean(getPrefix(context) + "power_" + shape.name() + "_rounded", true);
    }

    public static int getPowerLineSides(Context context) {
        return getPrefs(context).getInt(getPrefix(context) + "power_LINES_sides", 15);
    }

    public static void savePowerStatusSettings(Context context, boolean enabled, int shapeId, String plugged, String low, String charging) {
        SharedPreferences.Editor editor = getPrefs(context).edit();
        String p = getPrefix(context);
        editor.putBoolean(p + "power_status_enabled", enabled);
        editor.putInt(p + "power_status_shape", shapeId);
        editor.putString(p + "power_status_color_plugged", plugged);
        editor.putString(p + "power_status_color_low", low);
        editor.putString(p + "power_status_color_charging", charging);
        editor.apply();
    }

    public static void savePowerShapeSettings(Context context, ShapeType shape, int size, int x, int y, boolean rounded, int sides) {
        SharedPreferences.Editor editor = getPrefs(context).edit();
        String p = getPrefix(context) + "power_" + shape.name() + "_";
        editor.putInt(p + "size", size);
        editor.putInt(p + "x", x);
        editor.putInt(p + "y", y);
        editor.putBoolean(p + "rounded", rounded);
        if (shape == ShapeType.LINES) {
            editor.putInt(getPrefix(context) + "power_LINES_sides", sides);
        }
        editor.apply();
    }

    // --- Intelligent Filtering Settings ---
    public static int getMaxNotifAgeMinutes(Context context) {
        return getPrefs(context).getInt(getPrefix(context) + "max_notif_age", 60);
    }

    public static boolean shouldIgnoreNonClearable(Context context) {
        return getPrefs(context).getBoolean(getPrefix(context) + "ignore_non_clearable", true);
    }

    public static void saveGlobalSettings(Context context, String color, int duration, float min, float max, int timeout, int shapeId) {
        SharedPreferences.Editor editor = getPrefs(context).edit();
        String p = getPrefix(context);
        editor.putString(p + "color", color);
        editor.putInt(p + "duration", duration);
        editor.putFloat(p + "min_alpha", min);
        editor.putFloat(p + "max_alpha", max);
        editor.putInt(p + "timeout", timeout);
        editor.putInt(p + "current_shape", shapeId);
        editor.apply();
    }

    public static void saveShapeSettings(Context context, ShapeType shape, int size, int x, int y, boolean rounded, int sides) {
        SharedPreferences.Editor editor = getPrefs(context).edit();
        String p = getPrefix(context) + shape.name() + "_";
        editor.putInt(p + "size", size);
        editor.putInt(p + "x", x);
        editor.putInt(p + "y", y);
        editor.putBoolean(p + "rounded", rounded);
        if (shape == ShapeType.LINES) {
            editor.putInt(getPrefix(context) + "LINES_sides", sides);
        }
        editor.apply();
    }

    // --- Debug: Breathing FPS target ---
    public static int getBreathingFps(Context context) {
        return getPrefs(context).getInt(KEY_BREATHING_FPS, DEFAULT_BREATHING_FPS);
    }

    public static void setBreathingFps(Context context, int fps) {
        int clamped = Math.max(MIN_BREATHING_FPS, Math.min(MAX_BREATHING_FPS, fps));
        getPrefs(context).edit().putInt(KEY_BREATHING_FPS, clamped).apply();
    }

    // --- Overlay-visible time telemetry (battery diagnostics) ---
    // Fixed-size ring buffer of exactly TELEMETRY_SLOT_COUNT (7) slots, keyed by
    // (epochDay % 7), NOT by date directly. Each slot stores which epoch-day it currently
    // represents plus accumulated millis for that day. This guarantees exactly
    // TELEMETRY_SLOT_COUNT * 2 fixed SharedPreferences keys FOREVER — storage can never
    // grow beyond that, no pruning/cleanup pass is ever required, and old data is
    // automatically discarded (overwritten) the moment its slot is reused ~7 days later.
    // Persisted (SharedPreferences) so it survives "Reset & Initialize", force-stop, and
    // device reboot. Kept independent of KEY_CURRENT_PROFILE (getPrefix) since this is
    // device-level telemetry, not a per-profile display setting.
    private static final int TELEMETRY_SLOT_COUNT = 7;
    private static final String TELEMETRY_SLOT_DAY_PREFIX = "telemetry_slot_day_";
    private static final String TELEMETRY_SLOT_MS_PREFIX = "telemetry_slot_ms_";

    private static int slotFor(LocalDate date) {
        return (int) Math.floorMod(date.toEpochDay(), (long) TELEMETRY_SLOT_COUNT);
    }

    public static long getOverlayMillisForDate(Context context, LocalDate date) {
        SharedPreferences prefs = getPrefs(context);
        int slot = slotFor(date);
        long storedEpochDay = prefs.getLong(TELEMETRY_SLOT_DAY_PREFIX + slot, Long.MIN_VALUE);
        // A slot only holds valid data for the specific date that last wrote to it;
        // if it currently represents a different day (stale, from ~7+ days ago), treat as 0.
        if (storedEpochDay != date.toEpochDay()) return 0L;
        return prefs.getLong(TELEMETRY_SLOT_MS_PREFIX + slot, 0L);
    }

    public static void addOverlayMillisForDate(Context context, LocalDate date, long millis) {
        if (millis <= 0) return;
        long current = getOverlayMillisForDate(context, date); // already 0 if slot is stale
        int slot = slotFor(date);
        getPrefs(context).edit()
                .putLong(TELEMETRY_SLOT_DAY_PREFIX + slot, date.toEpochDay())
                .putLong(TELEMETRY_SLOT_MS_PREFIX + slot, current + millis)
                .apply();
    }

    /**
     * Returns overlay-visible time (ms) for today plus the previous {@code days - 1} days
     * (capped at {@link #TELEMETRY_SLOT_COUNT}, since that's all that's ever retained),
     * oldest first. Missing/stale days are included with a value of 0.
     */
    public static LinkedHashMap<LocalDate, Long> getRecentOverlayTelemetry(Context context, int days) {
        int clampedDays = Math.min(days, TELEMETRY_SLOT_COUNT);
        LinkedHashMap<LocalDate, Long> result = new LinkedHashMap<>();
        LocalDate today = LocalDate.now(ZoneId.systemDefault());
        for (int i = clampedDays - 1; i >= 0; i--) {
            LocalDate date = today.minusDays(i);
            result.put(date, getOverlayMillisForDate(context, date));
        }
        return result;
    }

    // --- Granular Discovery Methods ---
    /**
     * Records a discovered notification pattern.
     * Format: packageName|channelId|category
     */
    public static void addDiscoveredRule(Context context, String pkg, String channelId, String category) {
        String ruleKey = String.format("%s|%s|%s", 
                pkg, 
                (channelId != null ? channelId : "default"), 
                (category != null ? category : "none"));
        
        SharedPreferences prefs = getPrefs(context);
        Set<String> rules = new HashSet<>(prefs.getStringSet(KEY_DISCOVERED_RULES, new HashSet<>()));
        if (rules.add(ruleKey)) {
            prefs.edit().putStringSet(KEY_DISCOVERED_RULES, rules).apply();
        }
    }

    public static Set<String> getDiscoveredRules(Context context) {
        return getPrefs(context).getStringSet(KEY_DISCOVERED_RULES, new HashSet<>());
    }

    public static void clearDiscoveredRules(Context context) {
        getPrefs(context).edit().remove(KEY_DISCOVERED_RULES).apply();
    }

    // Generic accessor methods
    public static int getInt(Context context, String key, int def) {
        return getPrefs(context).getInt(getPrefix(context) + key, def);
    }

    public static String getString(Context context, String key, String def) {
        return getPrefs(context).getString(getPrefix(context) + key, def);
    }

    public static float getFloat(Context context, String key, float def) {
        return getPrefs(context).getFloat(getPrefix(context) + key, def);
    }

    public static boolean getBoolean(Context context, String key, boolean def) {
        return getPrefs(context).getBoolean(getPrefix(context) + key, def);
    }

    public static boolean isValidColor(String hex) {
        if (hex == null) return false;
        if (hex.startsWith("#")) hex = hex.substring(1);
        return hex.matches("^[0-9a-fA-F]{6}$");
    }

    public static boolean isAccessibilityServiceEnabled(Context context) {
        String service = context.getPackageName() + "/" + OpenAODOverlayService.class.getCanonicalName();
        try {
            int enabled = Settings.Secure.getInt(context.getContentResolver(), Settings.Secure.ACCESSIBILITY_ENABLED);
            if (enabled == 1) {
                String settingValue = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
                if (settingValue != null) {
                    TextUtils.SimpleStringSplitter splitter = new TextUtils.SimpleStringSplitter(':');
                    splitter.setString(settingValue);
                    while (splitter.hasNext()) {
                        if (splitter.next().equalsIgnoreCase(service)) return true;
                    }
                }
            }
        } catch (Exception ignored) {}
        return false;
    }

    public static boolean isNotificationAccessGranted(Context context) {
        String pkgName = context.getPackageName();
        String flat = Settings.Secure.getString(context.getContentResolver(), "enabled_notification_listeners");
        return flat != null && flat.contains(pkgName);
    }
}