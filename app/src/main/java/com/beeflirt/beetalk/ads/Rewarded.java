package com.beeflirt.beetalk.ads;

import android.app.Activity;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.beeflirt.beetalk.adsconfig.SettingAds;

import androidx.annotation.NonNull;

import com.applovin.mediation.MaxAd;
import com.applovin.mediation.MaxError;
import com.applovin.mediation.MaxReward;
import com.applovin.mediation.MaxRewardedAdListener;
import com.applovin.mediation.ads.MaxRewardedAd;
import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.rewarded.RewardedAd;
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback;

public class Rewarded {

    private static final String TAG = "Rewarded";

    public static RewardedAd admobRewarded;
    public static MaxRewardedAd applovinRewarded;

    public static boolean isRewardedReady = false;
    public static boolean isLoading = false;

    // Temporary holder untuk callback AppLovin (reward diberikan via listener)
    private static RewardCallback pendingCallback;

    /** Callback hasil reward ke pemanggil */
    public interface RewardCallback {
        void onUserEarnedReward(int amount);
        void onAdFailedToShow(String error);
    }

    // ===================================================================
    // LOAD — entry point utama
    // ===================================================================

    public static void loadAds(Activity activity, String placement) {
        if (isLoading) {
            Log.d(TAG, "Ad sudah dalam proses loading, skip...");
            return;
        }
        isLoading = true;
        isRewardedReady = false;

        boolean applovinOn = SettingAds.isPlacementEnabled(placement, SettingAds.NETWORK_APPLOVIN);
        boolean admobOn = SettingAds.isPlacementEnabled(placement, SettingAds.NETWORK_ADMOB);

        if (applovinOn && admobOn) {
            if ("admob".equals(SettingAds.WATERFALL_PRIORITY)) {
                Log.d(TAG, "loadAds[" + placement + "]: AdMob primary → AppLovin backup");
                LoadAdmobRewarded(activity, SettingAds.ADMOB_REWARDS, SettingAds.APPLOVIN_REWARDS);
            } else {
                Log.d(TAG, "loadAds[" + placement + "]: AppLovin primary → AdMob backup");
                LoadApplovinRewarded(activity, SettingAds.APPLOVIN_REWARDS, SettingAds.ADMOB_REWARDS);
            }
        } else if (admobOn) {
            Log.d(TAG, "loadAds[" + placement + "]: AdMob only");
            LoadAdmobRewarded(activity, SettingAds.ADMOB_REWARDS, null);
        } else if (applovinOn) {
            Log.d(TAG, "loadAds[" + placement + "]: AppLovin only");
            LoadApplovinRewarded(activity, SettingAds.APPLOVIN_REWARDS, null);
        } else {
            isLoading = false;
        }
    }

    public static void loadAds(Activity activity) {
        loadAds(activity, SettingAds.PLACEMENT_REWARDED_SUPER_LIKE);
    }

    // ===================================================================
    // LOAD APPLOVIN
    // @param idAdmobBackup — null/kosong jika tidak ada backup
    // ===================================================================

    public static void LoadApplovinRewarded(Activity activity, String adUnitId, String idAdmobBackup) {
        Log.d(TAG, "Loading AppLovin rewarded" + (hasBackup(idAdmobBackup) ? " (AdMob backup tersedia)" : "") + "...");

        applovinRewarded = MaxRewardedAd.getInstance(adUnitId, activity);
        applovinRewarded.setListener(new MaxRewardedAdListener() {

            @Override
            public void onAdLoaded(@NonNull MaxAd maxAd) {
                Log.d(TAG, "AppLovin rewarded loaded ✓");
                isLoading = false;
                isRewardedReady = true;
            }

            @Override
            public void onAdLoadFailed(@NonNull String adUnitId, @NonNull MaxError error) {
                Log.e(TAG, "AppLovin rewarded gagal load: " + error.getMessage());
                isLoading = false;
                isRewardedReady = false;

                if (hasBackup(idAdmobBackup) && !activity.isFinishing() && !activity.isDestroyed()) {
                    Log.w(TAG, "Fallback ke AdMob rewarded backup...");
                    LoadAdmobRewarded(activity, idAdmobBackup, null);
                } else {
                    // Retry setelah 10 detik
                    new Handler(Looper.getMainLooper()).postDelayed(() -> {
                        if (!activity.isFinishing() && !activity.isDestroyed()) {
                            Log.d(TAG, "Retrying AppLovin rewarded...");
                            loadAds(activity);
                        }
                    }, 10000);
                }
            }

            @Override
            public void onAdDisplayed(@NonNull MaxAd maxAd) {
                Log.d(TAG, "AppLovin rewarded displayed");
                isRewardedReady = false;
            }

            @Override
            public void onAdDisplayFailed(@NonNull MaxAd maxAd, @NonNull MaxError error) {
                Log.e(TAG, "AppLovin rewarded gagal display: " + error.getMessage());
                isLoading = false;
                isRewardedReady = false;
                if (pendingCallback != null) pendingCallback.onAdFailedToShow(error.getMessage());
                pendingCallback = null;
                if (!activity.isFinishing() && !activity.isDestroyed()) {
                    loadAds(activity);
                }
            }

            @Override
            public void onAdHidden(@NonNull MaxAd maxAd) {
                Log.d(TAG, "AppLovin rewarded dismissed → reloading...");
                isRewardedReady = false;
                pendingCallback = null;
                if (!activity.isFinishing() && !activity.isDestroyed()) {
                    loadAds(activity);
                }
            }

            @Override
            public void onAdClicked(@NonNull MaxAd maxAd) {}

            @Override
            public void onUserRewarded(@NonNull MaxAd maxAd, @NonNull MaxReward reward) {
                Log.d(TAG, "AppLovin user rewarded: " + reward.getAmount() + " " + reward.getLabel());
                if (pendingCallback != null) {
                    pendingCallback.onUserEarnedReward(reward.getAmount());
                }
            }
        });

        applovinRewarded.loadAd();
    }

