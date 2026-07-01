package com.beeflirt.beetalk.activities;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;

import com.beeflirt.beetalk.R;
import com.beeflirt.beetalk.adapters.SetupPagerAdapter;
import com.beeflirt.beetalk.api.ApiClient;
import com.beeflirt.beetalk.api.ApiService;
import com.beeflirt.beetalk.dialogs.LoadingDialog;
import com.beeflirt.beetalk.models.ApiResponse;
import com.google.gson.JsonObject;

import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import com.beeflirt.beetalk.MainActivity;



public class ProfileSetupActivity extends BaseActivity {

    private ViewPager2 viewPager;
    private Button btnBack, btnNext;
    private LoadingDialog loadingDialog;
    private View indicator1, indicator2, indicator3;

    private SetupPagerAdapter pagerAdapter;
    private ApiService apiService;
    private int currentPage = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile_setup);

        viewPager = findViewById(R.id.view_pager);
        btnBack   = findViewById(R.id.btn_back);
        btnNext   = findViewById(R.id.btn_next);
        loadingDialog = new LoadingDialog(this);
        indicator1 = findViewById(R.id.indicator_1);
        indicator2 = findViewById(R.id.indicator_2);
        indicator3 = findViewById(R.id.indicator_3);

        apiService = ApiClient.getApiService(this);

        // Setup ViewPager
        pagerAdapter = new SetupPagerAdapter(this);
        viewPager.setAdapter(pagerAdapter);
        viewPager.setUserInputEnabled(false); // Disable swipe

        // Setup photo count listener
        pagerAdapter.getPhotosFragment().setOnPhotoCountChangeListener(count -> {
            if (currentPage == 0) btnNext.setEnabled(count >= 1);
        });

        // Listen for location status changes from GeoFragment so we can
        // immediately reflect the status in the Finish button appearance.
        pagerAdapter.getGeoFragment().setOnLocationStatusChangedListener(enabled -> {
            // Only update button when on the geo page (page 2)
            if (currentPage == 2) {
                updateFinishButtonState(enabled);
            }
        });

        // ViewPager page change listener
        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                currentPage = position;
                updateUI();
            }
        });

        // Button listeners
        btnBack.setOnClickListener(v -> {
            if (currentPage > 0) {
                viewPager.setCurrentItem(currentPage - 1);
            }
        });

        btnNext.setOnClickListener(v -> {
            if (currentPage < 2) {
                viewPager.setCurrentItem(currentPage + 1);
            } else {
                finishSetup();
            }
        });

        updateUI();

        // Validate user session
        validateUserSession();
    }

    private void validateUserSession() {
        // Check if user is properly registered by fetching profile
        apiService.getProfile().enqueue(new Callback<ApiResponse<com.beeflirt.beetalk.models.Profile>>() {
            @Override
            public void onResponse(Call<ApiResponse<com.beeflirt.beetalk.models.Profile>> call,
                    Response<ApiResponse<com.beeflirt.beetalk.models.Profile>> response) {
                if (!response.isSuccessful() || response.body() == null || !response.body().isStatus()) {
                    // User not properly registered, redirect to login
                    redirectToLogin("Session invalid. Please login again.");
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<com.beeflirt.beetalk.models.Profile>> call, Throwable t) {
                // Network error or invalid session, redirect to login
                redirectToLogin("Unable to verify session. Please login again.");
            }
        });
    }

    private void redirectToLogin(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
        com.beeflirt.beetalk.utils.SharedPrefManager.getInstance(this).clearSession();
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void updateUI() {
        // Update back button
        btnBack.setVisibility(currentPage > 0 ? View.VISIBLE : View.GONE);

        if (currentPage == 2) {
            // ── GEO PAGE: Finish button reflects location status ──────────
            btnNext.setText("Finish");
            boolean locationOk = pagerAdapter.getGeoFragment().isLocationEnabled();
            updateFinishButtonState(locationOk);
        } else {
            // ── OTHER PAGES: Reset alpha and handle normal Next logic ──────
            btnNext.setText("Next");
            btnNext.setAlpha(1.0f); // Reset transparency for other pages
            
            if (currentPage == 0) {
                int photoCount = pagerAdapter.getPhotosFragment().getPhotoCount();
                btnNext.setEnabled(photoCount >= 1);
            } else {
                btnNext.setEnabled(true);
            }
        }

        // Update step indicators
        indicator1.setAlpha(currentPage == 0 ? 1.0f : 0.3f);
        indicator2.setAlpha(currentPage == 1 ? 1.0f : 0.3f);
        indicator3.setAlpha(currentPage == 2 ? 1.0f : 0.3f);
    }

    /**
     * Updates the visual state of the Finish button based on location permission.
     * The button is always clickable; when location is missing we show a warning dialog.
     */
    private void updateFinishButtonState(boolean locationGranted) {
        if (locationGranted) {
            btnNext.setEnabled(true);
            btnNext.setAlpha(1.0f);
        } else {
            // Keep enabled so user can tap and see WHY they can't proceed.
            btnNext.setEnabled(true);
            btnNext.setAlpha(0.55f); // Visually dimmed to hint it's blocked
        }
    }

    private void finishSetup() {
        // ── LOCATION GUARD ─────────────────────────────────────────────────
        if (!pagerAdapter.getGeoFragment().isLocationEnabled()) {
            showLocationRequiredDialog();
            return;
        }
        // ───────────────────────────────────────────────────────────────────

        loadingDialog.show("Finishing Setup…", "Saving your profile");
        btnNext.setEnabled(false);

        // Save location
        saveLocation(
                pagerAdapter.getGeoFragment().getLatitude(),
                pagerAdapter.getGeoFragment().getLongitude());

        // Save preferences
        savePreferences();
    }

    /**
     * Modern AlertDialog shown when the user taps Finish without enabling location.
     */
    private void showLocationRequiredDialog() {
        android.app.Dialog dialog = new android.app.Dialog(this);
        dialog.requestWindowFeature(android.view.Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_location_modern);
        
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));
            dialog.getWindow().setLayout(android.view.ViewGroup.LayoutParams.WRAP_CONTENT, android.view.ViewGroup.LayoutParams.WRAP_CONTENT);
        }

        View btnEnable = dialog.findViewById(R.id.btn_enable_location);
        View btnNotNow = dialog.findViewById(R.id.btn_not_now);

        btnEnable.setOnClickListener(v -> {
            dialog.dismiss();
            pagerAdapter.getGeoFragment().requestLocationPermissionPublic();
        });

        btnNotNow.setOnClickListener(v -> dialog.dismiss());

        dialog.setCancelable(true);
        dialog.show();
    }

    private void saveLocation(double latitude, double longitude) {
        JsonObject data = new JsonObject();
        data.addProperty("latitude", latitude);
        data.addProperty("longitude", longitude);

        apiService.updateProfile(data).enqueue(new Callback<ApiResponse<com.beeflirt.beetalk.models.Profile>>() {
            @Override
            public void onResponse(Call<ApiResponse<com.beeflirt.beetalk.models.Profile>> call,
                    Response<ApiResponse<com.beeflirt.beetalk.models.Profile>> response) {
                if (!response.isSuccessful()) {
                    Log.w("ProfileSetup", "Failed to save location: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<com.beeflirt.beetalk.models.Profile>> call, Throwable t) {
                Log.w("ProfileSetup", "Location save error: " + t.getMessage());
            }
        });
    }

    private void saveBio(String bio) {
        JsonObject data = new JsonObject();
        data.addProperty("bio", bio);

        apiService.updateProfile(data).enqueue(new Callback<ApiResponse<com.beeflirt.beetalk.models.Profile>>() {
            @Override
            public void onResponse(Call<ApiResponse<com.beeflirt.beetalk.models.Profile>> call,
                    Response<ApiResponse<com.beeflirt.beetalk.models.Profile>> response) {
                // Bio saved, continue
            }

            @Override
            public void onFailure(Call<ApiResponse<com.beeflirt.beetalk.models.Profile>> call, Throwable t) {
                // Ignore, bio is optional
            }
        });
    }

    private void savePreferences() {
        JsonObject data = new JsonObject();
        data.addProperty("interested_in", pagerAdapter.getPreferencesFragment().getInterestedIn());
        data.addProperty("min_age",        pagerAdapter.getPreferencesFragment().getMinAge());
        data.addProperty("max_age",        pagerAdapter.getPreferencesFragment().getMaxAge());
        data.addProperty("max_distance",   pagerAdapter.getPreferencesFragment().getMaxDistance());

        apiService.updatePreferences(data).enqueue(new Callback<ApiResponse<java.util.Map<String, Object>>>() {
            @Override
            public void onResponse(Call<ApiResponse<java.util.Map<String, Object>>> call,
                    Response<ApiResponse<java.util.Map<String, Object>>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isStatus()) {
                    populateLocationData();
                } else {
                    loadingDialog.dismiss();
                    btnNext.setEnabled(true);
                    Toast.makeText(ProfileSetupActivity.this, "Failed to save preferences. Please try again.", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<java.util.Map<String, Object>>> call, Throwable t) {
                loadingDialog.dismiss();
                btnNext.setEnabled(true);
                Toast.makeText(ProfileSetupActivity.this, "Connection error. Please try again.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void navigateToMain() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    @Override
    public void onBackPressed() {
        if (currentPage > 0) {
            viewPager.setCurrentItem(currentPage - 1);
        } else {
            // Show confirmation dialog
            new androidx.appcompat.app.AlertDialog.Builder(this)
                    .setTitle("Exit Setup?")
                    .setMessage("You need to complete profile setup to use the app. Do you want to logout?")
                    .setPositiveButton("Logout", (dialog, which) -> {
                        com.beeflirt.beetalk.utils.SharedPrefManager.getInstance(this).clearSession();
                        Intent intent = new Intent(this, LoginActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                        finish();
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        }
    }

    private void populateLocationData() {
        apiService.populateLocation().enqueue(new Callback<ApiResponse<Map<String, String>>>() {
            @Override
            public void onResponse(Call<ApiResponse<Map<String, String>>> call,
                                   Response<ApiResponse<Map<String, String>>> response) {
                loadingDialog.dismiss();

                if (response.isSuccessful() && response.body() != null && response.body().isStatus()) {
                    Log.d("ProfileSetup", "City/Country populated: " + response.body().getData());
                }

                Toast.makeText(ProfileSetupActivity.this, "Profile setup complete!", Toast.LENGTH_SHORT).show();
                navigateToMain();
            }

            @Override
            public void onFailure(Call<ApiResponse<Map<String, String>>> call, Throwable t) {
                loadingDialog.dismiss();
                Toast.makeText(ProfileSetupActivity.this, "Profile setup complete!", Toast.LENGTH_SHORT).show();
                navigateToMain();
            }
        });
    }
}
