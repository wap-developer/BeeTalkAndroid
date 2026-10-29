package com.beeflirt.beetalk;

import android.app.Application;
import android.util.Log;

import com.applovin.sdk.AppLovinMediationProvider;
import com.applovin.sdk.AppLovinSdk;
import com.applovin.sdk.AppLovinSdkConfiguration;
import com.applovin.sdk.AppLovinSdkInitializationConfiguration;
import com.beeflirt.beetalk.utils.Constants;
import com.google.android.gms.ads.MobileAds;

import java.util.Arrays;
import java.util.TimeZone;

public class BeeLiteApplication extends Application {

    private static final String TAG = "BeeTalkLiteApp";
    public static boolean isSdkReady = false; // Flag: AppLovin SDK sudah selesai init

    @Override
    public void onCreate() {
        super.onCreate();

        // Default timezone Asia/Jakarta (WIB)
        TimeZone.setDefault(TimeZone.getTimeZone(Constants.APP_TIMEZONE));

        ApplovinInitialize();
        AdmobInitialize();
    }

    private void ApplovinInitialize() {
        Log.d(TAG, "ApplovinInitialize: kuadran");
        AppLovinSdkInitializationConfiguration initConfig = AppLovinSdkInitializationConfiguration.builder(getResources().getString(R.string.applovin_sdk_key))
                .setMediationProvider( AppLovinMediationProvider.MAX )
//                .setTestDeviceAdvertisingIds(Arrays.asList("cf469a78-859d-4a43-8c4d-10ca1ad1abe1"))
                .build();

        // Initialize the SDK with the configuration
        AppLovinSdk.getInstance(this).initialize(initConfig, new AppLovinSdk.SdkInitializationListener()
        {
            @Override
            public void onSdkInitialized(final AppLovinSdkConfiguration sdkConfig)
            {
                isSdkReady = true;
                Log.d(TAG, "AppLovin SDK initialized successfully");
            }
        });
    }

    private void AdmobInitialize() {
        new Thread(
                () -> {
                    // Initialize the Google Mobile Ads SDK on a background thread.
                    MobileAds.initialize(this, initializationStatus -> {});
                })
                .start();
    }

}
