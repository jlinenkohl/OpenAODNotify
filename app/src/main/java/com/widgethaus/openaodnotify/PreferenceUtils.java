package com.widgethaus.openaodnotify;

import android.content.Context;
import android.content.SharedPreferences;
import android.provider.Settings;
import android.text.TextUtils;

public class PreferenceUtils {
    public static final String PREFS_NAME = "AOD_PREFS";
    public static final String KEY_CURRENT_PROFILE = "current_profile";
    public static final String PROFILE_DEFAULT = "default";

    // Bitmasks for the LINES shape
    public static final int SIDE_TOP = 1;
    public static final int SIDE_BOTTOM = 2;
    public static final int SIDE_LEFT = 4;
    public static final int SIDE_RIGHT = 8;

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