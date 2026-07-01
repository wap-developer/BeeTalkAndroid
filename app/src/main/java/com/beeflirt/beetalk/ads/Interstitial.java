package com.beeflirt.beetalk.ads;

import static com.beeflirt.beetalk.adsconfig.SettingAds.INTERVAL_CAPS;
import static com.beeflirt.beetalk.adsconfig.SettingAds.MAIN_ADS_INTERTITIAL;
import static com.beeflirt.beetalk.adsconfig.SettingAds.MAIN_ADS_INTERTITIAL_BACKUP;
import static com.beeflirt.beetalk.adsconfig.SettingAds.SELECT_MAIN_ADS;

import android.app.Activity;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;

import com.applovin.mediation.MaxAd;
import com.applovin.mediation.MaxAdListener;
import com.applovin.mediation.MaxError;
import com.applovin.mediation.ads.MaxInterstitialAd;
import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.interstitial.InterstitialAd;
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback;

public class Interstitial {

    public static MaxInterstitialAd interstitialAd;
    public static InterstitialAd mInterstitialAd;

    public static final long SHOW_DELAY_MS = 500;
    public static long lastShownTime = 0;
    public static boolean isLoading = false;
    public static boolean isInterstitialReady = false;

    public static final String TAG = "Interstitial";

    /** Callback dipanggil setelah iklan dismiss atau jika iklan tidak siap */
    public interface DismissCallback {
        void onDismissed();
    }

    // ===================================================================
    // LOAD — entry point utama
    // ===================================================================

    public static void loadAds(Activity activity) {
        if (isLoading) {
            Log.d(TAG, "Ad sudah loading, skip...");
            return;
        }
        isLoading = true;
        isInterstitialReady = false;

        switch (SELECT_MAIN_ADS) {
            case "APPLOVIN":
                // AppLovin saja, tidak ada backup
                LoadApplovinMax(activity, MAIN_ADS_INTERTITIAL, null);
                break;
            case "ADMOB":
                // AdMob saja, tidak ada backup
                LoadAdmob(activity, MAIN_ADS_INTERTITIAL, null);
                break;
            case "APPLOVIN_ADMOB":
                // AppLovin primary → AdMob backup jika gagal
                LoadApplovinMax(activity, MAIN_ADS_INTERTITIAL, MAIN_ADS_INTERTITIAL_BACKUP);
                break;
            case "ADMOB_APPLOVIN":
                // AdMob primary → AppLovin backup jika gagal
                LoadAdmob(activity, MAIN_ADS_INTERTITIAL, MAIN_ADS_INTERTITIAL_BACKUP);
                break;
            default:
                Log.w(TAG, "loadAds: SELECT_MAIN_ADS tidak dikenal → " + SELECT_MAIN_ADS);
                isLoading = false;
        }
    }

    // ===================================================================
    // LOAD APPLOVIN
    // @param idAdmobBackup — null/kosong jika tidak ada backup
    // ===================================================================

    public static void LoadApplovinMax(Activity activity, String idApplovin, String idAdmobBackup) {
        Log.d(TAG, "Loading AppLovin interstitial" + (hasBackup(idAdmobBackup) ? " (AdMob backup tersedia)" : "") + "...");

        interstitialAd = new MaxInterstitialAd(idApplovin, activity);
        interstitialAd.setListener(new MaxAdListener() {

            @Override
            public void onAdLoaded(@NonNull MaxAd maxAd) {
                Log.d(TAG, "AppLovin interstitial loaded ✓");
                isLoading = false;
                isInterstitialReady = true;
            }

            @Override
            public void onAdDisplayed(@NonNull MaxAd maxAd) {
                Log.d(TAG, "AppLovin interstitial displayed");
                isInterstitialReady = false;
            }

            @Override
            public void onAdHidden(@NonNull MaxAd maxAd) {
                Log.d(TAG, "AppLovin interstitial dismissed → reloading...");
                if (!activity.isFinishing() && !activity.isDestroyed()) {
                    loadAds(activity);
                }
            }

            @Override
            public void onAdClicked(@NonNull MaxAd maxAd) {}

            @Override
            public void onAdLoadFailed(@NonNull String s, @NonNull MaxError maxError) {
                Log.e(TAG, "AppLovin interstitial gagal load: " + maxError.getMessage());
                isLoading = false;
                isInterstitialReady = false;

                if (hasBackup(idAdmobBackup) && !activity.isFinishing() && !activity.isDestroyed()) {
                    // Fallback ke AdMob backup (satu kali, tanpa rekursif antar jaringan)
                    Log.w(TAG, "Fallback ke AdMob interstitial backup...");
                    LoadAdmob(activity, idAdmobBackup, null);
                } else {
                    // Retry AppLovin setelah 10 detik
                    new Handler(Looper.getMainLooper()).postDelayed(() -> {
                        if (!activity.isFinishing() && !activity.isDestroyed()) {
                            Log.d(TAG, "Retrying AppLovin interstitial...");
                            loadAds(activity);
                        }
                    }, 10000);
                }
            }

            @Override
            public void onAdDisplayFailed(@NonNull MaxAd maxAd, @NonNull MaxError maxError) {
                Log.e(TAG, "AppLovin interstitial gagal display: " + maxError.getMessage());
                isLoading = false;
                isInterstitialReady = false;
                if (!activity.isFinishing() && !activity.isDestroyed()) {
                    loadAds(activity);
                }
            }
        });

        interstitialAd.loadAd();
    }

