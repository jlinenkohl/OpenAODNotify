package com.widgethaus.openaodnotify;

import android.graphics.drawable.Drawable;

public class AppInfo {
    public String packageName;
    public String label;
    public Drawable icon;
    public boolean isSelected;
    public String color; // Hex string

    public AppInfo(String packageName, String label, Drawable icon) {
        this.packageName = packageName;
        this.label = label;
        this.icon = icon;
        this.isSelected = false;
        this.color = "";
    }
}
