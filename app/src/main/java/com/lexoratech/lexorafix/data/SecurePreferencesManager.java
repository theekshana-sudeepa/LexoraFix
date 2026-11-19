package com.lexoratech.lexorafix.data;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKey;

import java.io.IOException;
import java.security.GeneralSecurityException;

public class SecurePreferencesManager {

    private static final String PREF_NAME = "secure_lexorafix_prefs";
    private static final String KEY_SECURE_EMAIL = "secure_email";
    private static final String KEY_SECURE_PASSWORD = "secure_password";

    private SharedPreferences securePrefs;
    private static SecurePreferencesManager instance;

    public static synchronized SecurePreferencesManager getInstance(Context context) {
        if (instance == null) {
            instance = new SecurePreferencesManager(context.getApplicationContext());
        }
        return instance;
    }

    private SecurePreferencesManager(Context context) {
        try {
            MasterKey masterKey = new MasterKey.Builder(context)
                    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                    .build();

            securePrefs = EncryptedSharedPreferences.create(
                    context,
                    PREF_NAME,
                    masterKey,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            );
        } catch (GeneralSecurityException | IOException e) {
            e.printStackTrace();
            // Fallback (Not recommended for prod without further handling, but prevents crash)
            securePrefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        }
    }

    public void saveCredentials(String email, String password) {
        securePrefs.edit()
                .putString(KEY_SECURE_EMAIL, email)
                .putString(KEY_SECURE_PASSWORD, password)
                .apply();
    }

    public String getSavedEmail() {
        return securePrefs.getString(KEY_SECURE_EMAIL, null);
    }

    public String getSavedPassword() {
        return securePrefs.getString(KEY_SECURE_PASSWORD, null);
    }

    public void clearCredentials() {
        securePrefs.edit()
                .remove(KEY_SECURE_EMAIL)
                .remove(KEY_SECURE_PASSWORD)
                .apply();
    }
}
