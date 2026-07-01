package com.beeflirt.beetalk.ads;


import android.app.Activity;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;

import androidx.annotation.NonNull;

import com.applovin.mediation.MaxAd;
import com.applovin.mediation.MaxAdViewAdListener;
import com.applovin.mediation.MaxError;
import com.applovin.mediation.ads.MaxAdView;
import com.beeflirt.beetalk.R;
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.LoadAdError;

public class SmallBanner {

    private static final String TAG = "SmallBanner";
    public static boolean isAdMobLoaded = false;
    public static MaxAdView adViewMax;
    public static AdView adViewAdmob;


    public static void ApplovinMax(Activity activity, RelativeLayout layAds, String idBanner) {
        Log.d(TAG, "Loading AppLovin banner...");

        adViewMax = new MaxAdView(idBanner, activity);
        adViewMax.setListener(new MaxAdViewAdListener() {
            @Override
            public void onAdExpanded(@NonNull MaxAd maxAd) {
                // Ad expanded to fullscreen, no action needed here
            }

            @Override
            public void onAdCollapsed(@NonNull MaxAd maxAd) {

            }

            @Override
            public void onAdLoaded(@NonNull MaxAd maxAd) {
                Log.d(TAG, "onAdLoaded: " + maxAd);
                // Banner sudah attached sebelum loadAd(), cukup pastikan visible
                adViewMax.setVisibility(android.view.View.VISIBLE);
            }

            @Override
            public void onAdDisplayed(@NonNull MaxAd maxAd) {

            }

            @Override
            public void onAdHidden(@NonNull MaxAd maxAd) {

            }

            @Override
            public void onAdClicked(@NonNull MaxAd maxAd) {

            }

            @Override
            public void onAdLoadFailed(@NonNull String s, @NonNull MaxError maxError) {
                Log.d(TAG, "onAdLoadFailed: " + maxError);
                // Retry setelah 5 detik agar tidak loop berlebihan
                new Handler(Looper.getMainLooper()).postDelayed(() -> {
                    if (!activity.isFinishing() && !activity.isDestroyed()) {
                        adViewMax.loadAd();
                    }
                }, 5000);
            }

            @Override
            public void onAdDisplayFailed(@NonNull MaxAd maxAd, @NonNull MaxError maxError) {
                Log.d(TAG, "onAdDisplayFailed: " + maxError);
            }
        });

        // Set background color for banners to be fully functional
        adViewMax.setBackgroundColor(android.graphics.Color.TRANSPARENT);

        // PENTING: Attach ke layout SEBELUM loadAd() agar AppLovin bisa render
        int width = ViewGroup.LayoutParams.MATCH_PARENT;
        int heightPx = activity.getResources().getDimensionPixelSize(R.dimen.banner_height);
        adViewMax.setLayoutParams(new FrameLayout.LayoutParams(width, heightPx));
        adViewMax.setVisibility(android.view.View.GONE); // sembunyikan dulu
        layAds.removeAllViews();
        layAds.addView(adViewMax);

        // Load ad
        adViewMax.loadAd();
    }

    public static void Admob(Activity activity, RelativeLayout layAds, String idBanner) {
        adViewAdmob = new AdView(activity);

        // Urutan wajib: setAdUnitId → setAdSize → addView → setListener → loadAd
        adViewAdmob.setAdUnitId(idBanner);
        AdSize adSize = getAdSize(activity);
        adViewAdmob.setAdSize(adSize);

        layAds.removeAllViews();
        layAds.addView(adViewAdmob);

        adViewAdmob.setAdListener(new AdListener() {
            @Override
            public void onAdLoaded() {
                Log.d(TAG, "AdMob banner loaded successfully");
                isAdMobLoaded = true;
            }

            @Override
            public void onAdFailedToLoad(@NonNull LoadAdError error) {
                Log.e(TAG, "AdMob banner failed: " + error.getMessage());
                isAdMobLoaded = false;
                // Retry setelah 3 detik untuk menghindari infinite loop
                new Handler(Looper.getMainLooper()).postDelayed(() -> {
                    if (!activity.isFinishing() && !activity.isDestroyed()) {
                        AdRequest retryRequest = new AdRequest.Builder().build();
                        adViewAdmob.loadAd(retryRequest);
                    }
                }, 3000);
            }
        });

        AdRequest adRequest = new AdRequest.Builder().build();
        adViewAdmob.loadAd(adRequest);
    }

