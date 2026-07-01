package com.beeflirt.beetalk.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.beeflirt.beetalk.adsconfig.SettingAds;
import com.beeflirt.beetalk.utils.ReviewHelper;

import androidx.constraintlayout.widget.ConstraintLayout;

import com.beeflirt.beetalk.R;
import com.beeflirt.beetalk.adsconfig.Ads;

public class MainSelectActivity extends BaseActivity {
    private TextView titlewelcome;
    private com.airbnb.lottie.LottieAnimationView lottieTop;
    private ConstraintLayout bottomContainer;
    private Button next, ratenow;
    private RelativeLayout bannerAds;
    private ImageView back;

    private static final String TAG = "MainSelectActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_select);

        next = findViewById(R.id.nextButton);
        ratenow = findViewById(R.id.rateNow);
        titlewelcome = findViewById(R.id.titlewelcome);
        lottieTop = findViewById(R.id.lottieTop);
        back = findViewById(R.id.back);

        back.setOnClickListener(v -> finish());

        bannerAds = findViewById(R.id.bannerAds);

        if (SettingAds.ADS_MAINSELECT.equals("1")) {
            Ads.ShowBanner(this, bannerAds, SettingAds.PLACEMENT_BANNER_MAIN_SELECT);
            Ads.LoadInterstitialAds(this, SettingAds.PLACEMENT_INTERSTITIAL_MAIN_SELECT);
        }

        rateOnclick();
        nextOnclick();

        // Auto-trigger review prompt kalau user eligible
        // (sesi >= 3 + cooldown 90 hari belum lewat). Sesuai Google Policy: tanpa toast/dialog.
        ReviewHelper.tryShowReview(this);
    }

    private void rateOnclick() {
        // Manual rate button — selalu trigger flow, fallback ke Play Store kalau gagal.
        // Tidak ada toast/dialog konfirmasi sesuai Google Play In-App Review policy.
        ratenow.setOnClickListener(v -> ReviewHelper.manualReview(MainSelectActivity.this));
    }

    private void nextOnclick() {
        next.setOnClickListener(v -> {
            if (SettingAds.INTERS_MAINSELECT.equals("1")) {
                Ads.ShowInterstitialAds(MainSelectActivity.this, SettingAds.PLACEMENT_INTERSTITIAL_MAIN_SELECT, () -> {
                    Intent i = new Intent(MainSelectActivity.this, AppActivity.class);
                    startActivity(i);
                    finish();
                });
            } else {
                Intent i = new Intent(MainSelectActivity.this, AppActivity.class);
                startActivity(i);
                finish();
            }
        });
    }
}