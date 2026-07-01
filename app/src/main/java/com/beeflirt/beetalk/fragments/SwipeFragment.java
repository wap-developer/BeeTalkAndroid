package com.beeflirt.beetalk.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.DefaultItemAnimator;

import com.beeflirt.beetalk.R;
import com.beeflirt.beetalk.adapters.CardStackAdapter;
import com.beeflirt.beetalk.activities.PreferencesActivity;
import com.beeflirt.beetalk.ads.Rewarded;
import com.beeflirt.beetalk.adsconfig.Ads;
import com.beeflirt.beetalk.api.ApiClient;
import com.beeflirt.beetalk.api.ApiService;
import com.beeflirt.beetalk.models.ApiResponse;
import com.beeflirt.beetalk.models.Profile;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import com.yuyakaido.android.cardstackview.CardStackLayoutManager;
import com.yuyakaido.android.cardstackview.CardStackListener;
import com.yuyakaido.android.cardstackview.CardStackView;
import com.yuyakaido.android.cardstackview.Direction;
import com.yuyakaido.android.cardstackview.StackFrom;
import com.yuyakaido.android.cardstackview.SwipeableMethod;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SwipeFragment extends Fragment implements CardStackListener {

    private CardStackView cardStackView;
    private CardStackLayoutManager layoutManager;
    private CardStackAdapter adapter;
    private List<Profile> profiles;
    private ProgressBar progressBar;
    private LinearLayout tvNoProfiles;
    private View actionButtonsContainer;
    private int swipeCount = 0;
    // Local set to track swiped user IDs — prevents reappearance due to async race conditions
    private final java.util.Set<Integer> localSwipedIds = new java.util.HashSet<>();
    private static final int SWIPES_BEFORE_AD = 5;
    private TextView tvSuperLikeQuota;
    private android.widget.Button btnWatchAdSuperLike;
    private android.widget.ImageView btnPreferences;

    private ApiService apiService;
    private int superLikesRemaining = 5;

    // Undo swipe
    private int undoRemaining = 1;
    private int lastSwipedPosition = -1;
    private long lastSwipeTime = 0;
    private com.google.android.material.floatingactionbutton.FloatingActionButton btnUndo;
    private TextView tvUndoQuota;
    private android.os.Handler undoTimerHandler;
    private Runnable undoTimerRunnable;

    // Handler for periodic ad status check
    private android.os.Handler adStatusHandler;
    private Runnable adStatusChecker;
    
    // Flag to trigger reload after preferences change
    public static boolean reloadNeeded = false;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_swipe, container, false);

        // Initialize API
        apiService = ApiClient.getApiService(requireContext());

        // Initialize views
        cardStackView = view.findViewById(R.id.card_stack_view);
        progressBar = view.findViewById(R.id.progress_bar);
        tvNoProfiles = view.findViewById(R.id.tv_no_profiles);
        actionButtonsContainer = view.findViewById(R.id.action_buttons_container);
        tvSuperLikeQuota = view.findViewById(R.id.tv_super_like_quota);
        btnWatchAdSuperLike = view.findViewById(R.id.btn_watch_ad_superlike);
        btnPreferences = view.findViewById(R.id.setPreferences);

        // Initialize profiles list
        profiles = new ArrayList<>();

        // Setup CardStackView
        setupCardStackView();

        // Setup action buttons
        setupActionButtons(view);

        // Setup undo
        btnUndo = view.findViewById(R.id.btn_undo);
        tvUndoQuota = view.findViewById(R.id.tv_undo_quota);
        undoTimerHandler = new android.os.Handler(android.os.Looper.getMainLooper());
        btnUndo.setOnClickListener(v -> performUndo());
        btnUndo.setEnabled(false);
        btnUndo.setAlpha(0.4f);

        // Load potential matches
        loadPotentialMatches();

        // Load super like quota from backend
        loadSuperLikeQuota();

        // Load undo quota
        loadUndoQuota();

        // Load Ads
        Ads.LoadInterstitialAds(getActivity(), com.beeflirt.beetalk.adsconfig.SettingAds.PLACEMENT_INTERSTITIAL_SWIPE);
        Ads.LoadRewardsAds(getActivity(), com.beeflirt.beetalk.adsconfig.SettingAds.PLACEMENT_REWARDED_SUPER_LIKE);

        // Setup Watch Ad button
        btnWatchAdSuperLike.setOnClickListener(v -> showRewardedAdForSuperLike());

        // Hide watch ad button initially, will be shown when quota = 0
        btnWatchAdSuperLike.setVisibility(View.GONE);

        // Setup periodic ad status checker
        setupAdStatusChecker();

        RelativeLayout bannerAds = view.findViewById(R.id.banner_container);
        Ads.ShowBanner(getActivity(), bannerAds, com.beeflirt.beetalk.adsconfig.SettingAds.PLACEMENT_BANNER_SWIPE);

        // Setup btn_preferences click logic
        btnPreferences.setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), PreferencesActivity.class);
            startActivity(intent);
        });

        return view;
    }

    private void setupAdStatusChecker() {
        adStatusHandler = new android.os.Handler(android.os.Looper.getMainLooper());
        adStatusChecker = new Runnable() {
            @Override
            public void run() {
                if (!isAdded()) return;
                
                // Update button status if quota is 0
                if (superLikesRemaining <= 0) {
                    updateWatchAdButtonVisibility();
                }
                // Check again in 2 seconds
                adStatusHandler.postDelayed(this, 2000);
            }
        };
    }

    private void setupActionButtons(View view) {
        com.google.android.material.floatingactionbutton.FloatingActionButton btnPass = view
                .findViewById(R.id.btn_pass);
        com.google.android.material.floatingactionbutton.FloatingActionButton btnSuperLike = view
                .findViewById(R.id.btn_super_like);
        com.google.android.material.floatingactionbutton.FloatingActionButton btnLike = view
                .findViewById(R.id.btn_like);

        // Pass button - swipe left
        btnPass.setOnClickListener(v -> {
            if (profiles.size() > 0 && layoutManager.getTopPosition() < profiles.size()) {
                com.yuyakaido.android.cardstackview.SwipeAnimationSetting setting = new com.yuyakaido.android.cardstackview.SwipeAnimationSetting.Builder()
                        .setDirection(Direction.Left)
                        .setDuration(200)
                        .build();
                layoutManager.setSwipeAnimationSetting(setting);
                cardStackView.swipe();
            } else {
                Toast.makeText(requireContext(), "No more profiles to swipe", Toast.LENGTH_SHORT).show();
            }
        });

        // Super like button - swipe up
        btnSuperLike.setOnClickListener(v -> {
            if (profiles.size() > 0 && layoutManager.getTopPosition() < profiles.size()) {
                if (superLikesRemaining > 0) {
                    com.yuyakaido.android.cardstackview.SwipeAnimationSetting setting = new com.yuyakaido.android.cardstackview.SwipeAnimationSetting.Builder()
                            .setDirection(Direction.Top)
                            .setDuration(200)
                            .build();
                    layoutManager.setSwipeAnimationSetting(setting);
                    cardStackView.swipe();
                } else {
                    Toast.makeText(requireContext(), "No super likes left today!",
                            Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(requireContext(), "No more profiles to swipe", Toast.LENGTH_SHORT).show();
            }
        });

        // Like button - swipe right
        btnLike.setOnClickListener(v -> {
            if (profiles.size() > 0 && layoutManager.getTopPosition() < profiles.size()) {
                com.yuyakaido.android.cardstackview.SwipeAnimationSetting setting = new com.yuyakaido.android.cardstackview.SwipeAnimationSetting.Builder()
                        .setDirection(Direction.Right)
                        .setDuration(200)
                        .build();
                layoutManager.setSwipeAnimationSetting(setting);
                cardStackView.swipe();
            } else {
                Toast.makeText(requireContext(), "No more profiles to swipe", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupCardStackView() {
        layoutManager = new CardStackLayoutManager(requireContext(), this);
        layoutManager.setStackFrom(StackFrom.Top);
        layoutManager.setVisibleCount(3);
        layoutManager.setTranslationInterval(8.0f);
        layoutManager.setScaleInterval(0.95f);
        layoutManager.setSwipeThreshold(0.3f);
        layoutManager.setMaxDegree(20.0f);
        layoutManager.setDirections(Direction.FREEDOM); // Allow all directions including up
        layoutManager.setCanScrollHorizontal(true);
        layoutManager.setCanScrollVertical(true); // Enable vertical swipe for super like
        layoutManager.setSwipeableMethod(SwipeableMethod.AutomaticAndManual);

        adapter = new CardStackAdapter(profiles);
        cardStackView.setLayoutManager(layoutManager);
        cardStackView.setAdapter(adapter);
        cardStackView.setItemAnimator(new DefaultItemAnimator());

        Log.d("SwipeFragment", "CardStackView setup complete");
        Log.d("SwipeFragment", "Adapter item count: " + adapter.getItemCount());
    }

    /**
     * Fresh load: clear list, reset local swiped tracking, then fetch from server.
     * Called on first load and on resume after preferences change.
     */
    private void loadPotentialMatches() {
        localSwipedIds.clear();
        fetchProfiles(true);
    }

    /**
     * Append-load: fetch more profiles and add only unseen ones.
     * Called when card stack is running low to avoid clearing visible cards.
     */
    private void loadMoreProfiles() {
        fetchProfiles(false);
    }

    private void fetchProfiles(final boolean isFreshLoad) {
        progressBar.setVisibility(View.VISIBLE);
        if (isFreshLoad) tvNoProfiles.setVisibility(View.GONE);

        Log.d("SwipeFragment", "Fetching profiles (fresh=" + isFreshLoad + ")...");

        apiService.getPotentialMatches().enqueue(new Callback<ApiResponse<Map<String, Object>>>() {
            @Override
            public void onResponse(Call<ApiResponse<Map<String, Object>>> call,
                    Response<ApiResponse<Map<String, Object>>> response) {
                if (!isAdded()) return;

                progressBar.setVisibility(View.GONE);

                Log.d("SwipeFragment", "Response code: " + response.code());

                if (response.isSuccessful() && response.body() != null) {
                    ApiResponse<Map<String, Object>> apiResponse = response.body();

                    if (apiResponse.isStatus()) {
                        Map<String, Object> data = apiResponse.getData();

                        Gson gson = new Gson();
                        String profilesJson = gson.toJson(data.get("profiles"));
                        Type listType = new TypeToken<List<Profile>>() {}.getType();
                        List<Profile> newProfiles = gson.fromJson(profilesJson, listType);

                        if (newProfiles != null && !newProfiles.isEmpty()) {
                            if (isFreshLoad) {
                                // Fresh load: replace, but filter out already-swiped
                                profiles.clear();
                                for (Profile p : newProfiles) {
                                    if (!localSwipedIds.contains(p.getUserId())) {
                                        profiles.add(p);
                                    }
                                }
                            } else {
                                // Append mode: only add profiles not already seen
                                java.util.Set<Integer> existingIds = new java.util.HashSet<>();
                                for (Profile existing : profiles) {
                                    existingIds.add(existing.getUserId());
                                }
                                for (Profile p : newProfiles) {
                                    if (!localSwipedIds.contains(p.getUserId())
                                            && !existingIds.contains(p.getUserId())) {
                                        profiles.add(p);
                                    }
                                }
                            }

                            if (!profiles.isEmpty()) {
                                showProfiles();
                                adapter.notifyDataSetChanged();
                            } else {
                                showNoProfiles();
                            }
                        } else {
                            if (profiles.isEmpty() || layoutManager.getTopPosition() >= profiles.size()) {
                                showNoProfiles();
                            }
                        }
                    } else {
                        Log.e("SwipeFragment", "API error: " + apiResponse.getMessage());
                        if (isFreshLoad) {
                            Toast.makeText(requireContext(), "Error: " + apiResponse.getMessage(),
                                    Toast.LENGTH_LONG).show();
                            showNoProfiles();
                        }
                    }
                } else {
                    Log.e("SwipeFragment", "Response not successful. Code: " + response.code());
                    if (isFreshLoad) {
                        try {
                            String errorBody = response.errorBody() != null
                                    ? response.errorBody().string() : "No error body";
                            Toast.makeText(requireContext(), "Failed: " + errorBody,
                                    Toast.LENGTH_LONG).show();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        showNoProfiles();
                    }
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<Map<String, Object>>> call, Throwable t) {
                if (!isAdded()) return;

                progressBar.setVisibility(View.GONE);
                Log.e("SwipeFragment", "API call failed: " + t.getMessage(), t);
                if (isFreshLoad) {
                    Toast.makeText(requireContext(), "Connection error: " + t.getMessage(),
                            Toast.LENGTH_LONG).show();
                    showNoProfiles();
                }
            }
        });
    }

    private void showNoProfiles() {
        tvNoProfiles.setVisibility(View.VISIBLE);
        cardStackView.setVisibility(View.GONE);
        if (actionButtonsContainer != null) {
            actionButtonsContainer.setVisibility(View.GONE);
        }
    }

    private void showProfiles() {
        tvNoProfiles.setVisibility(View.GONE);
        cardStackView.setVisibility(View.VISIBLE);
        if (actionButtonsContainer != null) {
            actionButtonsContainer.setVisibility(View.VISIBLE);
        }
    }

    private void sendSwipe(int targetUserId, String action) {
        JsonObject swipeData = new JsonObject();
        swipeData.addProperty("swiped_id", targetUserId);
        swipeData.addProperty("action", action);

        apiService.swipe(swipeData).enqueue(new Callback<ApiResponse<Map<String, Object>>>() {
            @Override
            public void onResponse(Call<ApiResponse<Map<String, Object>>> call,
                    Response<ApiResponse<Map<String, Object>>> response) {
                if (!isAdded()) return;
                
                if (response.isSuccessful() && response.body() != null) {
                    ApiResponse<Map<String, Object>> apiResponse = response.body();

                    if (apiResponse.isStatus()) {
                        Map<String, Object> data = apiResponse.getData();

                        // Check if it's a match
                        Object matchedObj = data.get("matched");
                        boolean matched = false;

                        if (matchedObj instanceof Boolean) {
                            matched = (Boolean) matchedObj;
                        } else if (matchedObj instanceof Double) {
                            matched = ((Double) matchedObj) == 1.0;
                        } else if (matchedObj instanceof Integer) {
                            matched = ((Integer) matchedObj) == 1;
                        }

                        if (matched) {
                            // Get matched user info from response
                            String matchName = "";
                            String matchPhoto = "";
                            int matchId = 0;
                            int matchUserId = 0;

                            try {
                                Object matchedUserObj = data.get("matched_user");
                                if (matchedUserObj instanceof Map) {
                                    Map<String, Object> matchedUser = (Map<String, Object>) matchedUserObj;
                                    matchName = matchedUser.get("name") != null ? matchedUser.get("name").toString() : "";
                                    // Get photo
                                    Object photosObj = matchedUser.get("photos");
                                    if (photosObj instanceof java.util.List && !((java.util.List<?>) photosObj).isEmpty()) {
                                        Object firstPhoto = ((java.util.List<?>) photosObj).get(0);
                                        if (firstPhoto instanceof Map) {
                                            matchPhoto = ((Map<String, Object>) firstPhoto).get("photo_url").toString();
                                        }
                                    }
                                }
                                Object matchIdObj = data.get("match_id");
                                if (matchIdObj instanceof Number) matchId = ((Number) matchIdObj).intValue();
                                matchUserId = targetUserId;
                            } catch (Exception ignored) {}

                            showMatchDialog(matchName, matchPhoto, matchId, matchUserId);
                        }

                        Log.d("SwipeFragment", "Swipe " + action + " sent successfully");
                    }
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<Map<String, Object>>> call, Throwable t) {
                if (!isAdded()) return;
                Log.e("SwipeFragment", "Swipe failed: " + t.getMessage());
            }
        });
    }

    private void loadSuperLikeQuota() {
        apiService.getSuperLikeQuota().enqueue(new Callback<ApiResponse<Map<String, Object>>>() {
            @Override
            public void onResponse(Call<ApiResponse<Map<String, Object>>> call,
                    Response<ApiResponse<Map<String, Object>>> response) {
                if (!isAdded()) return;
                
                if (response.isSuccessful() && response.body() != null) {
                    ApiResponse<Map<String, Object>> apiResponse = response.body();

                    if (apiResponse.isStatus()) {
                        Map<String, Object> data = apiResponse.getData();
                        Object remainingObj = data.get("remaining");

                        if (remainingObj instanceof Double) {
                            superLikesRemaining = ((Double) remainingObj).intValue();
                        } else if (remainingObj instanceof Integer) {
                            superLikesRemaining = (Integer) remainingObj;
                        }

                        updateSuperLikeQuotaUI();
                        updateWatchAdButtonVisibility();
                    }
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<Map<String, Object>>> call, Throwable t) {
                if (!isAdded()) return;
                Log.e("SwipeFragment", "Failed to load super like quota: " + t.getMessage());
            }
        });
    }

    private void updateSuperLikeQuotaUI() {
        if (tvSuperLikeQuota != null) {
            tvSuperLikeQuota.setText(superLikesRemaining + " ⭐ Super Likes left today");
        }
        updateWatchAdButtonVisibility();
    }

    private void sendSuperLike(int targetUserId) {
        JsonObject superLikeData = new JsonObject();
        superLikeData.addProperty("swiped_id", targetUserId);

        apiService.superLike(superLikeData).enqueue(new Callback<ApiResponse<Map<String, Object>>>() {
            @Override
            public void onResponse(Call<ApiResponse<Map<String, Object>>> call,
                    Response<ApiResponse<Map<String, Object>>> response) {
                if (!isAdded()) return;
                
                if (response.isSuccessful() && response.body() != null) {
                    ApiResponse<Map<String, Object>> apiResponse = response.body();

                    if (apiResponse.isStatus()) {
                        Map<String, Object> data = apiResponse.getData();

                        // Update remaining super likes
                        Object remainingObj = data.get("remaining_super_likes");
                        if (remainingObj instanceof Double) {
                            superLikesRemaining = ((Double) remainingObj).intValue();
                        } else if (remainingObj instanceof Integer) {
                            superLikesRemaining = (Integer) remainingObj;
                        }
                        updateSuperLikeQuotaUI();

                        // Check if it's a match
                        Object matchedObj = data.get("matched");
                        boolean matched = false;

                        if (matchedObj instanceof Boolean) {
                            matched = (Boolean) matchedObj;
                        } else if (matchedObj instanceof Double) {
                            matched = ((Double) matchedObj) == 1.0;
                        } else if (matchedObj instanceof Integer) {
                            matched = ((Integer) matchedObj) == 1;
                        }

                        if (matched) {
                            String matchName = "";
                            String matchPhoto = "";
                            int matchId = 0;

                            try {
                                Object matchedUserObj = data.get("matched_user");
                                if (matchedUserObj instanceof Map) {
                                    Map<String, Object> matchedUser = (Map<String, Object>) matchedUserObj;
                                    matchName = matchedUser.get("name") != null ? matchedUser.get("name").toString() : "";
                                    Object photosObj = matchedUser.get("photos");
                                    if (photosObj instanceof java.util.List && !((java.util.List<?>) photosObj).isEmpty()) {
                                        Object firstPhoto = ((java.util.List<?>) photosObj).get(0);
                                        if (firstPhoto instanceof Map) {
                                            matchPhoto = ((Map<String, Object>) firstPhoto).get("photo_url").toString();
                                        }
                                    }
                                }
                                Object matchIdObj = data.get("match_id");
                                if (matchIdObj instanceof Number) matchId = ((Number) matchIdObj).intValue();
                            } catch (Exception ignored) {}

                            showMatchDialog(matchName, matchPhoto, matchId, targetUserId);
                        } else {
                            Toast.makeText(requireContext(), "Super Like sent!", Toast.LENGTH_SHORT).show();
                        }

                        Log.d("SwipeFragment", "Super like sent successfully");
                    } else {
                        Toast.makeText(requireContext(), apiResponse.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(requireContext(), "Failed to send super like", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<Map<String, Object>>> call, Throwable t) {
                if (!isAdded()) return;
                Log.e("SwipeFragment", "Super like failed: " + t.getMessage());
                Toast.makeText(requireContext(), "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    // CardStackListener methods
    @Override
    public void onCardDragging(Direction direction, float ratio) {
        // Card is being dragged
    }

    @Override
    public void onCardSwiped(Direction direction) {
        int position = layoutManager.getTopPosition() - 1;

        if (position >= 0 && position < profiles.size()) {
            Profile swipedProfile = profiles.get(position);

            // Track locally to prevent reappearance from async race condition
            localSwipedIds.add(swipedProfile.getUserId());

            // Store for undo
            lastSwipedPosition = position;
            lastSwipeTime = System.currentTimeMillis();
            enableUndoTemporarily();

            if (direction == Direction.Top) {
                // Super Liked
                Log.d("SwipeFragment", "Super Liked: " + swipedProfile.getName());
                sendSuperLike(swipedProfile.getUserId());
                loadSuperLikeQuota();
            } else if (direction == Direction.Right) {
                // Liked
                Log.d("SwipeFragment", "Liked: " + swipedProfile.getName());
                sendSwipe(swipedProfile.getUserId(), "like");
            } else {
                // Passed (Left atau Bottom)
                Log.d("SwipeFragment", "Passed: " + swipedProfile.getName());
                sendSwipe(swipedProfile.getUserId(), "pass");
            }
        }

        // Check if stack is exhausted
        if (layoutManager.getTopPosition() >= profiles.size()) {
            // Stack completely empty - show no profiles
            showNoProfiles();
        } else if (layoutManager.getTopPosition() >= profiles.size() - 2) {
            // Running low - try to load more
            loadMoreProfiles();
        }

        // Increment swipe count and show interstitial ad
        swipeCount++;
        if (swipeCount >= SWIPES_BEFORE_AD) {
            Ads.ShowInterstitialAds(getActivity(), com.beeflirt.beetalk.adsconfig.SettingAds.PLACEMENT_INTERSTITIAL_SWIPE);
            swipeCount = 0; // Reset counter after showing ad
        }
    }

    @Override
    public void onCardRewound() {
        // Card rewound
    }

    @Override
    public void onCardCanceled() {
        // Card drag canceled
    }

    @Override
    public void onCardAppeared(View view, int position) {
        // Card appeared
    }

    @Override
    public void onCardDisappeared(View view, int position) {
        // Card disappeared
    }

    /**
     * Show rewarded ad to earn extra Super Like
     */
    private void showRewardedAdForSuperLike() {
        if (!Rewarded.isReady()) {
            Toast.makeText(getContext(),
                    "Ad not ready. Please try again in a moment.",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        Ads.ShowRewardsAds(getActivity(), com.beeflirt.beetalk.adsconfig.SettingAds.PLACEMENT_REWARDED_SUPER_LIKE, new Rewarded.RewardCallback() {
            @Override
            public void onUserEarnedReward(int amount) {
                // Grant extra Super Like via backend API
                grantRewardedSuperLike();
            }

            @Override
            public void onAdFailedToShow(String error) {
                if (!isAdded()) return;
                Toast.makeText(getContext(),
                        "Failed to show ad: " + error,
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    // updateSuperLikeQuotaDisplay merged into updateSuperLikeQuotaUI

    /**
     * Update Watch Ad button visibility
     * Show when quota = 0, disable if ad not ready
     */
    private void updateWatchAdButtonVisibility() {
        if (btnWatchAdSuperLike != null) {
            // Show button when quota is 0
            boolean shouldShow = (superLikesRemaining <= 0);
            btnWatchAdSuperLike.setVisibility(shouldShow ? View.VISIBLE : View.GONE);

            // Enable button only if ad is ready
            if (shouldShow) {
                boolean isAdReady = Rewarded.isReady();
                btnWatchAdSuperLike.setEnabled(isAdReady);

                // Update button text based on ad status
                if (isAdReady) {
                    btnWatchAdSuperLike.setText("Watch Ad for Extra Super Like");
                } else {
                    btnWatchAdSuperLike.setText("Loading Ad...");
                }
            }
        }
    }

    /**
     * Grant rewarded super like via backend API
     */
    private void grantRewardedSuperLike() {
        apiService.addRewardedSuperLike().enqueue(new Callback<ApiResponse<Map<String, Object>>>() {
            @Override
            public void onResponse(Call<ApiResponse<Map<String, Object>>> call,
                    Response<ApiResponse<Map<String, Object>>> response) {
                if (!isAdded()) return;
                
                if (response.isSuccessful() && response.body() != null) {
                    ApiResponse<Map<String, Object>> apiResponse = response.body();

                    if (apiResponse.isStatus()) {
                        Map<String, Object> data = apiResponse.getData();

                        // Update remaining super likes
                        Object remainingObj = data.get("remaining");
                        if (remainingObj instanceof Double) {
                            superLikesRemaining = ((Double) remainingObj).intValue();
                        } else if (remainingObj instanceof Integer) {
                            superLikesRemaining = (Integer) remainingObj;
                        }

                        updateSuperLikeQuotaUI();

                        Toast.makeText(getContext(),
                                "🎉 You earned 1 extra Super Like!",
                                Toast.LENGTH_LONG).show();

                        Log.d("SwipeFragment", "Rewarded super like granted successfully");
                    } else {
                        Toast.makeText(getContext(),
                                "Failed to grant bonus: " + apiResponse.getMessage(),
                                Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(getContext(),
                            "Failed to grant rewarded super like",
                            Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<Map<String, Object>>> call, Throwable t) {
                if (!isAdded()) return;
                Log.e("SwipeFragment", "Grant rewarded super like failed: " + t.getMessage());
                Toast.makeText(getContext(),
                        "Error: " + t.getMessage(),
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadUndoQuota() {
        apiService.getUndoQuota().enqueue(new Callback<ApiResponse<Map<String, Object>>>() {
            @Override
            public void onResponse(Call<ApiResponse<Map<String, Object>>> call,
                    Response<ApiResponse<Map<String, Object>>> response) {
                if (!isAdded()) return;

                if (response.isSuccessful() && response.body() != null) {
                    ApiResponse<Map<String, Object>> apiResponse = response.body();

                    if (apiResponse.isStatus()) {
                        Map<String, Object> data = apiResponse.getData();
                        Object remainingObj = data.get("remaining");

                        if (remainingObj instanceof Double) {
                            undoRemaining = ((Double) remainingObj).intValue();
                        } else if (remainingObj instanceof Integer) {
                            undoRemaining = (Integer) remainingObj;
                        }

                        updateUndoUI();
                    }
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<Map<String, Object>>> call, Throwable t) {
                if (!isAdded()) return;
                Log.e("SwipeFragment", "Failed to load undo quota: " + t.getMessage());
            }
        });
    }

    private void performUndo() {
        if (undoRemaining <= 0) {
            Toast.makeText(requireContext(), "No undos left today", Toast.LENGTH_SHORT).show();
            return;
        }

        long elapsed = System.currentTimeMillis() - lastSwipeTime;
        if (lastSwipedPosition < 0 || elapsed > 10000) {
            Toast.makeText(requireContext(), "Undo time expired", Toast.LENGTH_SHORT).show();
            disableUndo();
            return;
        }

        btnUndo.setEnabled(false);

        apiService.undoSwipe().enqueue(new Callback<ApiResponse<Map<String, Object>>>() {
            @Override
            public void onResponse(Call<ApiResponse<Map<String, Object>>> call,
                    Response<ApiResponse<Map<String, Object>>> response) {
                if (!isAdded()) return;

                if (response.isSuccessful() && response.body() != null) {
                    ApiResponse<Map<String, Object>> apiResponse = response.body();

                    if (apiResponse.isStatus()) {
                        // Rewind card
                        Profile undoneProfile = profiles.get(lastSwipedPosition);
                        localSwipedIds.remove(undoneProfile.getUserId());

                        com.yuyakaido.android.cardstackview.RewindAnimationSetting setting =
                                new com.yuyakaido.android.cardstackview.RewindAnimationSetting.Builder()
                                        .setDirection(Direction.Bottom)
                                        .setDuration(300)
                                        .build();
                        layoutManager.setRewindAnimationSetting(setting);
                        cardStackView.rewind();

                        showProfiles();

                        undoRemaining = 0;
                        lastSwipedPosition = -1;
                        updateUndoUI();
                        disableUndo();

                        // Restore super like quota if it was a super like
                        Map<String, Object> data = apiResponse.getData();
                        Object wasSuperLikeObj = data.get("was_super_like");
                        boolean wasSuperLike = false;
                        if (wasSuperLikeObj instanceof Boolean) {
                            wasSuperLike = (Boolean) wasSuperLikeObj;
                        }
                        if (wasSuperLike) {
                            loadSuperLikeQuota();
                        }

                        Toast.makeText(requireContext(), "Swipe undone!", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(requireContext(), apiResponse.getMessage(), Toast.LENGTH_SHORT).show();
                        btnUndo.setEnabled(true);
                    }
                } else {
                    Toast.makeText(requireContext(), "Failed to undo swipe", Toast.LENGTH_SHORT).show();
                    btnUndo.setEnabled(true);
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<Map<String, Object>>> call, Throwable t) {
                if (!isAdded()) return;
                Toast.makeText(requireContext(), "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                btnUndo.setEnabled(true);
            }
        });
    }

    private void enableUndoTemporarily() {
        if (undoRemaining <= 0) return;

        btnUndo.setEnabled(true);
        btnUndo.setAlpha(1.0f);
        tvUndoQuota.setVisibility(View.VISIBLE);
        tvUndoQuota.setText(undoRemaining + " Undo left");

        if (undoTimerRunnable != null) {
            undoTimerHandler.removeCallbacks(undoTimerRunnable);
        }

        undoTimerRunnable = () -> {
            if (!isAdded()) return;
            disableUndo();
        };
        undoTimerHandler.postDelayed(undoTimerRunnable, 10000);
    }

    private void disableUndo() {
        btnUndo.setEnabled(false);
        btnUndo.setAlpha(0.4f);
        if (undoTimerRunnable != null) {
            undoTimerHandler.removeCallbacks(undoTimerRunnable);
        }
    }

    private void updateUndoUI() {
        if (tvUndoQuota != null) {
            if (undoRemaining > 0) {
                tvUndoQuota.setVisibility(View.VISIBLE);
                tvUndoQuota.setText(undoRemaining + " Undo left");
            } else {
                tvUndoQuota.setVisibility(View.VISIBLE);
                tvUndoQuota.setText("No undos left");
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        // Start periodic ad status check
        if (adStatusHandler != null && adStatusChecker != null) {
            adStatusHandler.post(adStatusChecker);
        }
        
        // Cek apakah perlu reload data (misal: habis simpan preferensi)
        if (reloadNeeded) {
            reloadNeeded = false;
            // Kosongkan list dulu agar animasi reload terasa
            profiles.clear();
            if (adapter != null) adapter.notifyDataSetChanged();
            
            // Panggil API ulang
            loadPotentialMatches();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        // Stop periodic ad status check
        if (adStatusHandler != null && adStatusChecker != null) {
            adStatusHandler.removeCallbacks(adStatusChecker);
        }
    }

    @Override
    public void onDestroyView() {
        // Stop periodic ad status check
        if (adStatusHandler != null && adStatusChecker != null) {
            adStatusHandler.removeCallbacks(adStatusChecker);
        }
        // Stop undo timer
        if (undoTimerHandler != null && undoTimerRunnable != null) {
            undoTimerHandler.removeCallbacks(undoTimerRunnable);
        }
        super.onDestroyView();
    }

    /**
     * Show match celebration dialog
     */
    private void showMatchDialog(String name, String photoUrl, int matchId, int matchUserId) {
        if (!isAdded()) return;

        android.app.Dialog dialog = new android.app.Dialog(requireContext());
        dialog.requestWindowFeature(android.view.Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_match);

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(
                    new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));
            dialog.getWindow().setLayout(
                    android.view.ViewGroup.LayoutParams.WRAP_CONTENT,
                    android.view.ViewGroup.LayoutParams.WRAP_CONTENT);
        }

        TextView tvTitle = dialog.findViewById(R.id.tv_match_title);
        TextView tvSubtitle = dialog.findViewById(R.id.tv_match_subtitle);
        TextView tvName = dialog.findViewById(R.id.tv_match_name);
        de.hdodenhof.circleimageview.CircleImageView ivPhoto = dialog.findViewById(R.id.iv_match_photo);
        View btnSendMessage = dialog.findViewById(R.id.btn_send_message);
        View btnKeepSwiping = dialog.findViewById(R.id.btn_keep_swiping);

        // Set data
        tvSubtitle.setText("You and " + name + " liked each other!");
        tvName.setText(name);

        // Load photo
        if (photoUrl != null && !photoUrl.isEmpty()) {
            String fullUrl = photoUrl.startsWith("http") ? photoUrl
                    : com.beeflirt.beetalk.utils.Constants.IMAGE_BASE_URL + photoUrl;
            com.bumptech.glide.Glide.with(requireContext())
                    .load(fullUrl)
                    .placeholder(R.drawable.ic_person)
                    .error(R.drawable.ic_person)
                    .centerCrop()
                    .into(ivPhoto);
        }

        // Send Message -> open ChatActivity
        final int fMatchId = matchId;
        final int fMatchUserId = matchUserId;
        final String fName = name;
        final String fPhoto = photoUrl;

        btnSendMessage.setOnClickListener(v -> {
            dialog.dismiss();
            Intent intent = new Intent(requireContext(), com.beeflirt.beetalk.activities.ChatActivity.class);
            intent.putExtra("match_id", fMatchId);
            intent.putExtra("user_id", fMatchUserId);
            intent.putExtra("user_name", fName);
            intent.putExtra("user_photo", fPhoto);
            startActivity(intent);
        });

        // Keep Swiping -> close dialog
        btnKeepSwiping.setOnClickListener(v -> dialog.dismiss());

        dialog.setCancelable(true);
        dialog.show();
    }
}
