package com.beeflirt.beetalk.ads;

import static com.beeflirt.beetalk.adsconfig.SettingAds.INTERVAL_CAPS;

import android.app.Activity;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.beeflirt.beetalk.adsconfig.SettingAds;

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

    public static void loadAds(Activity activity, String placement) {
        if (isLoading) {
            Log.d(TAG, "Ad sudah loading, skip...");
            return;
        }
        isLoading = true;
        isInterstitialReady = false;

        boolean applovinOn = SettingAds.isPlacementEnabled(placement, SettingAds.NETWORK_APPLOVIN);
        boolean admobOn = SettingAds.isPlacementEnabled(placement, SettingAds.NETWORK_ADMOB);

        if (applovinOn && admobOn) {
            if ("admob".equals(SettingAds.WATERFALL_PRIORITY)) {
                Log.d(TAG, "loadAds[" + placement + "]: AdMob primary → AppLovin backup");
                LoadAdmob(activity, SettingAds.ADMOB_INTERSTITIAL, SettingAds.APPLOVIN_INTERSTITIAL);
            } else {
                Log.d(TAG, "loadAds[" + placement + "]: AppLovin primary → AdMob backup");
                LoadApplovinMax(activity, SettingAds.APPLOVIN_INTERSTITIAL, SettingAds.ADMOB_INTERSTITIAL);
            }
        } else if (admobOn) {
            Log.d(TAG, "loadAds[" + placement + "]: AdMob only");
            LoadAdmob(activity, SettingAds.ADMOB_INTERSTITIAL, null);
        } else if (applovinOn) {
            Log.d(TAG, "loadAds[" + placement + "]: AppLovin only");
            LoadApplovinMax(activity, SettingAds.APPLOVIN_INTERSTITIAL, null);
        } else {
            isLoading = false;
        }
    }

    public static void loadAds(Activity activity) {
        loadAds(activity, SettingAds.PLACEMENT_INTERSTITIAL_MAIN_SELECT);
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

        if ("admob".equals(SettingAds.WATERFALL_PRIORITY)) {
            if (mInterstitialAd != null) {
                Log.d(TAG, "Showing AdMob interstitial (priority: admob)");
                showAdMobInterstitial(activity, callback);
                return true;
            } else if (interstitialAd != null && interstitialAd.isReady()) {
                Log.d(TAG, "AdMob not ready, fallback showing AppLovin interstitial");
                showAppLovinInterstitial(activity, callback);
                return true;
            }
        } else {
            if (interstitialAd != null && interstitialAd.isReady()) {
                Log.d(TAG, "Showing AppLovin interstitial (priority: applovin)");
                showAppLovinInterstitial(activity, callback);
                return true;
            } else if (mInterstitialAd != null) {
                Log.d(TAG, "AppLovin not ready, fallback showing AdMob interstitial");
                showAdMobInterstitial(activity, callback);
                return true;
            }
        }

        Log.d(TAG, "No interstitial ready to show");
        if (callback != null) callback.onDismissed();
        return false;
    }

    private static void showAppLovinInterstitial(Activity activity, DismissCallback callback) {
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
