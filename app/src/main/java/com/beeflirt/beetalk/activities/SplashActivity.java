package com.beeflirt.beetalk.activities;

import static com.beeflirt.beetalk.adsconfig.SettingAds.ON_OFF_ADS;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.beeflirt.beetalk.MainActivity;
import com.beeflirt.beetalk.R;
import com.beeflirt.beetalk.adsconfig.Ads;
import com.beeflirt.beetalk.adsconfig.SettingAds;
import com.beeflirt.beetalk.utils.ReviewHelper;
import com.beeflirt.beetalk.utils.SharedPrefManager;
import com.bumptech.glide.Glide;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class SplashActivity extends BaseActivity {

    private static final int SPLASH_DELAY = 2500; // 2.5 seconds

    private CardView cardLogo;
    private ImageView ivLogo, loader;
    private TextView tvAppName;
    private TextView tvTagline;

    private static final String TAG = "SplashActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        applyTopInset = false;
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        cardLogo = findViewById(R.id.card_logo);
        ivLogo = findViewById(R.id.iv_logo);
        tvAppName = findViewById(R.id.tv_app_name);
        tvTagline = findViewById(R.id.tv_tagline);
        loader = findViewById(R.id.progress);

        // State awal diset langsung agar tidak ada kedipan tampilan sebelum animasi
        cardLogo.setScaleX(0f);
        cardLogo.setScaleY(0f);
        cardLogo.setAlpha(0f);
        tvAppName.setAlpha(0f);
        tvAppName.setTranslationY(50f);
        tvTagline.setAlpha(0f);
        tvTagline.setTranslationY(30f);

        Glide.with(this).load(R.drawable.loader).into(loader);

        // Eksekusi animasi setelah layout pass pertama selesai agar tidak skip frame
        cardLogo.post(() -> {
            if (isFinishing() || (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1 && isDestroyed())) {
                return;
            }
            startAnimations();
        });

        // Track session di background thread agar disk I/O tidak membebani main thread
        new Thread(() -> ReviewHelper.trackSession(SplashActivity.this)).start();

        // Tunda inisialisasi network/ads sedikit agar UI thread fokus render animasi awal
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            if (isFinishing() || (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1 && isDestroyed())) {
                return;
            }
            initAdsAndFlow();
        }, 150);
    }

    private void initAdsAndFlow() {
        Ads.CheckAds(this);
        if (ON_OFF_ADS.equals("1")) {
            if (checkConnectivity()) {
                loadUrlData();
            } else {
                nointernetp();
            }
        } else {
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                navigateToNextScreen();
            }, SPLASH_DELAY);
        }
    }

    private void startAnimations() {
        // Aktifkan hardware layer agar rendering shadow CardView halus di GPU
        cardLogo.setLayerType(View.LAYER_TYPE_HARDWARE, null);

        // Skala logo dengan kurva pantulan natural dari OvershootInterpolator
        ObjectAnimator scaleX = ObjectAnimator.ofFloat(cardLogo, "scaleX", 0f, 1f);
        ObjectAnimator scaleY = ObjectAnimator.ofFloat(cardLogo, "scaleY", 0f, 1f);
        ObjectAnimator alpha = ObjectAnimator.ofFloat(cardLogo, "alpha", 0f, 1f);

        AnimatorSet logoSet = new AnimatorSet();
        logoSet.playTogether(scaleX, scaleY, alpha);
        logoSet.setDuration(600);
        logoSet.setInterpolator(new OvershootInterpolator(1.2f));

        logoSet.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                cardLogo.setLayerType(View.LAYER_TYPE_NONE, null);
            }
        });

        // Rotasi logo
        ObjectAnimator rotation = ObjectAnimator.ofFloat(ivLogo, "rotation", 0f, 360f);
        rotation.setDuration(800);
        rotation.setStartDelay(100);
        rotation.setInterpolator(new AccelerateDecelerateInterpolator());

        // Animasi teks nama app
        tvAppName.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(500)
                .setStartDelay(350)
                .setInterpolator(new AccelerateDecelerateInterpolator())
                .start();

        // Animasi teks tagline
        tvTagline.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(500)
                .setStartDelay(550)
                .setInterpolator(new AccelerateDecelerateInterpolator())
                .start();

        // Mulai animasi logo
        logoSet.start();
        rotation.start();
    }

    private void navigateToNextScreen() {
        // Fade out animation before navigation
        View rootView = findViewById(android.R.id.content);
        rootView.animate()
                .alpha(0f)
                .setDuration(600)
                .setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        SharedPrefManager prefManager = SharedPrefManager.getInstance(SplashActivity.this);

                        // Check if onboarding completed
                        android.content.SharedPreferences prefs = getSharedPreferences("app_prefs", MODE_PRIVATE);
                        boolean onboardingCompleted = prefs.getBoolean("onboarding_completed", false);

                        Intent intent;
                        if (prefManager.isLoggedIn()) {
                            // User is already logged in, go straight to Home
                            intent = new Intent(SplashActivity.this, MainActivity.class);
                        } else if (!onboardingCompleted) {
                            // First time user who hasn't seen onboarding - show Language selection
                            intent = new Intent(SplashActivity.this, LanguageActivity.class);
                        } else {
                            // User finished onboarding but is not logged in
                            intent = new Intent(SplashActivity.this, LanguageActivity.class);
                        }

                        startActivity(intent);
                        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
                        finish();
                    }
                })
                .start();
    }

    private void nointernetp() {
        // Modern BottomSheetDialog — lebih clean dari AlertDialog
        BottomSheetDialog bottomSheet = new BottomSheetDialog(this, com.google.android.material.R.style.Theme_Material3_Light_BottomSheetDialog);

        View view = LayoutInflater.from(this).inflate(R.layout.dialog_no_internet, null);
        bottomSheet.setContentView(view);
        bottomSheet.setCancelable(false); // User harus pilih salah satu opsi

        MaterialButton btnRetry = view.findViewById(R.id.btn_retry);
        MaterialButton btnExit  = view.findViewById(R.id.btn_exit);

        btnRetry.setOnClickListener(v -> {
            bottomSheet.dismiss();
            // Cek ulang koneksi lalu jalankan ulang logic splash
            if (checkConnectivity()) {
                loadUrlData();
            } else {
                nointernetp();
            }
        });

        btnExit.setOnClickListener(v -> {
            bottomSheet.dismiss();
            finish();
        });

        if (!isFinishing() && !isDestroyed()) {
            bottomSheet.show();
        }
    }

    private boolean checkConnectivity() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm == null) return false;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // API 23+ — gunakan NetworkCapabilities (tidak deprecated)
            Network activeNetwork = cm.getActiveNetwork();
            if (activeNetwork == null) return false;
            NetworkCapabilities caps = cm.getNetworkCapabilities(activeNetwork);
            return caps != null
                    && caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                    && caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
                    && (caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
                        || caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)
                        || caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET));
        } else {
            // API < 23 — fallback (deprecated tapi masih aman untuk versi lama)
            android.net.NetworkInfo info = cm.getActiveNetworkInfo();
            return info != null && info.isConnected();
        }
    }

    private void loadUrlData() {
        StringRequest stringRequest = new StringRequest(Request.Method.GET,
                SettingAds.URL_DATA, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                try {
                    JSONObject jsonObj = new JSONObject(response);
                    JSONArray contacts = jsonObj.getJSONArray("Ads");
                    for (int i = 0; i < contacts.length(); i++) {
                        JSONObject c = contacts.getJSONObject(i);

                        // Waterfall priority
                        SettingAds.WATERFALL_PRIORITY = c.optString("waterfall_priority", "applovin");

                        // Ad Unit IDs (new format) — jangan overwrite dengan string kosong dari backend
                        String applovinB = c.optString("applovin_banner");
                        if (!applovinB.trim().isEmpty()) SettingAds.APPLOVIN_BANNER = applovinB.trim();

                        String applovinI = c.optString("applovin_intertitial");
                        if (!applovinI.trim().isEmpty()) SettingAds.APPLOVIN_INTERSTITIAL = applovinI.trim();

                        String applovinR = c.optString("applovin_rewards");
                        if (!applovinR.trim().isEmpty()) SettingAds.APPLOVIN_REWARDS = applovinR.trim();

                        String admobB = c.optString("admob_banner");
                        if (!admobB.trim().isEmpty()) SettingAds.ADMOB_BANNER = admobB.trim();

                        String admobI = c.optString("admob_intertitial");
                        if (!admobI.trim().isEmpty()) SettingAds.ADMOB_INTERSTITIAL = admobI.trim();

                        String admobR = c.optString("admob_rewards");
                        if (!admobR.trim().isEmpty()) SettingAds.ADMOB_REWARDS = admobR.trim();

                        // Advanced settings
                        SettingAds.INTERVAL_CAPS = c.optInt("interval_caps", 0);
                        SettingAds.CHILD_DIRECT_GDPR = c.optBoolean("child_gdpr", false);
                        SettingAds.STATUS_APP = c.optString("status_app", "0");
                        SettingAds.SUSPENSION_MESSAGE = c.optString("suspension_message", SettingAds.SUSPENSION_MESSAGE);
                        SettingAds.LINK_REDIRECT = c.optString("link_redirect", SettingAds.LINK_REDIRECT);

                        // Update mode (off/flexible/immediate)
                        SettingAds.UPDATE_MODE = c.optString("update_mode", SettingAds.UPDATE_MODE_FLEXIBLE);

                        // Parse placement matrix
                        JSONObject placements = c.optJSONObject("placements");
                        if (placements != null) {
                            java.util.Iterator<String> keys = placements.keys();
                            while (keys.hasNext()) {
                                String key = keys.next();
                                JSONObject p = placements.optJSONObject(key);
                                if (p == null) continue;
                                boolean applovin = "1".equals(p.optString("applovin", "0"));
                                boolean admob = "1".equals(p.optString("admob", "0"));
                                SettingAds.setPlacement(key, applovin, admob);
                            }
                            Log.d(TAG, "Placement matrix loaded: " + placements.length() + " placements");
                        }

                        // --- Sync deprecated SELECT_MAIN_ADS + unit IDs dari placement matrix ---
                        // Interstitial.java masih pakai SELECT_MAIN_ADS, bukan placement-aware.
                        // Kita derive dari interstitial_main_select + waterfall_priority.
                        boolean mainApplovin = SettingAds.isPlacementEnabled(
                                SettingAds.PLACEMENT_INTERSTITIAL_MAIN_SELECT, SettingAds.NETWORK_APPLOVIN);
                        boolean mainAdmob = SettingAds.isPlacementEnabled(
                                SettingAds.PLACEMENT_INTERSTITIAL_MAIN_SELECT, SettingAds.NETWORK_ADMOB);

                        if (mainApplovin && mainAdmob) {
                            SettingAds.SELECT_MAIN_ADS = "admob".equals(SettingAds.WATERFALL_PRIORITY)
                                    ? "ADMOB_APPLOVIN" : "APPLOVIN_ADMOB";
                        } else if (mainAdmob) {
                            SettingAds.SELECT_MAIN_ADS = "ADMOB";
                        } else {
                            SettingAds.SELECT_MAIN_ADS = "APPLOVIN"; // default/fallback
                        }

                        // Sync interstitial/reward unit IDs —
                        // MAIN_ADS_INTERTITIAL must match the PRIMARY network from SELECT_MAIN_ADS.
                        // Interstitial.java uses MAIN_ADS_INTERTITIAL as the primary ID for ALL modes.
                        if ("ADMOB".equals(SettingAds.SELECT_MAIN_ADS) || "ADMOB_APPLOVIN".equals(SettingAds.SELECT_MAIN_ADS)) {
                            SettingAds.MAIN_ADS_INTERTITIAL = SettingAds.ADMOB_INTERSTITIAL;
                            SettingAds.MAIN_ADS_INTERTITIAL_BACKUP = SettingAds.APPLOVIN_INTERSTITIAL;
                        } else {
                            SettingAds.MAIN_ADS_INTERTITIAL = SettingAds.APPLOVIN_INTERSTITIAL;
                            SettingAds.MAIN_ADS_INTERTITIAL_BACKUP = SettingAds.ADMOB_INTERSTITIAL;
                        }

                        if ("ADMOB".equals(SettingAds.SELECT_MAIN_ADS) || "ADMOB_APPLOVIN".equals(SettingAds.SELECT_MAIN_ADS)) {
                            SettingAds.MAIN_ADS_REWARDS = SettingAds.ADMOB_REWARDS;
                            SettingAds.MAIN_ADS_REWARDS_BACKUP = SettingAds.APPLOVIN_REWARDS;
                        } else {
                            SettingAds.MAIN_ADS_REWARDS = SettingAds.APPLOVIN_REWARDS;
                            SettingAds.MAIN_ADS_REWARDS_BACKUP = SettingAds.ADMOB_REWARDS;
                        }

                        Log.d(TAG, "SELECT_MAIN_ADS synced: " + SettingAds.SELECT_MAIN_ADS
                                + " (applovin=" + mainApplovin + ", admob=" + mainAdmob + ")");

                        // Cek apakah app di-suspend (kill switch aktif)
                        if ("1".equals(SettingAds.STATUS_APP)) {
                            showAppSuspendedDialog();
                            return;
                        }

                        // Cek update Play Store dulu sebelum lanjut, sesuai mode dari admin
                        com.beeflirt.beetalk.utils.AppUpdateHelper.checkAndPromptUpdate(
                                SplashActivity.this,
                                SettingAds.UPDATE_MODE,
                                () -> new Handler(Looper.getMainLooper()).postDelayed(
                                        () -> navigateToNextScreen(),
                                        SPLASH_DELAY
                                )
                        );

                        Log.d(TAG, "Ad config loaded: priority=" + SettingAds.WATERFALL_PRIORITY +
                                ", interval_caps=" + SettingAds.INTERVAL_CAPS +
                                ", status_app=" + SettingAds.STATUS_APP +
                                ", update_mode=" + SettingAds.UPDATE_MODE);
                    }
                } catch (JSONException e) {
                    new Handler(Looper.getMainLooper()).postDelayed(() -> {
                        navigateToNextScreen();
                    }, SPLASH_DELAY);
                    e.printStackTrace();
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                new Handler(Looper.getMainLooper()).postDelayed(() -> {
                    navigateToNextScreen();
                }, SPLASH_DELAY);
                Toast.makeText(SplashActivity.this, "Error" + error.toString(), Toast.LENGTH_SHORT).show();
                Log.d(TAG, "onErrorResponse: " + error.toString());

            }
        });

        RequestQueue requestQueue = Volley.newRequestQueue(SplashActivity.this);
        requestQueue.add(stringRequest);
    }

    private void showAppSuspendedDialog() {
        BottomSheetDialog bottomSheet = new BottomSheetDialog(this, com.google.android.material.R.style.Theme_Material3_Light_BottomSheetDialog);

        View view = LayoutInflater.from(this).inflate(R.layout.dialog_app_suspended, null);
        bottomSheet.setContentView(view);
        bottomSheet.setCancelable(false);

        TextView tvMessage = view.findViewById(R.id.tv_suspended_message);
        tvMessage.setText(SettingAds.SUSPENSION_MESSAGE);

        MaterialButton btnOpenNewApp = view.findViewById(R.id.btn_open_new_app);
        btnOpenNewApp.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(SettingAds.LINK_REDIRECT));
            startActivity(intent);
            finish();
        });

        if (!isFinishing() && !isDestroyed()) {
            bottomSheet.show();
        }
    }


}