    // ===================================================================
    // LOAD ADMOB
    // @param idApplovinBackup — null/kosong jika tidak ada backup
    // ===================================================================

    public static void LoadAdmob(Activity activity, String idAdmob, String idApplovinBackup) {
        Log.d(TAG, "Loading AdMob interstitial" + (hasBackup(idApplovinBackup) ? " (AppLovin backup tersedia)" : "") + "...");

        AdRequest adRequest = new AdRequest.Builder().build();
        InterstitialAd.load(activity, idAdmob, adRequest, new InterstitialAdLoadCallback() {

            @Override
            public void onAdLoaded(@NonNull InterstitialAd ad) {
                Log.d(TAG, "AdMob interstitial loaded ✓");
                mInterstitialAd = ad;
                isLoading = false;
                isInterstitialReady = true;

                mInterstitialAd.setFullScreenContentCallback(new FullScreenContentCallback() {
                    @Override
                    public void onAdShowedFullScreenContent() {
                        Log.d(TAG, "AdMob interstitial showed");
                        isInterstitialReady = false;
                    }

                    @Override
                    public void onAdDismissedFullScreenContent() {
                        Log.d(TAG, "AdMob interstitial dismissed → reloading...");
                        mInterstitialAd = null;
                        isInterstitialReady = false;
                        if (!activity.isFinishing() && !activity.isDestroyed()) {
                            loadAds(activity);
                        }
                    }

                    @Override
                    public void onAdFailedToShowFullScreenContent(@NonNull AdError error) {
                        Log.e(TAG, "AdMob interstitial gagal show: " + error.getMessage());
                        mInterstitialAd = null;
                        isLoading = false;
                        isInterstitialReady = false;
                        if (!activity.isFinishing() && !activity.isDestroyed()) {
                            loadAds(activity);
                        }
                    }
                });
            }

            @Override
            public void onAdFailedToLoad(@NonNull LoadAdError error) {
                Log.e(TAG, "AdMob interstitial gagal load: " + error.getMessage());
                mInterstitialAd = null;
                isLoading = false;
                isInterstitialReady = false;

                if (hasBackup(idApplovinBackup) && !activity.isFinishing() && !activity.isDestroyed()) {
                    // Fallback ke AppLovin backup (satu kali, tanpa rekursif antar jaringan)
                    Log.w(TAG, "Fallback ke AppLovin interstitial backup...");
                    LoadApplovinMax(activity, idApplovinBackup, null);
                } else {
                    // Retry AdMob setelah 10 detik
                    new Handler(Looper.getMainLooper()).postDelayed(() -> {
                        if (!activity.isFinishing() && !activity.isDestroyed()) {
                            Log.d(TAG, "Retrying AdMob interstitial...");
                            loadAds(activity);
                        }
                    }, 10000);
                }
            }
        });
    }

    // ===================================================================
    // SHOW
    // ===================================================================

    public static boolean showInters(Activity activity) {
        return showInters(activity, null);
    }

