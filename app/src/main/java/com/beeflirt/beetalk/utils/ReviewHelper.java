package com.beeflirt.beetalk.utils;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.Task;
import com.google.android.play.core.review.ReviewInfo;
import com.google.android.play.core.review.ReviewManager;
import com.google.android.play.core.review.ReviewManagerFactory;

/**
 * Helper untuk Google Play In-App Review dengan eligibility check.
 *
 * Aturan trigger:
 * - User pernah buka app minimal 3 kali (sesi)
 * - Belum pernah di-prompt review dalam 90 hari terakhir
 * - Tidak menampilkan UI custom apapun ("Thank you" toast, dll) — Google policy
 *
 * Cara pakai:
 *   ReviewHelper.trackSession(context);   // panggil di Application.onCreate atau SplashActivity
 *   ReviewHelper.tryShowReview(activity); // panggil di moment positif (match, upload foto, dll)
 */
public class ReviewHelper {

    private static final String TAG = "ReviewHelper";

    private static final String PREF_NAME = "BeeTalk_Review";
    private static final String KEY_SESSION_COUNT = "session_count";
    private static final String KEY_LAST_PROMPT_TIME = "last_prompt_time";

    // Eligibility thresholds
    private static final int MIN_SESSIONS = 3;
    private static final long COOLDOWN_DAYS = 90L;
    private static final long COOLDOWN_MS = COOLDOWN_DAYS * 24L * 60L * 60L * 1000L;

    /**
     * Track session count. Panggil sekali per session (di SplashActivity.onCreate atau
     * Application.onCreate).
     */
    public static void trackSession(@NonNull Context context) {
        SharedPreferences sp = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        int count = sp.getInt(KEY_SESSION_COUNT, 0) + 1;
        sp.edit().putInt(KEY_SESSION_COUNT, count).apply();
        Log.d(TAG, "Session count: " + count);
    }

    /**
     * Cek apakah user eligible untuk review prompt.
     */
    public static boolean isEligible(@NonNull Context context) {
        SharedPreferences sp = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);

        int sessions = sp.getInt(KEY_SESSION_COUNT, 0);
        if (sessions < MIN_SESSIONS) {
            Log.d(TAG, "Not eligible: sessions=" + sessions + " < " + MIN_SESSIONS);
            return false;
        }

        long lastPrompt = sp.getLong(KEY_LAST_PROMPT_TIME, 0);
        long now = System.currentTimeMillis();
        if (lastPrompt > 0 && (now - lastPrompt) < COOLDOWN_MS) {
            long daysLeft = (COOLDOWN_MS - (now - lastPrompt)) / (24L * 60L * 60L * 1000L);
            Log.d(TAG, "Not eligible: cooldown active, " + daysLeft + " days left");
            return false;
        }

        return true;
    }

    /**
     * Trigger review flow kalau user eligible.
     * Aman dipanggil dari moment positif manapun — kalau tidak eligible, tidak ada efek samping.
     *
     * NOTE: Tidak ada UI confirmation, tidak ada toast — sesuai Google policy.
     */
    public static void tryShowReview(@NonNull Activity activity) {
        if (!isEligible(activity)) return;

        ReviewManager manager = ReviewManagerFactory.create(activity);
        Task<ReviewInfo> request = manager.requestReviewFlow();
        request.addOnCompleteListener(infoTask -> {
            if (!infoTask.isSuccessful()) {
                Log.w(TAG, "ReviewInfo request failed: " + infoTask.getException());
                return;
            }
            ReviewInfo info = infoTask.getResult();
            Task<Void> flow = manager.launchReviewFlow(activity, info);
            flow.addOnCompleteListener(flowTask -> {
                // Mark prompted regardless of outcome — Google API tidak expose hasil
                markPrompted(activity);
                Log.d(TAG, "Review flow completed");
            });
        });
    }

    /**
     * Manual trigger — dipanggil dari tombol "Rate Now" oleh user.
     * Tidak cek eligibility (user yang request).
     * Fallback ke Play Store kalau ReviewInfo gagal.
     */
    public static void manualReview(@NonNull Activity activity) {
        ReviewManager manager = ReviewManagerFactory.create(activity);
        Task<ReviewInfo> request = manager.requestReviewFlow();
        request.addOnCompleteListener(infoTask -> {
            if (infoTask.isSuccessful()) {
                ReviewInfo info = infoTask.getResult();
                Task<Void> flow = manager.launchReviewFlow(activity, info);
                flow.addOnCompleteListener(flowTask -> {
                    markPrompted(activity);
                    Log.d(TAG, "Manual review completed");
                });
            } else {
                fallbackToPlayStore(activity);
            }
        });
    }

    private static void fallbackToPlayStore(@NonNull Activity activity) {
        try {
            android.net.Uri uri = android.net.Uri.parse("market://details?id=" + activity.getPackageName());
            activity.startActivity(new android.content.Intent(android.content.Intent.ACTION_VIEW, uri));
        } catch (android.content.ActivityNotFoundException e) {
            try {
                android.net.Uri uri = android.net.Uri.parse("https://play.google.com/store/apps/details?id=" + activity.getPackageName());
                activity.startActivity(new android.content.Intent(android.content.Intent.ACTION_VIEW, uri));
            } catch (Exception ignored) {}
        }
    }

    private static void markPrompted(@NonNull Context context) {
        SharedPreferences sp = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        sp.edit().putLong(KEY_LAST_PROMPT_TIME, System.currentTimeMillis()).apply();
    }

    /**
     * Reset state (untuk testing).
     */
    public static void resetForTesting(@NonNull Context context) {
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE).edit().clear().apply();
    }
}
