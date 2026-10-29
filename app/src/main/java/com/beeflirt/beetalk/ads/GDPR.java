package com.beeflirt.beetalk.ads;

import android.app.Activity;
import android.provider.Settings;
import android.util.Log;

import androidx.annotation.Nullable;

import com.beeflirt.beetalk.BuildConfig;
import com.google.android.gms.ads.MobileAds;
import com.google.android.ump.ConsentDebugSettings;
import com.google.android.ump.ConsentForm;
import com.google.android.ump.ConsentInformation;
import com.google.android.ump.ConsentRequestParameters;
import com.google.android.ump.FormError;
import com.google.android.ump.UserMessagingPlatform;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.atomic.AtomicBoolean;

public class GDPR {

    private static final String TAG = "GDPR";

    public static ConsentInformation consentInformation;
    public static ConsentDebugSettings debugSettings;
    public static ConsentRequestParameters params;
    private static final AtomicBoolean isMobileAdsInitializeCalled = new AtomicBoolean(false);

    /**
     * Jalankan GDPR consent untuk jaringan yang memerlukan AdMob UMP.
     * Dipanggil untuk mode: ADMOB, ADMOB_APPLOVIN, APPLOVIN_ADMOB (karena AdMob terlibat).
     * Mode APPLOVIN murni tidak memerlukan UMP karena AppLovin punya consent-nya sendiri via MAX SDK.
     */
    public static void loadGdpr(Activity activity, String selectAds, boolean childDirected) {
        switch (selectAds) {
            case "ADMOB":
            case "ADMOB_APPLOVIN":  // AdMob primary → perlu UMP consent
            case "APPLOVIN_ADMOB":  // AdMob backup → tetap perlu UMP consent
                Log.d(TAG, "Memulai GDPR consent untuk mode: " + selectAds);
                requestAdMobConsent(activity, childDirected);
                break;

            case "APPLOVIN":
                // AppLovin MAX mengelola consent-nya sendiri via SDK init
                // Tidak perlu UMP/GDPR manual di sini
                Log.d(TAG, "Mode APPLOVIN: consent dikelola oleh AppLovin SDK");
                break;

            default:
                Log.w(TAG, "loadGdpr: mode tidak dikenal → " + selectAds);
        }
    }

    private static void requestAdMobConsent(Activity activity, boolean childDirected) {
        if (BuildConfig.DEBUG) {
            String androidId = Settings.Secure.getString(
                    activity.getContentResolver(), Settings.Secure.ANDROID_ID);
            String deviceId = (androidId != null) ? md5(androidId).toUpperCase() : "";

            debugSettings = new ConsentDebugSettings.Builder(activity)
                    .setDebugGeography(ConsentDebugSettings.DebugGeography.DEBUG_GEOGRAPHY_DISABLED)
                    .addTestDeviceHashedId(deviceId)
                    .build();

            params = new ConsentRequestParameters.Builder()
                    .setConsentDebugSettings(debugSettings)
                    .setTagForUnderAgeOfConsent(childDirected)
                    .build();
        } else {
            params = new ConsentRequestParameters.Builder()
                    .setTagForUnderAgeOfConsent(childDirected)
                    .build();
        }

        consentInformation = UserMessagingPlatform.getConsentInformation(activity);

        consentInformation.requestConsentInfoUpdate(
                activity,
                params,
                // Success: info consent berhasil diambil
                () -> {
                    UserMessagingPlatform.loadAndShowConsentFormIfRequired(
                            activity,
                            loadAndShowError -> {
                                if (loadAndShowError != null) {
                                    // Log error tapi tetap lanjutkan jika memungkinkan
                                    Log.w(TAG, "Consent form error: " + loadAndShowError.getMessage()
                                            + " (code: " + loadAndShowError.getErrorCode() + ")");
                                }
                                // Cek apakah bisa request iklan setelah consent
                                if (consentInformation.canRequestAds()) {
                                    Log.d(TAG, "Consent diterima → inisialisasi MobileAds");
                                    initializeMobileAdsSdk(activity);
                                } else {
                                    Log.d(TAG, "Tidak bisa request ads setelah consent form");
                                }
                            }
                    );
                },
                // Failure: gagal mendapatkan info consent
                requestConsentError -> {
                    Log.e(TAG, "Gagal mengambil consent info: " + requestConsentError.getMessage()
                            + " (code: " + requestConsentError.getErrorCode() + ")");
                    // Tetap cek apakah bisa request ads (misalnya di luar EEA)
                    if (consentInformation.canRequestAds()) {
                        Log.d(TAG, "canRequestAds=true meski consent gagal → inisialisasi MobileAds");
                        initializeMobileAdsSdk(activity);
                    }
                }
        );

        // Langsung cek juga jika consent sudah pernah diberikan sebelumnya
        if (consentInformation.canRequestAds()) {
            Log.d(TAG, "Consent sudah ada sebelumnya → inisialisasi MobileAds");
            initializeMobileAdsSdk(activity);
        }
    }

    private static void initializeMobileAdsSdk(Activity activity) {
        if (isMobileAdsInitializeCalled.getAndSet(true)) {
            Log.d(TAG, "MobileAds sudah diinisialisasi sebelumnya, skip");
            return;
        }
        Log.d(TAG, "Menginisialisasi MobileAds...");
        MobileAds.initialize(activity, initializationStatus ->
                Log.d(TAG, "MobileAds initialized: " + initializationStatus.getAdapterStatusMap())
        );
    }

    /**
     * Hash MD5 untuk device ID (digunakan saat debug mode).
     * Mengembalikan string kosong jika input null atau hashing gagal.
     */
    public static String md5(final String s) {
        if (s == null) return "";
        try {
            MessageDigest digest = MessageDigest.getInstance("MD5");
            digest.update(s.getBytes());
            byte[] messageDigest = digest.digest();

            StringBuilder hexString = new StringBuilder(); // StringBuilder lebih efisien dari StringBuffer
            for (byte b : messageDigest) {
                String h = Integer.toHexString(0xFF & b);
                while (h.length() < 2) h = "0" + h;
                hexString.append(h);
            }
            return hexString.toString();

        } catch (NoSuchAlgorithmException e) {
            Log.e(TAG, "MD5 tidak tersedia: " + e.getMessage());
            return "";
        }
    }
}
