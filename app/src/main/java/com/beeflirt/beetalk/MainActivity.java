package com.beeflirt.beetalk;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.beeflirt.beetalk.activities.LoginActivity;
import com.beeflirt.beetalk.activities.BaseActivity;
import com.beeflirt.beetalk.activities.ProfileSetupActivity;
import com.beeflirt.beetalk.api.ApiClient;
import com.beeflirt.beetalk.api.ApiService;
import com.beeflirt.beetalk.fragments.ExploreFragment;
import com.beeflirt.beetalk.fragments.MatchesFragment;
import com.beeflirt.beetalk.fragments.MessagesFragment;
import com.beeflirt.beetalk.fragments.ProfileFragment;
import com.beeflirt.beetalk.fragments.SwipeFragment;
import com.beeflirt.beetalk.models.ApiResponse;
import com.beeflirt.beetalk.models.User;
import com.beeflirt.beetalk.utils.SharedPrefManager;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationBarView;

import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends BaseActivity {

    private static final String TAG = "MainActivity";

    private BottomNavigationView bottomNavigationView;
    private FragmentManager fragmentManager;
    private SharedPrefManager prefManager;

    // Banners
    private LinearLayout bannerVerifyEmail;
    private LinearLayout bannerCompleteProfile;

    // Fragments
    private SwipeFragment swipeFragment;
    private ExploreFragment exploreFragment;
    private MatchesFragment matchesFragment;
    private MessagesFragment messagesFragment;
    private ProfileFragment profileFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Check if user is logged in
        prefManager = SharedPrefManager.getInstance(this);
        if (!prefManager.isLoggedIn()) {
            navigateToLogin();
            return;
        }

        // Initialize views
        bottomNavigationView = findViewById(R.id.bottom_navigation);
        bannerVerifyEmail = findViewById(R.id.banner_verify_email);
        bannerCompleteProfile = findViewById(R.id.banner_complete_profile);
        fragmentManager = getSupportFragmentManager();

        // Setup banner actions
        setupBanners();

        // Initialize fragments
        swipeFragment = new SwipeFragment();
        exploreFragment = new ExploreFragment();
        matchesFragment = new MatchesFragment();
        messagesFragment = new MessagesFragment();
        profileFragment = new ProfileFragment();

        // Set default fragment
        loadFragment(swipeFragment);

        // Check user status (email verification + profile completion)
        checkUserStatus();

        // Register FCM token
        registerFCMToken();

        // Bottom navigation listener
        bottomNavigationView.setOnItemSelectedListener(new NavigationBarView.OnItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                int itemId = item.getItemId();

                if (itemId == R.id.nav_swipe) {
                    loadFragment(swipeFragment);
                    return true;
                } else if (itemId == R.id.nav_explore) {
                    loadFragment(exploreFragment);
                    return true;
                } else if (itemId == R.id.nav_matches) {
                    loadFragment(matchesFragment);
                    return true;
                } else if (itemId == R.id.nav_messages) {
                    loadFragment(messagesFragment);
                    return true;
                } else if (itemId == R.id.nav_profile) {
                    loadFragment(profileFragment);
                    return true;
                }

                return false;
            }
        });

        // Sync bottom nav highlight with default fragment
        bottomNavigationView.setSelectedItemId(R.id.nav_swipe);
    }

    private void setupBanners() {
        // Verify email banner - "Verify" button -> go to EditProfile
        TextView btnVerify = findViewById(R.id.btn_banner_verify);
        if (btnVerify != null) {
            btnVerify.setOnClickListener(v -> {
                Intent intent = new Intent(MainActivity.this,
                        com.beeflirt.beetalk.activities.EditProfileActivity.class);
                startActivity(intent);
            });
        }

        // Verify email banner - dismiss button
        View btnDismiss = findViewById(R.id.btn_banner_dismiss);
        if (btnDismiss != null) {
            btnDismiss.setOnClickListener(v -> {
                bannerVerifyEmail.animate()
                        .alpha(0f)
                        .setDuration(200)
                        .withEndAction(() -> bannerVerifyEmail.setVisibility(View.GONE))
                        .start();
            });
        }

        // Complete profile banner - "Complete" button
        TextView btnComplete = findViewById(R.id.btn_banner_complete);
        if (btnComplete != null) {
            btnComplete.setOnClickListener(v -> {
                Intent intent = new Intent(MainActivity.this, ProfileSetupActivity.class);
                startActivity(intent);
            });
        }
    }

    /**
     * Cek status user: email verified? profile complete?
     * Tampilkan banner yang sesuai.
     */
    private void checkUserStatus() {
        ApiService apiService = ApiClient.getApiService(this);

        // Cek via /auth/me endpoint
        apiService.getCurrentUser().enqueue(new Callback<ApiResponse<User>>() {
            @Override
            public void onResponse(Call<ApiResponse<User>> call,
                    Response<ApiResponse<User>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isStatus()) {
                    User user = response.body().getData();
                    if (user != null && !user.isVerified()) {
                        showBanner(bannerVerifyEmail);
                    } else if (bannerVerifyEmail != null) {
                        bannerVerifyEmail.setVisibility(View.GONE);
                    }
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<User>> call, Throwable t) {
                Log.w(TAG, "Failed to check user status: " + t.getMessage());
            }
        });

        // Cek profile completion
        apiService.getProfileCompletionStatus().enqueue(new Callback<ApiResponse<Map<String, Object>>>() {
            @Override
            public void onResponse(Call<ApiResponse<Map<String, Object>>> call,
                    Response<ApiResponse<Map<String, Object>>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isStatus()) {
                    Map<String, Object> data = response.body().getData();

                    Object isCompleteObj = data.get("is_complete");
                    boolean isComplete = false;
                    if (isCompleteObj instanceof Boolean) {
                        isComplete = (Boolean) isCompleteObj;
                    }

                    if (!isComplete) {
                        showBanner(bannerCompleteProfile);
                    }
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<Map<String, Object>>> call, Throwable t) {
                Log.w(TAG, "Failed to check profile completion: " + t.getMessage());
            }
        });
    }

    private void showBanner(LinearLayout banner) {
        if (banner != null && banner.getVisibility() != View.VISIBLE) {
            banner.setVisibility(View.VISIBLE);
            banner.setAlpha(0f);
            banner.setTranslationY(-banner.getHeight());
            banner.animate()
                    .alpha(1f)
                    .translationY(0f)
                    .setDuration(300)
                    .start();
        }
    }

    private void loadFragment(Fragment fragment) {
        FragmentTransaction transaction = fragmentManager.beginTransaction();
        transaction.replace(R.id.fragment_container, fragment);
        transaction.commit();
    }

    private void navigateToLogin() {
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    public void switchToSwipeTab() {
        bottomNavigationView.setSelectedItemId(R.id.nav_swipe);
    }

    private void registerFCMToken() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(
                    android.Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[] { android.Manifest.permission.POST_NOTIFICATIONS }, 100);
            }
        }

        com.google.firebase.messaging.FirebaseMessaging.getInstance().getToken()
                .addOnCompleteListener(task -> {
                    if (!task.isSuccessful()) {
                        Log.w(TAG, "Fetching FCM registration token failed", task.getException());
                        return;
                    }

                    String token = task.getResult();
                    Log.d(TAG, "FCM Token: " + token);
                    sendTokenToServer(token);
                });
    }

    private void sendTokenToServer(String token) {
        ApiService apiService = ApiClient.getApiService(this);

        com.google.gson.JsonObject requestBody = new com.google.gson.JsonObject();
        requestBody.addProperty("device_token", token);
        requestBody.addProperty("device_type", "android");

        apiService.registerDeviceToken(requestBody).enqueue(new retrofit2.Callback<Object>() {
            @Override
            public void onResponse(retrofit2.Call<Object> call, retrofit2.Response<Object> response) {
                if (response.isSuccessful()) {
                    Log.d(TAG, "FCM token registered successfully");
                } else {
                    Log.d(TAG, "Failed to register FCM token: " + response.code());
                }
            }

            @Override
            public void onFailure(retrofit2.Call<Object> call, Throwable t) {
                Log.d(TAG, "Error registering FCM token: " + t.getMessage());
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Re-check email verification status saat kembali dari EditProfile
        checkUserStatus();
    }

    @Override
    public void onBackPressed() {
        if (bottomNavigationView.getSelectedItemId() != R.id.nav_swipe) {
            bottomNavigationView.setSelectedItemId(R.id.nav_swipe);
        } else {
            super.onBackPressed();
        }
    }
}
