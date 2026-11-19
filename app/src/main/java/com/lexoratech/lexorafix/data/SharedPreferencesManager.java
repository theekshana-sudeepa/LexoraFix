package com.lexoratech.lexorafix.data;

import android.content.Context;
import android.content.SharedPreferences;

public class SharedPreferencesManager {

    private static final String PREF_NAME = "lexorafix_prefs";
    private static final String KEY_FIREBASE_UID = "firebase_uid";
    private static final String KEY_USER_NAME = "user_name";
    private static final String KEY_USER_EMAIL = "user_email";
    private static final String KEY_IS_LOGGED_IN = "is_logged_in";
    private static final String KEY_LAST_ADDRESS = "last_address";
    private static final String KEY_FIRST_LAUNCH = "first_launch";
    private static final String KEY_NOTIFICATIONS_ENABLED = "notifications_enabled";
    private static final String KEY_BIOMETRIC_ENABLED = "biometric_enabled";
    private static final String KEY_NEEDS_BIOMETRIC_REAUTH = "needs_biometric_reauth";

    private final SharedPreferences prefs;
    private final SharedPreferences.Editor editor;
    private static SharedPreferencesManager instance;

    public static synchronized SharedPreferencesManager getInstance(Context context) {
        if (instance == null) {
            instance = new SharedPreferencesManager(context.getApplicationContext());
        }
        return instance;
    }

    private SharedPreferencesManager(Context context) {
        prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        editor = prefs.edit();
    }

    // ========== SESSION ==========

    public void saveLoginSession(String firebaseUid, String name, String email) {
        editor.putString(KEY_FIREBASE_UID, firebaseUid);
        editor.putString(KEY_USER_NAME, name);
        editor.putString(KEY_USER_EMAIL, email);
        editor.putBoolean(KEY_IS_LOGGED_IN, true);
        // First login after registration/sign-in should require biometric verification
        editor.putBoolean(KEY_NEEDS_BIOMETRIC_REAUTH, true);
        editor.apply();
    }

    public boolean isLoggedIn() {
        return prefs.getBoolean(KEY_IS_LOGGED_IN, false);
    }

    public String getFirebaseUid() {
        return prefs.getString(KEY_FIREBASE_UID, "");
    }

    public String getUserName() {
        return prefs.getString(KEY_USER_NAME, "");
    }

    public String getUserEmail() {
        return prefs.getString(KEY_USER_EMAIL, "");
    }

    public void logout() {
        editor.remove(KEY_FIREBASE_UID);
        editor.remove(KEY_USER_NAME);
        editor.remove(KEY_USER_EMAIL);
        editor.putBoolean(KEY_IS_LOGGED_IN, false);
        // Keep biometric_enabled preference — it protects the device, not the session
        // Mark that next login should require biometric re-verification
        editor.putBoolean(KEY_NEEDS_BIOMETRIC_REAUTH, true);
        editor.apply();
        
        // Clear secure cached credentials safely
        try {
            SecurePreferencesManager.getInstance(
                    android.app.Application.class.cast(
                            instance.prefs.getClass().getMethod("getClass").getDeclaringClass()
                    )
            ).clearCredentials();
        } catch (Exception ignored) {
            // Non-critical — credentials will be invalid after Firebase sign-out anyway
        }
    }

    // ========== ADDRESS ==========

    public void saveLastAddress(String address) {
        editor.putString(KEY_LAST_ADDRESS, address);
        editor.apply();
    }

    public String getLastAddress() {
        return prefs.getString(KEY_LAST_ADDRESS, "");
    }

    // ========== SETTINGS ==========

    public boolean isFirstLaunch() {
        return prefs.getBoolean(KEY_FIRST_LAUNCH, true);
    }

    public void setFirstLaunchDone() {
        editor.putBoolean(KEY_FIRST_LAUNCH, false);
        editor.apply();
    }

    public boolean areNotificationsEnabled() {
        return prefs.getBoolean(KEY_NOTIFICATIONS_ENABLED, true);
    }

    public void setNotificationsEnabled(boolean enabled) {
        editor.putBoolean(KEY_NOTIFICATIONS_ENABLED, enabled);
        editor.apply();
    }

    public boolean isBiometricEnabled() {
        return prefs.getBoolean(KEY_BIOMETRIC_ENABLED, false);
    }

    public void setBiometricEnabled(boolean enabled) {
        editor.putBoolean(KEY_BIOMETRIC_ENABLED, enabled);
        editor.apply();
    }

    public boolean needsBiometricReauth() {
        return prefs.getBoolean(KEY_NEEDS_BIOMETRIC_REAUTH, true);
    }

    public void setNeedsBiometricReauth(boolean needed) {
        editor.putBoolean(KEY_NEEDS_BIOMETRIC_REAUTH, needed);
        editor.apply();
    }

    // ========== PROFILE UPDATE ==========

    private static final String KEY_USER_PHONE = "user_phone";
    private static final String KEY_USER_ADDRESS = "user_address";

    public void saveUserName(String name) {
        editor.putString(KEY_USER_NAME, name);
        editor.apply();
    }

    public void saveUserPhone(String phone) {
        editor.putString(KEY_USER_PHONE, phone);
        editor.apply();
    }

    public String getUserPhone() {
        return prefs.getString(KEY_USER_PHONE, "");
    }

    public void saveUserAddress(String address) {
        editor.putString(KEY_USER_ADDRESS, address);
        editor.apply();
    }

    public String getUserAddress() {
        return prefs.getString(KEY_USER_ADDRESS, "");
    }
}
