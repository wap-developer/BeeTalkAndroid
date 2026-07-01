package com.beeflirt.beetalk.adsconfig;

import static com.beeflirt.beetalk.adsconfig.SettingAds.ADMOB_BANNER;
import static com.beeflirt.beetalk.adsconfig.SettingAds.APPLOVIN_BANNER;
import static com.beeflirt.beetalk.adsconfig.SettingAds.CHILD_DIRECT_GDPR;
import static com.beeflirt.beetalk.adsconfig.SettingAds.NETWORK_ADMOB;
import static com.beeflirt.beetalk.adsconfig.SettingAds.NETWORK_APPLOVIN;
import static com.beeflirt.beetalk.adsconfig.SettingAds.URL_DATA;
import static com.beeflirt.beetalk.adsconfig.SettingAds.WATERFALL_PRIORITY;

import android.app.Activity;
import android.util.Log;
import android.widget.RelativeLayout;

import com.beeflirt.beetalk.BuildConfig;
import com.beeflirt.beetalk.ads.GDPR;
import com.beeflirt.beetalk.ads.Interstitial;
import com.beeflirt.beetalk.ads.Rewarded;
import com.beeflirt.beetalk.ads.SmallBanner;

public class Ads {
    public static String APPID;
    public static String DATA;

    private static final String TAG = "Ads";

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
        boolean applovinOn = SettingAds.isPlacementEnabled(placement, NETWORK_APPLOVIN);
        boolean admobOn = SettingAds.isPlacementEnabled(placement, NETWORK_ADMOB);

        if (!applovinOn && !admobOn) {
            Log.d(TAG, "ShowBanner[" + placement + "]: both disabled, skip");
            return;
        }

        if (applovinOn && admobOn) {
            // Waterfall: try priority first, fallback the other
            if ("admob".equals(WATERFALL_PRIORITY)) {
                Log.d(TAG, "ShowBanner[" + placement + "]: AdMob → fallback AppLovin");
                SmallBanner.AdmobWithApplovinBackup(activity, layAds, ADMOB_BANNER, APPLOVIN_BANNER);
            } else {
                Log.d(TAG, "ShowBanner[" + placement + "]: AppLovin → fallback AdMob");
                SmallBanner.ApplovinMaxWithAdmobBackup(activity, layAds, APPLOVIN_BANNER, ADMOB_BANNER);
            }
        } else if (applovinOn) {
            Log.d(TAG, "ShowBanner[" + placement + "]: AppLovin only");
            SmallBanner.ApplovinMax(activity, layAds, APPLOVIN_BANNER);
        } else {
            Log.d(TAG, "ShowBanner[" + placement + "]: AdMob only");
            SmallBanner.Admob(activity, layAds, ADMOB_BANNER);
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
        if (!SettingAds.shouldShow(placement)) {
            Log.d(TAG, "LoadInterstitialAds[" + placement + "]: disabled, skip preload");
            return;
        }
        Interstitial.loadAds(activity);
    }

    @Deprecated
    public static void LoadInterstitialAds(Activity activity) {
        Interstitial.loadAds(activity);
    }

    public static void ShowInterstitialAds(Activity activity, String placement) {
        if (!SettingAds.shouldShow(placement)) {
            Log.d(TAG, "ShowInterstitialAds[" + placement + "]: disabled, skip");
            return;
        }
        boolean shown = Interstitial.showInters(activity);
        Log.d(TAG, "ShowInterstitialAds[" + placement + "]: " + (shown ? "shown" : "skipped (cap/not ready)"));
    }

    public static void ShowInterstitialAds(Activity activity, String placement, Interstitial.DismissCallback callback) {
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
        Rewarded.loadAds(activity);
    }

    @Deprecated
    public static void LoadRewardsAds(Activity activity) {
        Rewarded.loadAds(activity);
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
