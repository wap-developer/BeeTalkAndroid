package com.beeflirt.beetalk.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Super Like Quota Manager (Client-Side)
 * Manages daily Super Like quota with local storage
 * 
 * Features:
 * - Daily quota limit (default: 5)
 * - Auto-reset at midnight
 * - Rewarded ad bonus tracking
 * - Persistent storage
 */
public class SuperLikeQuotaManager {

    private static final String TAG = "SuperLikeQuotaManager";
    private static final String PREFS_NAME = "super_like_prefs";

    // Keys
    private static final String KEY_QUOTA_REMAINING = "quota_remaining";
    private static final String KEY_LAST_RESET_DATE = "last_reset_date";
    private static final String KEY_REWARDED_BONUS = "rewarded_bonus";

    // Configuration
    private static final int DAILY_QUOTA = 5;
    private static final int REWARDED_AD_BONUS = 1;

    private SharedPreferences prefs;
    private Context context;

    public SuperLikeQuotaManager(Context context) {
        this.context = context;
        this.prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

        // Check if need to reset quota
        checkAndResetQuota();
    }

    /**
     * Get current remaining quota
     */
    public int getRemainingQuota() {
        checkAndResetQuota();
        return prefs.getInt(KEY_QUOTA_REMAINING, DAILY_QUOTA);
    }

    /**
     * Use one Super Like
     * Returns true if successful, false if no quota
     */
    public boolean useSuperLike() {
        int remaining = getRemainingQuota();

        if (remaining <= 0) {
            Log.w(TAG, "No Super Likes remaining");
            return false;
        }

        prefs.edit()
                .putInt(KEY_QUOTA_REMAINING, remaining - 1)
                .apply();

        Log.d(TAG, "Super Like used. Remaining: " + (remaining - 1));
        return true;
    }

    /**
     * Grant bonus Super Like from rewarded ad
     */
    public void grantRewardedBonus() {
        int current = getRemainingQuota();
        int newQuota = current + REWARDED_AD_BONUS;

        prefs.edit()
                .putInt(KEY_QUOTA_REMAINING, newQuota)
                .putInt(KEY_REWARDED_BONUS, prefs.getInt(KEY_REWARDED_BONUS, 0) + 1)
                .apply();

        Log.d(TAG, "Rewarded bonus granted. New quota: " + newQuota);
    }

    /**
     * Check if quota needs daily reset
     */
    private void checkAndResetQuota() {
        String today = getTodayDate();
        String lastReset = prefs.getString(KEY_LAST_RESET_DATE, "");

        if (!today.equals(lastReset)) {
            // New day - reset quota
            resetQuota();
        }
    }

    /**
     * Reset quota to daily limit
     */
    private void resetQuota() {
        String today = getTodayDate();

        prefs.edit()
                .putInt(KEY_QUOTA_REMAINING, DAILY_QUOTA)
                .putString(KEY_LAST_RESET_DATE, today)
                .putInt(KEY_REWARDED_BONUS, 0) // Reset bonus counter
                .apply();

        Log.d(TAG, "Quota reset for new day: " + today);
    }

    /**
     * Get today's date as string (yyyy-MM-dd)
     */
    private String getTodayDate() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        return sdf.format(new Date());
    }

    /**
     * Get total rewarded bonuses earned today
     */
    public int getTodayRewardedBonuses() {
        return prefs.getInt(KEY_REWARDED_BONUS, 0);
    }

    /**
     * Check if user can use Super Like
     */
    public boolean canUseSuperLike() {
        return getRemainingQuota() > 0;
    }

    /**
     * Get daily quota limit
     */
    public int getDailyQuota() {
        return DAILY_QUOTA;
    }

    /**
     * Manual reset (for testing)
     */
    public void forceReset() {
        resetQuota();
        Log.d(TAG, "Quota force reset");
    }

    /**
     * Get quota info for display
     */
    public String getQuotaDisplayText() {
        int remaining = getRemainingQuota();
        return remaining + " ⭐ Super Likes left today";
    }
}