    /**
     * Tampilkan interstitial. Jika iklan berhasil tayang, callback dipanggil setelah dismiss.
     * Jika iklan tidak siap / frequency cap aktif, callback langsung dipanggil.
     */
    public static boolean showInters(Activity activity, DismissCallback callback) {
        // Gunakan INTERVAL dari server (dalam menit), konversi ke millisecond
        long intervalMs = (long) INTERVAL_CAPS * 60 * 1000;
        long currentTime = System.currentTimeMillis();

        if (currentTime - lastShownTime < intervalMs) {
            Log.d(TAG, "Frequency cap aktif. Sisa: "
                    + ((intervalMs - (currentTime - lastShownTime)) / 1000) + "s");
            if (callback != null) callback.onDismissed();
            return false;
        }

        if (!isInterstitialReady) {
            Log.d(TAG, "Interstitial belum siap");
            if (callback != null) callback.onDismissed();
            return false;
        }

        if (activity.isFinishing() || activity.isDestroyed()) {
            if (callback != null) callback.onDismissed();
            return false;
        }

        switch (SELECT_MAIN_ADS) {
            case "APPLOVIN":
            case "APPLOVIN_ADMOB":
                if (interstitialAd != null && interstitialAd.isReady()) {
                    Log.d(TAG, "Showing AppLovin interstitial (with callback)");
                    // Override listener sementara untuk tangkap dismiss
                    interstitialAd.setListener(new com.applovin.mediation.MaxAdListener() {
                        @Override public void onAdLoaded(@NonNull MaxAd ad) {}
                        @Override public void onAdDisplayed(@NonNull MaxAd ad) { isInterstitialReady = false; }
                        @Override public void onAdClicked(@NonNull MaxAd ad) {}
                        @Override public void onAdLoadFailed(@NonNull String s, @NonNull MaxError e) {}
                        @Override public void onAdDisplayFailed(@NonNull MaxAd ad, @NonNull MaxError e) {
                            isInterstitialReady = false;
                            if (callback != null) callback.onDismissed();
                            if (!activity.isFinishing() && !activity.isDestroyed()) loadAds(activity);
                        }
                        @Override public void onAdHidden(@NonNull MaxAd ad) {
                            if (callback != null) callback.onDismissed();
                            if (!activity.isFinishing() && !activity.isDestroyed()) loadAds(activity);
                        }
                    });
                    interstitialAd.showAd(activity);
                    lastShownTime = System.currentTimeMillis();
                } else if (SELECT_MAIN_ADS.equals("APPLOVIN_ADMOB") && mInterstitialAd != null) {
                    Log.d(TAG, "AppLovin tidak ready, showing fallback AdMob interstitial");
                    showAdMobInterstitial(activity, callback);
                } else {
                    Log.d(TAG, "AppLovin interstitial belum ready saat show");
                    if (callback != null) callback.onDismissed();
                }
                break;

            case "ADMOB":
            case "ADMOB_APPLOVIN":
                if (mInterstitialAd != null) {
                    Log.d(TAG, "Showing AdMob interstitial (with callback)");
                    showAdMobInterstitial(activity, callback);
                } else if (SELECT_MAIN_ADS.equals("ADMOB_APPLOVIN") && interstitialAd != null && interstitialAd.isReady()) {
                    Log.d(TAG, "AdMob tidak ready, showing fallback AppLovin interstitial");
                    // Tampilkan AppLovin
                    interstitialAd.setListener(new com.applovin.mediation.MaxAdListener() {
                        @Override public void onAdLoaded(@NonNull MaxAd ad) {}
                        @Override public void onAdDisplayed(@NonNull MaxAd ad) { isInterstitialReady = false; }
                        @Override public void onAdClicked(@NonNull MaxAd ad) {}
                        @Override public void onAdLoadFailed(@NonNull String s, @NonNull MaxError e) {}
                        @Override public void onAdDisplayFailed(@NonNull MaxAd ad, @NonNull MaxError e) {
                            isInterstitialReady = false;
                            if (callback != null) callback.onDismissed();
                            if (!activity.isFinishing() && !activity.isDestroyed()) loadAds(activity);
                        }
                        @Override public void onAdHidden(@NonNull MaxAd ad) {
                            if (callback != null) callback.onDismissed();
                            if (!activity.isFinishing() && !activity.isDestroyed()) loadAds(activity);
                        }
                    });
                    interstitialAd.showAd(activity);
                    lastShownTime = System.currentTimeMillis();
                } else {
                    Log.d(TAG, "AdMob interstitial null saat show");
                    if (callback != null) callback.onDismissed();
                }
                break;

            default:
                Log.d(TAG, "showInters: mode tidak dikenal → " + SELECT_MAIN_ADS);
                if (callback != null) callback.onDismissed();
        }

        return true;
    }

    // ===================================================================
    // HELPER
    // ===================================================================

    private static void showAdMobInterstitial(Activity activity, DismissCallback callback) {
        if (mInterstitialAd == null) {
            if (callback != null) callback.onDismissed();
            return;
        }
        mInterstitialAd.setFullScreenContentCallback(new com.google.android.gms.ads.FullScreenContentCallback() {
            @Override
            public void onAdDismissedFullScreenContent() {
                mInterstitialAd = null;
                isInterstitialReady = false;
                if (callback != null) callback.onDismissed();
                if (!activity.isFinishing() && !activity.isDestroyed()) loadAds(activity);
            }
            @Override
            public void onAdFailedToShowFullScreenContent(@NonNull com.google.android.gms.ads.AdError error) {
                mInterstitialAd = null;
                isInterstitialReady = false;
                if (callback != null) callback.onDismissed();
                if (!activity.isFinishing() && !activity.isDestroyed()) loadAds(activity);
            }
            @Override
            public void onAdShowedFullScreenContent() { isInterstitialReady = false; }
        });
        mInterstitialAd.show(activity);
        lastShownTime = System.currentTimeMillis();
    }

    private static boolean hasBackup(String backupId) {
        return backupId != null && !backupId.trim().isEmpty();
    }
}
