package com.beeflirt.beetalk.activities;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.beeflirt.beetalk.R;
import com.beeflirt.beetalk.adsconfig.Ads;
import com.beeflirt.beetalk.adsconfig.SettingAds;
import com.beeflirt.beetalk.utils.LocaleHelper;

public class AppActivity extends BaseActivity {
    private RelativeLayout bannerAds;
    private Button loginBtn, signupBtn;
    private ImageView back;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_app);

        bannerAds = findViewById(R.id.bannerAds);
        loginBtn = findViewById(R.id.loginBtn);
        signupBtn = findViewById(R.id.signupBtn);
        back = findViewById(R.id.back);

        if (SettingAds.ADS_APPACTIVITY.equals("1")){
            Ads.ShowBanner(this, bannerAds, SettingAds.PLACEMENT_BANNER_APP_ACTIVITY);
            Ads.LoadInterstitialAds(this, SettingAds.PLACEMENT_INTERSTITIAL_MAIN_SELECT);
        }

        // Mark onboarding as completed when user reaches this screen
        // to prevent returning to LanguageActivity on next splash
        android.content.SharedPreferences prefs = getSharedPreferences("app_prefs", MODE_PRIVATE);
        prefs.edit().putBoolean("onboarding_completed", true).apply();

        loginBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent i = new Intent(AppActivity.this, LoginActivity.class);
                startActivity(i);
            }
        });

        signupBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Langsung navigasi ke Register tanpa interstitial
                // (menampilkan iklan saat user mencoba mendaftar melanggar kebijakan AdMob)
                Intent i = new Intent(AppActivity.this, RegisterActivity.class);
                startActivity(i);
            }
        });

        back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

    }
}