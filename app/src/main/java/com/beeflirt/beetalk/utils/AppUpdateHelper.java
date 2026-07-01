package com.beeflirt.beetalk.utils;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.annotation.NonNull;

import com.beeflirt.beetalk.adsconfig.SettingAds;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.play.core.appupdate.AppUpdateInfo;
import com.google.android.play.core.appupdate.AppUpdateManager;
import com.google.android.play.core.appupdate.AppUpdateManagerFactory;
import com.google.android.play.core.appupdate.AppUpdateOptions;
import com.google.android.play.core.install.InstallStateUpdatedListener;
import com.google.android.play.core.install.model.AppUpdateType;
import com.google.android.play.core.install.model.InstallStatus;
import com.google.android.play.core.install.model.UpdateAvailability;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class AppUpdateHelper {

    private static final String TAG = "AppUpdateHelper";
    public static final int REQUEST_CODE_UPDATE = 5021;

    private static final String PREF_NAME = "BeeTalk_AppUpdate";
    private static final String KEY_LAST_CHECK_DATE = "last_update_check_date";

    public interface OnCompleteCallback {
        void onComplete();
    }

    private static AppUpdateManager appUpdateManager;
    private static InstallStateUpdatedListener installListener;

    /**
     * Entry point: check update mode dan trigger flow yang sesuai.
     */
    public static void checkAndPromptUpdate(Activity activity, String mode, OnCompleteCallback onComplete) {
        if (mode == null || SettingAds.UPDATE_MODE_OFF.equalsIgnoreCase(mode)) {
            Log.d(TAG, "Update mode OFF, skip");
            if (onComplete != null) onComplete.onComplete();
            return;
        }

        if (!shouldCheckToday(activity)) {
            Log.d(TAG, "Already checked today, skip");
            if (onComplete != null) onComplete.onComplete();
            return;
        }

        appUpdateManager = AppUpdateManagerFactory.create(activity.getApplicationContext());

        boolean isImmediate = SettingAds.UPDATE_MODE_IMMEDIATE.equalsIgnoreCase(mode);
        int updateType = isImmediate ? AppUpdateType.IMMEDIATE : AppUpdateType.FLEXIBLE;

        appUpdateManager.getAppUpdateInfo()
                .addOnSuccessListener(info -> handleUpdateInfo(activity, info, updateType, isImmediate, onComplete))
                .addOnFailureListener(e -> {
                    Log.w(TAG, "Failed to check update: " + e.getMessage());
                    if (onComplete != null) onComplete.onComplete();
                });
    }

    private static void handleUpdateInfo(Activity activity, AppUpdateInfo info, int updateType, boolean isImmediate, OnCompleteCallback onComplete) {
        markChecked(activity);

        boolean updateAvailable = info.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE;
        boolean alreadyDownloading = info.updateAvailability() == UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS;

        if (!updateAvailable && !alreadyDownloading) {
            Log.d(TAG, "No update available");
            if (onComplete != null) onComplete.onComplete();
            return;
        }

        if (!info.isUpdateTypeAllowed(updateType)) {
            Log.d(TAG, "Update type " + updateType + " not allowed by Play");
            if (onComplete != null) onComplete.onComplete();
            return;
        }

        try {
            if (isImmediate) {
                Log.d(TAG, "Launching IMMEDIATE update flow");
                appUpdateManager.startUpdateFlowForResult(
                        info,
                        activity,
                        AppUpdateOptions.newBuilder(AppUpdateType.IMMEDIATE).build(),
                        REQUEST_CODE_UPDATE
                );
                // IMMEDIATE memblokir — onComplete tidak dipanggil
            } else {
                Log.d(TAG, "Launching FLEXIBLE update flow");
                installListener = state -> {
                    if (state.installStatus() == InstallStatus.DOWNLOADED) {
                        showCompleteUpdateSnackbar(activity);
                    }
                };
                appUpdateManager.registerListener(installListener);

                appUpdateManager.startUpdateFlowForResult(
                        info,
                        activity,
                        AppUpdateOptions.newBuilder(AppUpdateType.FLEXIBLE).build(),
                        REQUEST_CODE_UPDATE
                );
                if (onComplete != null) onComplete.onComplete();
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to launch update flow: " + e.getMessage());
            if (onComplete != null) onComplete.onComplete();
        }
    }

    private static void showCompleteUpdateSnackbar(Activity activity) {
        try {
            android.view.View root = activity.findViewById(android.R.id.content);
            if (root == null) {
                appUpdateManager.completeUpdate();
                return;
            }
            Snackbar sb = Snackbar.make(root, "Update selesai diunduh. Restart sekarang?", Snackbar.LENGTH_INDEFINITE);
            sb.setAction("Restart", v -> appUpdateManager.completeUpdate());
            sb.show();
        } catch (Exception e) {
            appUpdateManager.completeUpdate();
        }
    }

    /**
     * Resume check — dipanggil dari Activity.onResume().
     */
    public static void resumeUpdateCheck(Activity activity) {
        if (appUpdateManager == null) return;

        appUpdateManager.getAppUpdateInfo().addOnSuccessListener(info -> {
            if (info.updateAvailability() == UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS) {
                try {
                    appUpdateManager.startUpdateFlowForResult(
                            info,
                            activity,
                            AppUpdateOptions.newBuilder(AppUpdateType.IMMEDIATE).build(),
                            REQUEST_CODE_UPDATE
                    );
                } catch (Exception ignored) {}
            }
            if (info.installStatus() == InstallStatus.DOWNLOADED) {
                showCompleteUpdateSnackbar(activity);
            }
        });
    }

    public static void unregisterListener() {
        if (appUpdateManager != null && installListener != null) {
            appUpdateManager.unregisterListener(installListener);
            installListener = null;
        }
    }

    // ===== Frequency Cap (1x per day) =====

    private static boolean shouldCheckToday(@NonNull Context context) {
        SharedPreferences sp = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        String saved = sp.getString(KEY_LAST_CHECK_DATE, null);
        String today = todayString();
        return saved == null || !saved.equals(today);
    }

    private static void markChecked(@NonNull Context context) {
        SharedPreferences sp = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        sp.edit().putString(KEY_LAST_CHECK_DATE, todayString()).apply();
    }

    private static String todayString() {
        return new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date());
    }
}
