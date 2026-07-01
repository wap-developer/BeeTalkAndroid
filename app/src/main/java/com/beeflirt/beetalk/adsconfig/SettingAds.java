package com.beeflirt.beetalk.adsconfig;

import static com.beeflirt.beetalk.utils.Constants.BASE_URL;

import java.util.HashMap;
import java.util.Map;

public class SettingAds {
    // Ad config endpoint — served by BeeTalk admin panel (CodeIgniter 4)
    // Admin can edit values via /admin/settings/ads
    public static final String URL_DATA = BASE_URL + "ad-config";

    public static String ON_OFF_ADS = "1";

    // Waterfall priority — saat dua-duanya enabled di placement, mana coba duluan
    // "applovin" = AppLovin first, fallback AdMob
    // "admob" = AdMob first, fallback AppLovin
    public static String WATERFALL_PRIORITY = "applovin";

    // Ad Unit IDs (per network)
    public static String APPLOVIN_BANNER = "e0cdb5b6d3949138";
    public static String APPLOVIN_INTERSTITIAL = "e93dfb72c74c439b";
    public static String APPLOVIN_REWARDS = "3b55bce9775419c1";

    public static String ADMOB_BANNER = "ca-app-pub-3940256099942544/6300978111";
    public static String ADMOB_INTERSTITIAL = "ca-app-pub-3940256099942544/1033173712";
    public static String ADMOB_REWARDS = "ca-app-pub-3940256099942544/5354046379";

    // ====================================================
    // Placement keys — match dengan admin panel
    // ====================================================
    public static final String PLACEMENT_BANNER_LANGUAGE = "banner_language";
    public static final String PLACEMENT_BANNER_MAIN_SELECT = "banner_main_select";
    public static final String PLACEMENT_BANNER_APP_ACTIVITY = "banner_app_activity";
    public static final String PLACEMENT_BANNER_SWIPE = "banner_swipe";
    public static final String PLACEMENT_BANNER_EXPLORE = "banner_explore";
    public static final String PLACEMENT_BANNER_MESSAGES = "banner_messages";
    public static final String PLACEMENT_BANNER_MATCHES = "banner_matches";
    public static final String PLACEMENT_BANNER_PROFILE = "banner_profile";
    public static final String PLACEMENT_INTERSTITIAL_MAIN_SELECT = "interstitial_main_select";
    public static final String PLACEMENT_INTERSTITIAL_GEO = "interstitial_geo";
    public static final String PLACEMENT_INTERSTITIAL_SWIPE = "interstitial_swipe";
    public static final String PLACEMENT_REWARDED_SUPER_LIKE = "rewarded_super_like";

    public static final String NETWORK_APPLOVIN = "applovin";
    public static final String NETWORK_ADMOB = "admob";

    /**
     * Per-placement network toggle.
     */
    public static class PlacementConfig {
        public boolean applovinEnabled;
        public boolean admobEnabled;

        public PlacementConfig() { this(false, false); }
        public PlacementConfig(boolean applovin, boolean admob) {
            this.applovinEnabled = applovin;
            this.admobEnabled = admob;
        }
    }

    public static final Map<String, PlacementConfig> PLACEMENTS = new HashMap<>();

    static {
        // Default values (Balanced Mode equivalent) — overridden by remote config
        PLACEMENTS.put(PLACEMENT_BANNER_LANGUAGE, new PlacementConfig(true, false));
        PLACEMENTS.put(PLACEMENT_BANNER_MAIN_SELECT, new PlacementConfig(true, true));
        PLACEMENTS.put(PLACEMENT_BANNER_APP_ACTIVITY, new PlacementConfig(true, true));
        PLACEMENTS.put(PLACEMENT_BANNER_SWIPE, new PlacementConfig(true, false));
        PLACEMENTS.put(PLACEMENT_BANNER_EXPLORE, new PlacementConfig(true, true));
        PLACEMENTS.put(PLACEMENT_BANNER_MESSAGES, new PlacementConfig(true, false));
        PLACEMENTS.put(PLACEMENT_BANNER_MATCHES, new PlacementConfig(true, false));
        PLACEMENTS.put(PLACEMENT_BANNER_PROFILE, new PlacementConfig(true, true));
        PLACEMENTS.put(PLACEMENT_INTERSTITIAL_MAIN_SELECT, new PlacementConfig(true, true));
        PLACEMENTS.put(PLACEMENT_INTERSTITIAL_GEO, new PlacementConfig(false, false));
        PLACEMENTS.put(PLACEMENT_INTERSTITIAL_SWIPE, new PlacementConfig(true, false));
        PLACEMENTS.put(PLACEMENT_REWARDED_SUPER_LIKE, new PlacementConfig(true, true));
    }

    public static boolean isPlacementEnabled(String placement, String network) {
        PlacementConfig p = PLACEMENTS.get(placement);
        if (p == null) return false;
        if (NETWORK_APPLOVIN.equals(network)) return p.applovinEnabled;
        if (NETWORK_ADMOB.equals(network)) return p.admobEnabled;
        return false;
    }

    public static boolean shouldShow(String placement) {
        PlacementConfig p = PLACEMENTS.get(placement);
        if (p == null) return false;
        return p.applovinEnabled || p.admobEnabled;
    }

    public static void setPlacement(String placement, boolean applovinEnabled, boolean admobEnabled) {
        PlacementConfig p = PLACEMENTS.get(placement);
        if (p == null) {
            PLACEMENTS.put(placement, new PlacementConfig(applovinEnabled, admobEnabled));
        } else {
            p.applovinEnabled = applovinEnabled;
            p.admobEnabled = admobEnabled;
        }
    }

    // ====================================================
    // Advanced settings
    // ====================================================

    public static boolean CHILD_DIRECT_GDPR = false;
    public static int INTERVAL_CAPS = 0;
    public static String STATUS_APP = "0";
    public static String SUSPENSION_MESSAGE = "Aplikasi ini sudah tidak tersedia. Silakan pindah ke aplikasi baru kami.";
    public static String LINK_REDIRECT = "https://play.google.com/store/apps/details?id=com.beeflirt.beetalk";

    // App Update mode (controlled by admin panel via /api/ad-config)
    public static final String UPDATE_MODE_OFF = "off";
    public static final String UPDATE_MODE_FLEXIBLE = "flexible";
    public static final String UPDATE_MODE_IMMEDIATE = "immediate";
    public static String UPDATE_MODE = UPDATE_MODE_FLEXIBLE;

    // ====================================================
    // DEPRECATED — kept for backward compat with old code paths
    // ====================================================
    @Deprecated public static String SELECT_MAIN_ADS = "APPLOVIN";
    @Deprecated public static String MAIN_ADS_INTERTITIAL = APPLOVIN_INTERSTITIAL;
    @Deprecated public static String MAIN_ADS_INTERTITIAL_BACKUP = ADMOB_INTERSTITIAL;
    @Deprecated public static String MAIN_ADS_BANNER = APPLOVIN_BANNER;
    @Deprecated public static String MAIN_ADS_BANNER_BACKUP = ADMOB_BANNER;
    @Deprecated public static String MAIN_ADS_REWARDS = APPLOVIN_REWARDS;
    @Deprecated public static String MAIN_ADS_REWARDS_BACKUP = ADMOB_REWARDS;
    @Deprecated public static String ADS_MAINSELECT = "1";
    @Deprecated public static String INTERS_MAINSELECT = "1";
    @Deprecated public static String ADS_APPACTIVITY = "1";
    @Deprecated public static String ADS_GEO = "1";
}
