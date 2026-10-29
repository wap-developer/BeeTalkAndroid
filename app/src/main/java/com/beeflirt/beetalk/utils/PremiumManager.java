package com.beeflirt.beetalk.utils;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Local cache of server entitlements.
 * Source of truth = GET /premium/entitlements.
 */
public class PremiumManager {

    private static final String PREFS = "premium_prefs";
    private static final String KEY_PREMIUM_ENABLED = "premium_enabled";
    private static final String KEY_IS_PREMIUM = "is_premium";
    private static final String KEY_UNTIL = "premium_until";
    private static final String KEY_SUPER_LIKE = "super_like_daily";
    private static final String KEY_UNDO = "undo_daily";
    private static final String KEY_PRODUCT_WEEKLY = "product_weekly";
    private static final String KEY_PRODUCT_MONTHLY = "product_monthly";
    private static final String KEY_PRODUCT_YEARLY = "product_yearly";
    private static final String KEY_PRODUCT_BOOST = "product_boost";
    private static final String KEY_AD_FREE = "ad_free";
    private static final String KEY_BOOST_UNTIL = "boost_until";

    private static PremiumManager instance;
    private final SharedPreferences prefs;

    private PremiumManager(Context context) {
        prefs = context.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public static synchronized PremiumManager getInstance(Context context) {
        if (instance == null) {
            instance = new PremiumManager(context);
        }
        return instance;
    }

    public boolean isPremiumFeatureEnabled() {
        return prefs.getBoolean(KEY_PREMIUM_ENABLED, false);
    }

    public boolean isPremium() {
        return isPremiumFeatureEnabled() && prefs.getBoolean(KEY_IS_PREMIUM, false);
    }

    public String getPremiumUntil() {
        return prefs.getString(KEY_UNTIL, null);
    }

    public int getSuperLikeDaily() {
        return prefs.getInt(KEY_SUPER_LIKE, 5);
    }

    public int getUndoDaily() {
        return prefs.getInt(KEY_UNDO, 1);
    }

    public String getProductWeekly() {
        return prefs.getString(KEY_PRODUCT_WEEKLY, "premium_weekly");
    }

    public String getProductMonthly() {
        return prefs.getString(KEY_PRODUCT_MONTHLY, "premium_monthly");
    }

    public String getProductYearly() {
        return prefs.getString(KEY_PRODUCT_YEARLY, "premium_yearly");
    }

    public String getProductBoost() {
        return prefs.getString(KEY_PRODUCT_BOOST, "boost_30m");
    }

    public boolean isAdFree() {
        return isPremiumFeatureEnabled() && prefs.getBoolean(KEY_AD_FREE, false);
    }

    public String getBoostUntil() {
        return prefs.getString(KEY_BOOST_UNTIL, null);
    }

    public boolean isBoostActive() {
        if (!isPremiumFeatureEnabled()) return false;
        String until = getBoostUntil();
        if (until == null || until.isEmpty() || "null".equals(until)) return false;
        try {
            // server format: yyyy-MM-dd HH:mm:ss — loose parse via epoch if ISO, else treat non-empty active
            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.US);
            sdf.setTimeZone(java.util.TimeZone.getTimeZone(Constants.APP_TIMEZONE));
            java.util.Date d = sdf.parse(until);
            return d != null && d.getTime() > System.currentTimeMillis();
        } catch (Exception e) {
            return false;
        }
    }

    public void applySnapshot(boolean isPremium, String until,
                              int superLikeDaily, int undoDaily,
                              String weekly, String monthly, String yearly) {
        applySnapshot(false, isPremium, until, superLikeDaily, undoDaily,
                weekly, monthly, yearly, null, isPremium, null);
    }

    public void applySnapshot(boolean isPremium, String until,
                              int superLikeDaily, int undoDaily,
                              String weekly, String monthly, String yearly,
                              String boostProduct, boolean adFree, String boostUntil) {
        applySnapshot(false, isPremium, until, superLikeDaily, undoDaily,
                weekly, monthly, yearly, boostProduct, adFree, boostUntil);
    }

    public void applySnapshot(boolean premiumEnabled, boolean isPremium, String until,
                              int superLikeDaily, int undoDaily,
                              String weekly, String monthly, String yearly,
                              String boostProduct, boolean adFree, String boostUntil) {
        prefs.edit()
                .putBoolean(KEY_PREMIUM_ENABLED, premiumEnabled)
                .putBoolean(KEY_IS_PREMIUM, isPremium)
                .putString(KEY_UNTIL, until)
                .putInt(KEY_SUPER_LIKE, superLikeDaily)
                .putInt(KEY_UNDO, undoDaily)
                .putString(KEY_PRODUCT_WEEKLY, weekly != null ? weekly : "premium_weekly")
                .putString(KEY_PRODUCT_MONTHLY, monthly != null ? monthly : "premium_monthly")
                .putString(KEY_PRODUCT_YEARLY, yearly != null ? yearly : "premium_yearly")
                .putString(KEY_PRODUCT_BOOST, boostProduct != null ? boostProduct : "boost_30m")
                .putBoolean(KEY_AD_FREE, adFree)
                .putString(KEY_BOOST_UNTIL, boostUntil)
                .apply();
    }

    public void clear() {
        prefs.edit().clear().apply();
    }
}