    /**
     * AppLovin sebagai PRIMARY, AdMob sebagai BACKUP.
     * Jika AppLovin gagal load → otomatis load AdMob.
     * SELECT_MAIN_ADS = "APPLOVIN_ADMOB"
     */
    public static void ApplovinMaxWithAdmobBackup(Activity activity, RelativeLayout layAds,
                                                   String idApplovin, String idAdmobBackup) {
        Log.d(TAG, "Loading AppLovin (primary) | AdMob (backup)...");

        adViewMax = new MaxAdView(idApplovin, activity);
        adViewMax.setListener(new MaxAdViewAdListener() {
            @Override public void onAdExpanded(@NonNull MaxAd maxAd) {}
            @Override public void onAdCollapsed(@NonNull MaxAd maxAd) {}
            @Override public void onAdDisplayed(@NonNull MaxAd maxAd) {}
            @Override public void onAdHidden(@NonNull MaxAd maxAd) {}
            @Override public void onAdClicked(@NonNull MaxAd maxAd) {}

            @Override
            public void onAdLoaded(@NonNull MaxAd maxAd) {
                Log.d(TAG, "AppLovin (primary) loaded ✓");
                adViewMax.setVisibility(android.view.View.VISIBLE);
            }

            @Override
            public void onAdLoadFailed(@NonNull String s, @NonNull MaxError maxError) {
                Log.w(TAG, "AppLovin (primary) gagal → fallback ke AdMob. Error: " + maxError);
                // Bersihkan AppLovin yang gagal
                if (adViewMax != null) {
                    adViewMax.destroy();
                    adViewMax = null;
                }
                // Fallback ke AdMob (satu kali, tanpa rekursif)
                if (!activity.isFinishing() && !activity.isDestroyed()) {
                    Admob(activity, layAds, idAdmobBackup);
                }
            }

            @Override
            public void onAdDisplayFailed(@NonNull MaxAd maxAd, @NonNull MaxError maxError) {
                Log.d(TAG, "onAdDisplayFailed: " + maxError);
            }
        });

        adViewMax.setBackgroundColor(android.graphics.Color.TRANSPARENT);

        int width = ViewGroup.LayoutParams.MATCH_PARENT;
        int heightPx = activity.getResources().getDimensionPixelSize(R.dimen.banner_height);
        adViewMax.setLayoutParams(new FrameLayout.LayoutParams(width, heightPx));
        adViewMax.setVisibility(android.view.View.GONE);
        layAds.removeAllViews();
        layAds.addView(adViewMax);

        adViewMax.loadAd();
    }

    /**
     * AdMob sebagai PRIMARY, AppLovin sebagai BACKUP.
     * Jika AdMob gagal load → otomatis load AppLovin.
     * SELECT_MAIN_ADS = "ADMOB_APPLOVIN"
     */
    public static void AdmobWithApplovinBackup(Activity activity, RelativeLayout layAds,
                                                String idAdmob, String idApplovinBackup) {
        Log.d(TAG, "Loading AdMob (primary) | AppLovin (backup)...");

        adViewAdmob = new AdView(activity);
        adViewAdmob.setAdUnitId(idAdmob);
        AdSize adSize = getAdSize(activity);
        adViewAdmob.setAdSize(adSize);

        layAds.removeAllViews();
        layAds.addView(adViewAdmob);

        adViewAdmob.setAdListener(new AdListener() {
            @Override
            public void onAdLoaded() {
                Log.d(TAG, "AdMob (primary) loaded ✓");
                isAdMobLoaded = true;
            }

            @Override
            public void onAdFailedToLoad(@NonNull LoadAdError error) {
                Log.w(TAG, "AdMob (primary) gagal → fallback ke AppLovin. Error: " + error.getMessage());
                isAdMobLoaded = false;
                // Fallback ke AppLovin (satu kali, tanpa rekursif)
                if (!activity.isFinishing() && !activity.isDestroyed()) {
                    ApplovinMax(activity, layAds, idApplovinBackup);
                }
            }
        });

        AdRequest adRequest = new AdRequest.Builder().build();
        adViewAdmob.loadAd(adRequest);
    }

    // Method ini tidak lagi digunakan karena adViewMax sudah di-attach sebelum loadAd()
    // Visibility diatur langsung di onAdLoaded
    private static void showAppLovinBanner(RelativeLayout layads, Activity activity) {
        if (layads != null && adViewMax != null) {
            adViewMax.setVisibility(android.view.View.VISIBLE);
        }
    }

    private static AdSize getAdSize(Activity activity) {
        int adWidthDp;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // API 30+: gunakan WindowMetrics (tidak deprecated)
            android.graphics.Rect bounds = activity.getWindowManager().getCurrentWindowMetrics().getBounds();
            android.view.WindowInsets windowInsets = activity.getWindowManager()
                    .getCurrentWindowMetrics().getWindowInsets();
            android.graphics.Insets insets = windowInsets.getInsetsIgnoringVisibility(
                    android.view.WindowInsets.Type.systemBars());
            int adWidthPx = bounds.width() - insets.left - insets.right;
            adWidthDp = (int) (adWidthPx / activity.getResources().getDisplayMetrics().density);
        } else {
            // API < 30: fallback dengan DisplayMetrics
            DisplayMetrics outMetrics = new DisplayMetrics();
            activity.getWindowManager().getDefaultDisplay().getMetrics(outMetrics);
            adWidthDp = (int) (outMetrics.widthPixels / outMetrics.density);
        }

        // AnchoredAdaptiveBanner = ukuran adaptif untuk banner tetap (bukan scrollable)
        return AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(activity, adWidthDp);
    }

}
