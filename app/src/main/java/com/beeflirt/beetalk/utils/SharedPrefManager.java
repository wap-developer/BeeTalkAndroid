package com.beeflirt.beetalk.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKey;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Map;

public class SharedPrefManager {
    private static final String TAG = "SharedPrefManager";
    private static final String ENCRYPTED_PREF_NAME = "BeeTalkLite_Secure_Prefs";

    private static SharedPrefManager instance;
    private SharedPreferences sharedPreferences;

    private SharedPrefManager(Context context) {
        migrateFromLegacyPrefs(context);
        migrateFromUnencryptedPrefs(context);
        sharedPreferences = createEncryptedPrefs(context);
    }

    public static synchronized SharedPrefManager getInstance(Context context) {
        if (instance == null) {
            instance = new SharedPrefManager(context.getApplicationContext());
        }
        return instance;
    }

    /**
     * Buat EncryptedSharedPreferences.
     * Jika gagal (device lama / hardware issue), fallback ke SharedPreferences biasa.
     */
    private SharedPreferences createEncryptedPrefs(Context context) {
        try {
            MasterKey masterKey = new MasterKey.Builder(context)
                    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                    .build();

            return EncryptedSharedPreferences.create(
                    context,
                    ENCRYPTED_PREF_NAME,
                    masterKey,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            );
        } catch (GeneralSecurityException | IOException e) {
            Log.e(TAG, "Failed to create EncryptedSharedPreferences, falling back to regular prefs", e);
            return context.getSharedPreferences(Constants.PREF_NAME, Context.MODE_PRIVATE);
        }
    }

    /**
     * Migrate dari legacy SharedPreferences (TinderKW_Prefs) ke unencrypted baru (BeeTalkLite_Prefs).
     * Step 1 dari 2-step migration.
     */
    private void migrateFromLegacyPrefs(Context context) {
        SharedPreferences oldPrefs = context.getSharedPreferences(Constants.LEGACY_PREF_NAME, Context.MODE_PRIVATE);
        Map<String, ?> allOldEntries = oldPrefs.getAll();

        if (allOldEntries.isEmpty()) {
            return;
        }

        Log.d(TAG, "Migrating " + allOldEntries.size() + " entries from legacy prefs (TinderKW)");

        // Pindahkan ke unencrypted prefs dulu (sebagai staging)
        SharedPreferences stagingPrefs = context.getSharedPreferences(Constants.PREF_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor stagingEditor = stagingPrefs.edit();

        for (Map.Entry<String, ?> entry : allOldEntries.entrySet()) {
            Object value = entry.getValue();
            String key = entry.getKey();

            if (value instanceof String) {
                stagingEditor.putString(key, (String) value);
            } else if (value instanceof Integer) {
                stagingEditor.putInt(key, (Integer) value);
            } else if (value instanceof Boolean) {
                stagingEditor.putBoolean(key, (Boolean) value);
            } else if (value instanceof Float) {
                stagingEditor.putFloat(key, (Float) value);
            } else if (value instanceof Long) {
                stagingEditor.putLong(key, (Long) value);
            }
        }
        stagingEditor.apply();

        // Hapus legacy prefs
        oldPrefs.edit().clear().apply();
        Log.d(TAG, "Legacy prefs migration complete");
    }

    /**
     * Migrate dari unencrypted SharedPreferences (BeeTalkLite_Prefs) ke EncryptedSharedPreferences.
     * Step 2 dari 2-step migration. Juga menangani user yang sudah pakai BeeTalkLite_Prefs sebelumnya.
     */
    private void migrateFromUnencryptedPrefs(Context context) {
        SharedPreferences unencryptedPrefs = context.getSharedPreferences(Constants.PREF_NAME, Context.MODE_PRIVATE);
        Map<String, ?> allEntries = unencryptedPrefs.getAll();

        if (allEntries.isEmpty()) {
            return;
        }

        // Cek apakah encrypted prefs sudah punya data
        SharedPreferences encryptedPrefs = createEncryptedPrefs(context);
        if (encryptedPrefs.contains(Constants.KEY_IS_LOGGED_IN)) {
            // Sudah di-migrate, hapus unencrypted
            unencryptedPrefs.edit().clear().apply();
            return;
        }

        Log.d(TAG, "Migrating " + allEntries.size() + " entries to encrypted prefs");

        SharedPreferences.Editor encEditor = encryptedPrefs.edit();
        for (Map.Entry<String, ?> entry : allEntries.entrySet()) {
            Object value = entry.getValue();
            String key = entry.getKey();

            if (value instanceof String) {
                encEditor.putString(key, (String) value);
            } else if (value instanceof Integer) {
                encEditor.putInt(key, (Integer) value);
            } else if (value instanceof Boolean) {
                encEditor.putBoolean(key, (Boolean) value);
            } else if (value instanceof Float) {
                encEditor.putFloat(key, (Float) value);
            } else if (value instanceof Long) {
                encEditor.putLong(key, (Long) value);
            }
        }
        encEditor.apply();

        // Hapus unencrypted prefs setelah berhasil migrate
        unencryptedPrefs.edit().clear().apply();
        Log.d(TAG, "Encrypted prefs migration complete");
    }

    // Save user session
    public void saveUserSession(String token, int userId, String email) {
        sharedPreferences.edit()
                .putString(Constants.KEY_TOKEN, token)
                .putInt(Constants.KEY_USER_ID, userId)
                .putString(Constants.KEY_EMAIL, email)
                .putBoolean(Constants.KEY_IS_LOGGED_IN, true)
                .apply();
    }

    // Get token
    public String getToken() {
        return sharedPreferences.getString(Constants.KEY_TOKEN, null);
    }

    // Save token only (for refresh)
    public void saveToken(String token) {
        sharedPreferences.edit()
                .putString(Constants.KEY_TOKEN, token)
                .apply();
    }

    // Get user ID
    public int getUserId() {
        return sharedPreferences.getInt(Constants.KEY_USER_ID, -1);
    }

    // Get email
    public String getEmail() {
        return sharedPreferences.getString(Constants.KEY_EMAIL, null);
    }

    // Check if logged in
    public boolean isLoggedIn() {
        return sharedPreferences.getBoolean(Constants.KEY_IS_LOGGED_IN, false);
    }

    // Clear session (logout)
    public void clearSession() {
        sharedPreferences.edit().clear().apply();
    }
}
