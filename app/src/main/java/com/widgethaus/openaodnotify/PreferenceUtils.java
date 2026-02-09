package com.widgethaus.openaodnotify;

import android.content.Context;
import android.content.SharedPreferences;

public class PreferenceUtils {
    public static final String PREFS_NAME = "AOD_PREFS";
    public static final String KEY_CURRENT_PROFILE = "current_profile";
    public static final String PROFILE_DEFAULT = "default";

    public enum ShapeType {
        CIRCLE(0, true),
        SQUARE(1, true),
        RECTANGLE(2, true),
        RING(3, true),
        TOP_LINE(4, false),
        FULL_BORDER(5, false),
        VERTICAL_EDGES(6, false),
        HORIZONTAL_EDGES(7, false);

        private final int id;
        private final boolean isDraggable;

        ShapeType(int id, boolean isDraggable) {
            this.id = id;
            this.isDraggable = isDraggable;
        }

        public int getId() { return id; }
        public boolean isDraggable() { return isDraggable; }

        public static ShapeType fromId(int id) {
            for (ShapeType type : values()) {
                if (type.id == id) return type;
            }
            return CIRCLE;
        }
    }

    public static boolean isValidColor(String hex) {
        if (hex == null) return false;
        if (hex.startsWith("#")) hex = hex.substring(1);
        return hex.matches("^[0-9a-fA-F]{6}$");
    }

    private static String p(Context context, String key) {
        String profile = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getString(KEY_CURRENT_PROFILE, PROFILE_DEFAULT);
        return profile + "_" + key;
    }

    public static SharedPreferences getPrefs(Context context) {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public static void saveSettings(Context context, int timeout, String color, int x, int y, 
                                    int size, int duration, float minAlpha, float maxAlpha, 
                                    int shapeId, boolean rounded) {
        SharedPreferences.Editor editor = getPrefs(context).edit();
        String prefix = getPrefs(context).getString(KEY_CURRENT_PROFILE, PROFILE_DEFAULT) + "_";
        
        editor.putInt(prefix + "timeout", timeout);
        editor.putString(prefix + "color", color.startsWith("#") ? color.substring(1) : color);
        editor.putInt(prefix + "x", x);
        editor.putInt(prefix + "y", y);
        editor.putInt(prefix + "size", size);
        editor.putInt(prefix + "duration", duration);
        editor.putFloat(prefix + "min_alpha", minAlpha);
        editor.putFloat(prefix + "max_alpha", maxAlpha);
        editor.putInt(prefix + "shape", shapeId);
        editor.putBoolean(prefix + "rounded", rounded);
        editor.apply();
    }
    
    // Helper to get prefixed keys
    public static int getInt(Context context, String key, int def) {
        return getPrefs(context).getInt(p(context, key), def);
    }
    public static String getString(Context context, String key, String def) {
        return getPrefs(context).getString(p(context, key), def);
    }
    public static float getFloat(Context context, String key, float def) {
        return getPrefs(context).getFloat(p(context, key), def);
    }
    public static boolean getBoolean(Context context, String key, boolean def) {
        return getPrefs(context).getBoolean(p(context, key), def);
    }
}