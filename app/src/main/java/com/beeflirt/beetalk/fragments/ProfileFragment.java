package com.beeflirt.beetalk.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;

import com.beeflirt.beetalk.R;
import com.beeflirt.beetalk.activities.BlockedUsersActivity;
import com.beeflirt.beetalk.activities.LoginActivity;
import com.beeflirt.beetalk.adsconfig.Ads;
import com.beeflirt.beetalk.api.ApiClient;
import com.beeflirt.beetalk.api.ApiService;
import com.beeflirt.beetalk.models.ApiResponse;
import com.beeflirt.beetalk.repository.ProfileRepository;
import com.beeflirt.beetalk.repository.RepositoryCallback;
import com.beeflirt.beetalk.utils.Constants;
import com.beeflirt.beetalk.utils.SharedPrefManager;
import com.bumptech.glide.Glide;

import java.util.Map;

import de.hdodenhof.circleimageview.CircleImageView;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ProfileFragment extends Fragment {

    private CircleImageView ivProfilePhoto;
    private TextView tvName, tvAge, tvUsername, tvLocation, tvBio;
    private TextView tvHobbies, tvDreams, tvCompletionText;
    private ProgressBar profileCompletionBar;
    private CardView btnGallery, btnEditProfile, btnPreferences, btnAccountSettings, btnBlockedUsers;
    private CardView btnWhoLiked, btnPremium;
    private CardView btnLogout;
    private ProgressBar progressBar;
    private TextView tvPremiumLabel;

    private SharedPrefManager prefManager;
    private ApiService apiService;
    private ProfileRepository profileRepository;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        // Initialize
        prefManager = SharedPrefManager.getInstance(requireContext());
        apiService = ApiClient.getApiService(requireContext());
        profileRepository = new ProfileRepository(requireContext());

        // Initialize views
        ivProfilePhoto = view.findViewById(R.id.iv_profile_photo);
        tvName = view.findViewById(R.id.tv_name);
        tvAge = view.findViewById(R.id.tv_age);
        tvUsername = view.findViewById(R.id.tv_username);
        tvLocation = view.findViewById(R.id.tv_location);
        tvBio = view.findViewById(R.id.tv_bio);
        tvHobbies = view.findViewById(R.id.tv_hobbies);
        tvDreams = view.findViewById(R.id.tv_dreams);
        tvCompletionText = view.findViewById(R.id.tv_completion_text);
        profileCompletionBar = view.findViewById(R.id.profile_completion_bar);
        btnGallery = view.findViewById(R.id.btn_gallery);
        btnEditProfile = view.findViewById(R.id.btn_edit_profile);
        btnPreferences = view.findViewById(R.id.btn_preferences);
        btnAccountSettings = view.findViewById(R.id.btn_account_settings);
        btnBlockedUsers = view.findViewById(R.id.btn_blocked_users);
        btnWhoLiked = view.findViewById(R.id.btn_who_liked);
        btnPremium = view.findViewById(R.id.btn_premium);
        tvPremiumLabel = view.findViewById(R.id.tv_premium_label);
        btnLogout = view.findViewById(R.id.btn_logout);
        progressBar = view.findViewById(R.id.progress_bar);

        // Button listeners
        btnGallery.setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), com.beeflirt.beetalk.activities.GalleryActivity.class);
            startActivity(intent);
        });
        if (btnWhoLiked != null) {
            btnWhoLiked.setOnClickListener(v ->
                    startActivity(new Intent(requireContext(), com.beeflirt.beetalk.activities.WhoLikedMeActivity.class)));
        }
        if (btnPremium != null) {
            btnPremium.setVisibility(com.beeflirt.beetalk.utils.PremiumManager.getInstance(requireContext()).isPremiumFeatureEnabled() ? View.VISIBLE : View.GONE);
            btnPremium.setOnClickListener(v ->
                    startActivity(new Intent(requireContext(), com.beeflirt.beetalk.activities.PremiumActivity.class)));
        }
        btnEditProfile.setOnClickListener(v -> openEditProfile());
        btnPreferences.setOnClickListener(v -> openPreferences());
        btnAccountSettings.setOnClickListener(v -> openAccountSettings());
        btnBlockedUsers.setOnClickListener(v -> openBlockedUsers());
        btnLogout.setOnClickListener(v -> logout());

        // Load profile
        loadProfile();
        loadEntitlements();

        // Initialize and load banner ad
        RelativeLayout bannerContainer = view.findViewById(R.id.banner_container);
        Ads.ShowBanner(getActivity(), bannerContainer, com.beeflirt.beetalk.adsconfig.SettingAds.PLACEMENT_BANNER_PROFILE);

        return view;
    }

    private void loadProfile() {
        progressBar.setVisibility(View.VISIBLE);

        int userId = prefManager.getUserId();
        profileRepository.getProfile(userId, new RepositoryCallback<com.beeflirt.beetalk.models.Profile>() {
            @Override
            public void onSuccess(com.beeflirt.beetalk.models.Profile profile) {
                if (!isAdded()) return;
                progressBar.setVisibility(View.GONE);
                displayProfile(profile);
            }

            @Override
            public void onError(String message) {
                if (!isAdded()) return;
                progressBar.setVisibility(View.GONE);
                Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
            }
        });

        // Load profile completion status
        loadProfileCompletion();
    }

    private void loadProfileCompletion() {
        apiService.getProfileCompletionStatus().enqueue(new Callback<ApiResponse<Map<String, Object>>>() {
            @Override
            public void onResponse(Call<ApiResponse<Map<String, Object>>> call,
                    Response<ApiResponse<Map<String, Object>>> response) {
                if (!isAdded()) return;

                if (response.isSuccessful() && response.body() != null && response.body().isStatus()) {
                    Map<String, Object> data = response.body().getData();
                    if (data != null) {
                        int percent = 0;
                        Object pctObj = data.get("completion_percentage");
                        if (pctObj instanceof Number) {
                            percent = ((Number) pctObj).intValue();
                        }

                        if (profileCompletionBar != null) {
                            profileCompletionBar.setProgress(percent);
                        }
                        if (tvCompletionText != null) {
                            tvCompletionText.setText("Profile " + percent + "% complete");
                        }
                    }
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<Map<String, Object>>> call, Throwable t) {
                if (!isAdded()) return;
                Log.e("ProfileFragment", "Error loading completion status", t);
            }
        });
    }

    private void displayProfile(com.beeflirt.beetalk.models.Profile profile) {
        if (profile == null || !isAdded())
            return;

        // Display name
        if (profile.getName() != null) {
            tvName.setText(profile.getName());
        }

        // Display age
        if (profile.getAge() > 0) {
            tvAge.setText(profile.getAge() + " years old");
            tvAge.setVisibility(View.VISIBLE);
        } else {
            tvAge.setVisibility(View.GONE);
        }

        // Display username
        if (profile.getUsername() != null && !profile.getUsername().isEmpty()) {
            tvUsername.setText("@" + profile.getUsername());
            tvUsername.setVisibility(View.VISIBLE);
        } else {
            tvUsername.setVisibility(View.GONE);
        }

        // Display location (city + country)
        String location = buildLocationString(profile.getCity(), profile.getCountry());
        tvLocation.setText(location);

        // Display bio
        if (profile.getBio() != null && !profile.getBio().isEmpty()) {
            tvBio.setText(profile.getBio());
        } else {
            tvBio.setText("No bio yet. Tap edit to add one!");
        }

        // Display hobbies
        if (profile.getHobbies() != null && !profile.getHobbies().isEmpty()) {
            tvHobbies.setText(profile.getHobbies());
        } else {
            tvHobbies.setText("Not set");
        }

        // Display dreams
        if (profile.getDreams() != null && !profile.getDreams().isEmpty()) {
            tvDreams.setText(profile.getDreams());
        } else {
            tvDreams.setText("Not set");
        }

        // Load profile photo with Glide
        String photoUrl = profile.getPrimaryPhoto();
        if (photoUrl == null || photoUrl.isEmpty()) {
            // Fallback: cek dari photos list
            if (profile.getPhotos() != null && !profile.getPhotos().isEmpty()) {
                photoUrl = profile.getPhotos().get(0).getPhotoUrl();
            }
        }

        if (photoUrl != null && !photoUrl.isEmpty()) {
            String fullUrl = photoUrl.startsWith("http") ? photoUrl : Constants.IMAGE_BASE_URL + photoUrl;
            Glide.with(this)
                    .load(fullUrl)
                    .placeholder(R.drawable.ic_person)
                    .error(R.drawable.ic_person)
                    .centerCrop()
                    .into(ivProfilePhoto);
        }
    }

    private String buildLocationString(String city, String country) {
        if (city != null && !city.isEmpty() && country != null && !country.isEmpty()) {
            return city + ", " + country;
        } else if (city != null && !city.isEmpty()) {
            return city;
        } else if (country != null && !country.isEmpty()) {
            return country;
        }
        return "Unknown location";
    }

    private void openEditProfile() {
        Intent intent = new Intent(requireContext(), com.beeflirt.beetalk.activities.EditProfileActivity.class);
        startActivity(intent);
    }

    private void openPreferences() {
        Intent intent = new Intent(requireContext(), com.beeflirt.beetalk.activities.PreferencesActivity.class);
        startActivity(intent);
    }

    private void openAccountSettings() {
        Intent intent = new Intent(requireContext(), com.beeflirt.beetalk.activities.AccountSettingsActivity.class);
        startActivity(intent);
    }

    private void openBlockedUsers() {
        Intent intent = new Intent(requireContext(), BlockedUsersActivity.class);
        startActivity(intent);
    }

    private void loadEntitlements() {
        apiService.getEntitlements().enqueue(new Callback<ApiResponse<Map<String, Object>>>() {
            @Override
            public void onResponse(Call<ApiResponse<Map<String, Object>>> call,
                    Response<ApiResponse<Map<String, Object>>> response) {
                if (!isAdded()) return;
                if (!response.isSuccessful() || response.body() == null || response.body().getData() == null) {
                    return;
                }
                Map<String, Object> data = response.body().getData();
                boolean premiumEnabled = false;
                Object isPremEnabled = data.get("premium_enabled");
                if (isPremEnabled instanceof Boolean) premiumEnabled = (Boolean) isPremEnabled;
                else if (isPremEnabled instanceof Number) premiumEnabled = ((Number) isPremEnabled).intValue() == 1;

                boolean premium = false;
                Object isPrem = data.get("is_premium");
                if (isPrem instanceof Boolean) premium = (Boolean) isPrem;
                else if (isPrem instanceof Number) premium = ((Number) isPrem).intValue() == 1;

                String until = data.get("premium_until") != null
                        ? String.valueOf(data.get("premium_until")) : null;
                int superLike = 5;
                int undo = 1;
                Object quotas = data.get("quotas");
                if (quotas instanceof Map) {
                    Map<?, ?> q = (Map<?, ?>) quotas;
                    if (q.get("super_like_daily") instanceof Number) {
                        superLike = ((Number) q.get("super_like_daily")).intValue();
                    }
                    if (q.get("undo_daily") instanceof Number) {
                        undo = ((Number) q.get("undo_daily")).intValue();
                    }
                }
                String weekly = "premium_weekly", monthly = "premium_monthly", yearly = "premium_yearly", boost = "boost_30m";
                Object products = data.get("products");
                if (products instanceof Map) {
                    Map<?, ?> p = (Map<?, ?>) products;
                    if (p.get("weekly") != null) weekly = String.valueOf(p.get("weekly"));
                    if (p.get("monthly") != null) monthly = String.valueOf(p.get("monthly"));
                    if (p.get("yearly") != null) yearly = String.valueOf(p.get("yearly"));
                    if (p.get("boost") != null) boost = String.valueOf(p.get("boost"));
                }
                boolean adFree = premium;
                Object features = data.get("features");
                if (features instanceof Map) {
                    Object af = ((Map<?, ?>) features).get("ad_free");
                    if (af instanceof Boolean) adFree = (Boolean) af;
                    else if (af instanceof Number) adFree = ((Number) af).intValue() == 1;
                }
                String boostUntil = data.get("boost_until") != null
                        ? String.valueOf(data.get("boost_until")) : null;
                com.beeflirt.beetalk.utils.PremiumManager.getInstance(requireContext())
                        .applySnapshot(premiumEnabled, premium, until, superLike, undo, weekly, monthly, yearly,
                                boost, adFree, boostUntil);

                if (btnPremium != null) {
                    btnPremium.setVisibility(premiumEnabled ? View.VISIBLE : View.GONE);
                }

                if (tvPremiumLabel != null) {
                    tvPremiumLabel.setText(premium ? "Premium active · Ad-free" : "BeeTalk Premium");
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<Map<String, Object>>> call, Throwable t) {
                // keep cache
            }
        });
    }

    private void logout() {
        android.app.Dialog dialog = new android.app.Dialog(requireContext());
        dialog.requestWindowFeature(android.view.Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_logout_modern);
        
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));
            dialog.getWindow().setLayout(android.view.ViewGroup.LayoutParams.WRAP_CONTENT, android.view.ViewGroup.LayoutParams.WRAP_CONTENT);
        }

        View btnConfirm = dialog.findViewById(R.id.btn_confirm_logout);
        View btnCancel = dialog.findViewById(R.id.btn_cancel_logout);

        btnConfirm.setOnClickListener(v -> {
            dialog.dismiss();
            performLogout();
        });

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        dialog.setCancelable(true);
        dialog.show();
    }

    private void performLogout() {
        // Call backend logout API (invalidate token server-side)
        try {
            apiService.logout().enqueue(new Callback<ApiResponse<Object>>() {
                @Override
                public void onResponse(Call<ApiResponse<Object>> call, Response<ApiResponse<Object>> response) {
                    // Ignore response - proceed with local logout regardless
                }

                @Override
                public void onFailure(Call<ApiResponse<Object>> call, Throwable t) {
                    // Ignore failure - proceed with local logout regardless
                }
            });
        } catch (Exception ignored) {}

        // Clear Room database (remove cached data for privacy)
        java.util.concurrent.Executors.newSingleThreadExecutor().execute(() -> {
            com.beeflirt.beetalk.db.AppDatabase.getInstance(requireContext()).clearAllTables();
        });

        // Clear local session
        prefManager.clearSession();
        com.beeflirt.beetalk.utils.PremiumManager.getInstance(requireContext()).clear();

        // Reset API client (clear cached token)
        com.beeflirt.beetalk.api.ApiClient.resetApiService();

        // Navigate to login
        Intent intent = new Intent(requireActivity(), LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        requireActivity().finish();
    }

    @Override
    public void onResume() {
        super.onResume();
        loadProfile();
        loadEntitlements();
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
    }
}
