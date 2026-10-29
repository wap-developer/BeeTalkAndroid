package com.beeflirt.beetalk.adsconfig;

import static com.beeflirt.beetalk.adsconfig.SettingAds.CHILD_DIRECT_GDPR;
import static com.beeflirt.beetalk.adsconfig.SettingAds.URL_DATA;

import android.app.Activity;
import android.util.Log;
import android.widget.RelativeLayout;

import com.beeflirt.beetalk.BuildConfig;
import com.beeflirt.beetalk.ads.GDPR;
import com.beeflirt.beetalk.ads.Interstitial;
import com.beeflirt.beetalk.ads.Rewarded;
import com.beeflirt.beetalk.ads.SmallBanner;
import com.beeflirt.beetalk.utils.PremiumManager;

public class Ads {
    public static String APPID;
    public static String DATA;

    private static final String TAG = "Ads";

    /** Premium users: skip banner + interstitial. Rewarded stays (opt-in earn). */
    private static boolean isAdFree(Activity activity) {
        if (activity == null) return false;
        return PremiumManager.getInstance(activity).isAdFree();
    }

    public static void CheckAds(Activity activity) {
        if (URL_DATA.startsWith("http")) {
            Ads.DATA = URL_DATA;
            Ads.APPID = BuildConfig.APPLICATION_ID;
        }
    }

    public static void LoadGDPR(Activity activity) {
        // GDPR flow tetap pakai logic existing — kalau ada AdMob enabled di placement manapun,
        // run UMP. Kalau cuma AppLovin, skip.
        boolean anyAdmob = false;
        for (SettingAds.PlacementConfig p : SettingAds.PLACEMENTS.values()) {
            if (p.admobEnabled) { anyAdmob = true; break; }
        }
        String mode = anyAdmob ? "ADMOB_APPLOVIN" : "APPLOVIN";
        GDPR.loadGdpr(activity, mode, CHILD_DIRECT_GDPR);
    }

    /**
     * Show banner pakai placement-aware logic.
     * Cek toggle per network dari PLACEMENTS map.
     */
    public static void ShowBanner(Activity activity, RelativeLayout layAds, String placement) {
        if (isAdFree(activity)) {
            Log.d(TAG, "ShowBanner[" + placement + "]: premium ad-free, skip");
            if (layAds != null) layAds.setVisibility(android.view.View.GONE);
            return;
        }

        boolean applovinOn = SettingAds.isPlacementEnabled(placement, SettingAds.NETWORK_APPLOVIN);
        boolean admobOn = SettingAds.isPlacementEnabled(placement, SettingAds.NETWORK_ADMOB);

        if (!applovinOn && !admobOn) {
            Log.d(TAG, "ShowBanner[" + placement + "]: both disabled, skip");
            if (layAds != null) layAds.setVisibility(android.view.View.GONE);
            return;
        }

        if (applovinOn && admobOn) {
            // Waterfall: try priority first, fallback the other
            if ("admob".equals(SettingAds.WATERFALL_PRIORITY)) {
                Log.d(TAG, "ShowBanner[" + placement + "]: AdMob → fallback AppLovin");
                SmallBanner.AdmobWithApplovinBackup(activity, layAds, SettingAds.ADMOB_BANNER, SettingAds.APPLOVIN_BANNER);
            } else {
                Log.d(TAG, "ShowBanner[" + placement + "]: AppLovin → fallback AdMob");
                SmallBanner.ApplovinMaxWithAdmobBackup(activity, layAds, SettingAds.APPLOVIN_BANNER, SettingAds.ADMOB_BANNER);
            }
        } else if (applovinOn) {
            Log.d(TAG, "ShowBanner[" + placement + "]: AppLovin only");
            SmallBanner.ApplovinMax(activity, layAds, SettingAds.APPLOVIN_BANNER);
        } else {
            Log.d(TAG, "ShowBanner[" + placement + "]: AdMob only");
            SmallBanner.Admob(activity, layAds, SettingAds.ADMOB_BANNER);
        }
    }

