package com.beeflirt.beetalk.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.beeflirt.beetalk.R;
import com.beeflirt.beetalk.adsconfig.Ads;
import com.beeflirt.beetalk.utils.LocaleHelper;
import com.google.android.material.bottomsheet.BottomSheetDialog;

import android.content.Context;

public class LanguageActivity extends BaseActivity {
    private static final String TAG = "WelcomeActivity";
    private TextView languageBtn, privacyPolicy;
    private CardView btnNext;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        applyTopInset = false;
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_language);

        Ads.LoadGDPR(this);

        languageBtn = findViewById(R.id.languageBtn);
        updateLanguageButtonText();
        
        languageBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showLanguageDialog();
            }
        });

        privacyPolicy = findViewById(R.id.privacy_policy);
        privacyPolicy.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
//                Intent i = new Intent(LanguageActivity.this, WebViewActivity.class);
//                i.putExtra("url", "https://beetalk.rextdev.cyou/privacy.html");
//                i.putExtra("title", "Privacy Policy");
//                startActivity(i);
                Intent intent = new Intent(LanguageActivity.this, PolicyViewerActivity.class);
                intent.putExtra(PolicyViewerActivity.EXTRA_POLICY_TYPE, PolicyViewerActivity.POLICY_TYPE_PRIVACY);
                startActivity(intent);
            }
        });

        RelativeLayout bannerAds = findViewById(R.id.bannerAds);
        Ads.ShowBanner(LanguageActivity.this, bannerAds, com.beeflirt.beetalk.adsconfig.SettingAds.PLACEMENT_BANNER_LANGUAGE);

        btnNext = findViewById(R.id.btnNext);
        btnNext.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent i = new Intent(LanguageActivity.this, MainSelectActivity.class);
                startActivity(i);
            }
        });
    }

    private void updateLanguageButtonText() {
        String lang = LocaleHelper.getLanguage(this);
        String langText = "English";
        if (lang.equals("in")) langText = "Indonesia";
        else if (lang.equals("th")) langText = "Thailand";
        
        languageBtn.setText("Language: " + langText);
    }

    private void showLanguageDialog() {
        BottomSheetDialog dialog = new BottomSheetDialog(this);
        View view = getLayoutInflater().inflate(R.layout.dialog_language_selection, null);
        dialog.setContentView(view);

        CardView btnIn = view.findViewById(R.id.btnIndonesian);
        CardView btnEn = view.findViewById(R.id.btnEnglish);
        CardView btnTh = view.findViewById(R.id.btnThai);
        
        View checkIn = view.findViewById(R.id.checkIndonesian);
        View checkEn = view.findViewById(R.id.checkEnglish);
        View checkTh = view.findViewById(R.id.checkThai);

        String currentLang = LocaleHelper.getLanguage(this);
        if (currentLang.equals("in")) checkIn.setVisibility(View.VISIBLE);
        else if (currentLang.equals("th")) checkTh.setVisibility(View.VISIBLE);
        else checkEn.setVisibility(View.VISIBLE);

        btnIn.setOnClickListener(v -> setAppLocale("in", dialog));
        btnEn.setOnClickListener(v -> setAppLocale("en", dialog));
        btnTh.setOnClickListener(v -> setAppLocale("th", dialog));

        dialog.show();
    }

    private void setAppLocale(String lang, BottomSheetDialog dialog) {
        LocaleHelper.setLocale(this, lang);
        dialog.dismiss();
        
        // Restart activity to apply changes
        Intent intent = getIntent();
        finish();
        startActivity(intent);
    }

}