    // ===================================================================
    // LOAD ADMOB
    // @param idApplovinBackup — null/kosong jika tidak ada backup
    // ===================================================================

    public static void LoadAdmobRewarded(Activity activity, String adUnitId, String idApplovinBackup) {
        Log.d(TAG, "Loading AdMob rewarded" + (hasBackup(idApplovinBackup) ? " (AppLovin backup tersedia)" : "") + "...");

        AdRequest adRequest = new AdRequest.Builder().build();
        RewardedAd.load(activity, adUnitId, adRequest, new RewardedAdLoadCallback() {

            @Override
            public void onAdLoaded(@NonNull RewardedAd ad) {
                Log.d(TAG, "AdMob rewarded loaded ✓");
                admobRewarded = ad;
                isLoading = false;
                isRewardedReady = true;

                admobRewarded.setFullScreenContentCallback(new FullScreenContentCallback() {
                    @Override
                    public void onAdShowedFullScreenContent() {
                        Log.d(TAG, "AdMob rewarded showed");
                        isRewardedReady = false;
                    }

                    @Override
                    public void onAdDismissedFullScreenContent() {
                        Log.d(TAG, "AdMob rewarded dismissed → reloading...");
                        admobRewarded = null;
                        isRewardedReady = false;
                        if (!activity.isFinishing() && !activity.isDestroyed()) {
                            loadAds(activity);
                        }
                    }

                    @Override
                    public void onAdFailedToShowFullScreenContent(@NonNull AdError error) {
                        Log.e(TAG, "AdMob rewarded gagal show: " + error.getMessage());
                        admobRewarded = null;
                        isLoading = false;
                        isRewardedReady = false;
                        if (!activity.isFinishing() && !activity.isDestroyed()) {
                            loadAds(activity);
                        }
                    }
                });
            }

            @Override
            public void onAdFailedToLoad(@NonNull LoadAdError error) {
                Log.e(TAG, "AdMob rewarded gagal load: " + error.getMessage());
                admobRewarded = null;
                isLoading = false;
                isRewardedReady = false;

                if (hasBackup(idApplovinBackup) && !activity.isFinishing() && !activity.isDestroyed()) {
                    Log.w(TAG, "Fallback ke AppLovin rewarded backup...");
                    LoadApplovinRewarded(activity, idApplovinBackup, null);
                } else {
                    // Retry setelah 10 detik
                    new Handler(Looper.getMainLooper()).postDelayed(() -> {
                        if (!activity.isFinishing() && !activity.isDestroyed()) {
                            Log.d(TAG, "Retrying AdMob rewarded...");
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

    public static void showRewarded(Activity activity, RewardCallback callback) {
        if (!isRewardedReady) {
            Log.d(TAG, "Rewarded belum siap");
            if (callback != null) callback.onAdFailedToShow("Ad belum siap, coba lagi nanti.");
            return;
        }

        if ("admob".equals(SettingAds.WATERFALL_PRIORITY)) {
            if (admobRewarded != null) {
                Log.d(TAG, "Showing AdMob rewarded (priority: admob)");
                admobRewarded.show(activity, rewardItem -> {
                    Log.d(TAG, "AdMob user rewarded: " + rewardItem.getAmount());
                    if (callback != null) callback.onUserEarnedReward(rewardItem.getAmount());
                });
                return;
            } else if (applovinRewarded != null && applovinRewarded.isReady()) {
                Log.d(TAG, "AdMob not ready, showing fallback AppLovin rewarded");
                pendingCallback = callback;
                applovinRewarded.showAd(activity);
                return;
            }
        } else {
            if (applovinRewarded != null && applovinRewarded.isReady()) {
                Log.d(TAG, "Showing AppLovin rewarded (priority: applovin)");
                pendingCallback = callback;
                applovinRewarded.showAd(activity);
                return;
            } else if (admobRewarded != null) {
                Log.d(TAG, "AppLovin not ready, showing fallback AdMob rewarded");
                admobRewarded.show(activity, rewardItem -> {
                    Log.d(TAG, "AdMob user rewarded: " + rewardItem.getAmount());
                    if (callback != null) callback.onUserEarnedReward(rewardItem.getAmount());
                });
                return;
            }
        }

        Log.d(TAG, "No rewarded ad ready to show");
        if (callback != null) callback.onAdFailedToShow("Ad belum siap");
    }

    // ===================================================================
    // UTILITY
    // ===================================================================

    public static boolean isReady() {
        return isRewardedReady;
    }

    public static void destroy() {
        if (applovinRewarded != null) {
            applovinRewarded.destroy();
            applovinRewarded = null;
        }
        admobRewarded = null;
        isRewardedReady = false;
        isLoading = false;
        pendingCallback = null;
    }

    private static boolean hasBackup(String backupId) {
        return backupId != null && !backupId.trim().isEmpty();
    }
}