    /**
     * Backward compat — old callers tanpa placement param.
     * @deprecated use ShowBanner(activity, layAds, placement)
     */
    @Deprecated
    public static void ShowBanner(Activity activity, RelativeLayout layAds) {
        // Default ke main_select untuk old callers
        ShowBanner(activity, layAds, SettingAds.PLACEMENT_BANNER_MAIN_SELECT);
    }

    public static void LoadInterstitialAds(Activity activity, String placement) {
        if (isAdFree(activity)) {
            Log.d(TAG, "LoadInterstitialAds[" + placement + "]: premium ad-free, skip");
            return;
        }
        if (!SettingAds.shouldShow(placement)) {
            Log.d(TAG, "LoadInterstitialAds[" + placement + "]: disabled, skip preload");
            return;
        }
        Interstitial.loadAds(activity, placement);
    }

    @Deprecated
    public static void LoadInterstitialAds(Activity activity) {
        Interstitial.loadAds(activity, SettingAds.PLACEMENT_INTERSTITIAL_MAIN_SELECT);
    }

    public static void ShowInterstitialAds(Activity activity, String placement) {
        if (isAdFree(activity)) {
            Log.d(TAG, "ShowInterstitialAds[" + placement + "]: premium ad-free, skip");
            return;
        }
        if (!SettingAds.shouldShow(placement)) {
            Log.d(TAG, "ShowInterstitialAds[" + placement + "]: disabled, skip");
            return;
        }
        boolean shown = Interstitial.showInters(activity);
        Log.d(TAG, "ShowInterstitialAds[" + placement + "]: " + (shown ? "shown" : "skipped (cap/not ready)"));
    }

    public static void ShowInterstitialAds(Activity activity, String placement, Interstitial.DismissCallback callback) {
        if (isAdFree(activity)) {
            Log.d(TAG, "ShowInterstitialAds[" + placement + "]: premium ad-free, callback");
            if (callback != null) callback.onDismissed();
            return;
        }
        if (!SettingAds.shouldShow(placement)) {
            Log.d(TAG, "ShowInterstitialAds[" + placement + "]: disabled, callback langsung");
            if (callback != null) callback.onDismissed();
            return;
        }
        boolean shown = Interstitial.showInters(activity, callback);
        Log.d(TAG, "ShowInterstitialAds[" + placement + "]+callback: " + (shown ? "shown" : "skipped → callback"));
    }

    @Deprecated
    public static void ShowInterstitialAds(Activity activity) {
        ShowInterstitialAds(activity, SettingAds.PLACEMENT_INTERSTITIAL_MAIN_SELECT);
    }

    @Deprecated
    public static void ShowInterstitialAds(Activity activity, Interstitial.DismissCallback callback) {
        ShowInterstitialAds(activity, SettingAds.PLACEMENT_INTERSTITIAL_MAIN_SELECT, callback);
    }

    public static void LoadRewardsAds(Activity activity, String placement) {
        if (!SettingAds.shouldShow(placement)) {
            Log.d(TAG, "LoadRewardsAds[" + placement + "]: disabled, skip");
            return;
        }
        Rewarded.loadAds(activity, placement);
    }

    @Deprecated
    public static void LoadRewardsAds(Activity activity) {
        Rewarded.loadAds(activity, SettingAds.PLACEMENT_REWARDED_SUPER_LIKE);
    }

    public static void ShowRewardsAds(Activity activity, String placement, Rewarded.RewardCallback callback) {
        if (!SettingAds.shouldShow(placement)) {
            Log.d(TAG, "ShowRewardsAds[" + placement + "]: disabled");
            if (callback != null) callback.onAdFailedToShow("Placement disabled");
            return;
        }
        Rewarded.showRewarded(activity, callback);
    }

    @Deprecated
    public static void ShowRewardsAds(Activity activity, Rewarded.RewardCallback callback) {
        ShowRewardsAds(activity, SettingAds.PLACEMENT_REWARDED_SUPER_LIKE, callback);
    }
}
