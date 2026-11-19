package com.lexoratech.lexorafix.admin.utils;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.appcompat.app.AppCompatDelegate;

public class ThemeUtils {

    public static final String PREF_NAME = "admin_prefs";
    public static final String KEY_THEME_MODE = "theme_mode";

    public static final int THEME_SYSTEM = 0;
    public static final int THEME_LIGHT = 1;
    public static final int THEME_DARK = 2;

    public static void applyTheme(Context context) {
        int themeMode = getThemeMode(context);
        setNightMode(themeMode);
    }

    public static void setThemeMode(Context context, int mode) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        prefs.edit().putInt(KEY_THEME_MODE, mode).apply();
        setNightMode(mode);
    }

    public static int getThemeMode(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        // Default to System (0)
        return prefs.getInt(KEY_THEME_MODE, THEME_SYSTEM);
    }

    private static void setNightMode(int mode) {
        switch (mode) {
            case THEME_LIGHT:
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                break;
            case THEME_DARK:
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                break;
            case THEME_SYSTEM:
            default:
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
                break;
        }
    }
}
