package com.beeflirt.beetalk.fragments;

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
import androidx.core.widget.NestedScrollView;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.beeflirt.beetalk.R;
import com.beeflirt.beetalk.adapters.ExploreUsersAdapter;
import com.beeflirt.beetalk.adapters.GalleryFeedAdapter;
import com.beeflirt.beetalk.adsconfig.Ads;
import com.beeflirt.beetalk.api.ApiClient;
import com.beeflirt.beetalk.api.ApiService;
import com.beeflirt.beetalk.models.ApiResponse;
import com.beeflirt.beetalk.models.ExploreUser;
import com.beeflirt.beetalk.models.GalleryPost;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ExploreFragment extends Fragment {

    private static final String TAG = "ExploreFragment";
    private static final int PAGE_SIZE = 20;
    private static final int USERS_PAGE_SIZE = 10;

    private RecyclerView rvExploreUsers, rvGalleryFeed;
    private LinearLayout layoutEmptyFeed;
    private TextView btnMoreExplore;
    private SwipeRefreshLayout swipeRefreshLayout;
    private NestedScrollView nestedScrollView;
    private ProgressBar progressLoadMore;

    private ExploreUsersAdapter usersAdapter;
    private GalleryFeedAdapter feedAdapter;
    private List<ExploreUser> exploreUsers = new ArrayList<>();
    private List<GalleryPost> feedPosts = new ArrayList<>();

    private ApiService apiService;

    // Pagination state
    private int exploreUsersOffset = 0;
    private int feedOffset = 0;
    private boolean isLoadingUsers = false;
    private boolean isLoadingFeed = false;
    private boolean hasMoreUsers = true;
    private boolean hasMoreFeed = true;
    private Set<Integer> loadedUserIds = new HashSet<>();
    private Set<Integer> loadedPostIds = new HashSet<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_explore, container, false);

        apiService = ApiClient.getApiService(requireContext());

        // Initialize views
        rvExploreUsers = view.findViewById(R.id.rv_explore_users);
        rvGalleryFeed = view.findViewById(R.id.rv_gallery_feed);
        layoutEmptyFeed = view.findViewById(R.id.layout_empty_feed);
        btnMoreExplore = view.findViewById(R.id.btn_more_explore);
        swipeRefreshLayout = view.findViewById(R.id.swipe_refresh);
        nestedScrollView = view.findViewById(R.id.nested_scroll_view);
        progressLoadMore = view.findViewById(R.id.progress_load_more);

        // Setup horizontal user list
        usersAdapter = new ExploreUsersAdapter(exploreUsers);
        usersAdapter.setOnUserClickListener(user -> showProfileDialog(user));
        rvExploreUsers.setLayoutManager(
                new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false));
        rvExploreUsers.setAdapter(usersAdapter);

        // Setup grid feed (2 columns)
        feedAdapter = new GalleryFeedAdapter(feedPosts);
        feedAdapter.setOnPostClickListener(post -> {
            android.content.Intent intent = new android.content.Intent(requireContext(),
                    com.beeflirt.beetalk.activities.GalleryDetailActivity.class);
            intent.putExtra("post_id", post.getId());
            startActivity(intent);
        });
        GridLayoutManager gridLayoutManager = new GridLayoutManager(requireContext(), 2);
        rvGalleryFeed.setLayoutManager(gridLayoutManager);
        rvGalleryFeed.setAdapter(feedAdapter);
        rvGalleryFeed.setNestedScrollingEnabled(false);
        rvGalleryFeed.setHasFixedSize(false);

        // More Explore click
        btnMoreExplore.setOnClickListener(v -> {
            android.content.Intent intent = new android.content.Intent(requireContext(),
                    com.beeflirt.beetalk.activities.MoreExploreActivity.class);
            startActivity(intent);
        });

        // Pull-to-refresh
        swipeRefreshLayout.setOnRefreshListener(this::refreshAll);
        swipeRefreshLayout.setColorSchemeResources(R.color.primary);

        // Infinite scroll for horizontal users
        rvExploreUsers.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                LinearLayoutManager lm = (LinearLayoutManager) recyclerView.getLayoutManager();
                if (lm == null) return;

                int totalItems = lm.getItemCount();
                int lastVisible = lm.findLastVisibleItemPosition();

                if (!isLoadingUsers && hasMoreUsers && lastVisible >= totalItems - 3) {
                    loadNearbyUsers(true);
                }
            }
        });

        // Infinite scroll for feed via NestedScrollView
        nestedScrollView.setOnScrollChangeListener((NestedScrollView.OnScrollChangeListener)
                (v, scrollX, scrollY, oldScrollX, oldScrollY) -> {
                    int childHeight = v.getChildAt(0).getMeasuredHeight();
                    int scrollViewHeight = v.getMeasuredHeight();

                    if (scrollY >= childHeight - scrollViewHeight - 200) {
                        if (!isLoadingFeed && hasMoreFeed) {
                            loadGalleryFeed(true);
                        }
                    }
                });

        // Initial load
        loadNearbyUsers(false);
        loadGalleryFeed(false);

        // Banner ad
        RelativeLayout bannerContainer = view.findViewById(R.id.banner_container);
        Ads.ShowBanner(getActivity(), bannerContainer, com.beeflirt.beetalk.adsconfig.SettingAds.PLACEMENT_BANNER_EXPLORE);

        return view;
    }

    private void refreshAll() {
        exploreUsersOffset = 0;
        feedOffset = 0;
        hasMoreUsers = true;
        hasMoreFeed = true;
        loadedUserIds.clear();
        loadedPostIds.clear();

        exploreUsers.clear();
        usersAdapter.notifyDataSetChanged();

        feedPosts.clear();
        feedAdapter.notifyDataSetChanged();

        loadNearbyUsers(false);
        loadGalleryFeed(false);
    }

    private void loadNearbyUsers(boolean append) {
        if (isLoadingUsers) return;
        isLoadingUsers = true;

        apiService.getNearbyUsers(USERS_PAGE_SIZE, exploreUsersOffset)
                .enqueue(new Callback<ApiResponse<Map<String, Object>>>() {
            @Override
            public void onResponse(Call<ApiResponse<Map<String, Object>>> call,
                    Response<ApiResponse<Map<String, Object>>> response) {
                if (!isAdded()) return;
                isLoadingUsers = false;

                if (response.isSuccessful() && response.body() != null && response.body().isStatus()) {
                    Map<String, Object> data = response.body().getData();
                    Gson gson = new Gson();
                    String json = gson.toJson(data.get("users"));
                    Type listType = new TypeToken<List<ExploreUser>>() {}.getType();
                    List<ExploreUser> users = gson.fromJson(json, listType);

                    if (users != null && !users.isEmpty()) {
                        int added = 0;
                        for (ExploreUser user : users) {
                            if (!loadedUserIds.contains(user.getUserId())) {
                                loadedUserIds.add(user.getUserId());
                                exploreUsers.add(user);
                                added++;
                            }
                        }
                        if (added > 0) {
                            usersAdapter.notifyDataSetChanged();
                        }
                        exploreUsersOffset += users.size();
                        hasMoreUsers = users.size() >= USERS_PAGE_SIZE;
                    } else {
                        hasMoreUsers = false;
                    }
                } else {
                    if (!append) {
                        Toast.makeText(requireContext(), "Failed to load users", Toast.LENGTH_SHORT).show();
                    }
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<Map<String, Object>>> call, Throwable t) {
                if (!isAdded()) return;
                isLoadingUsers = false;
                Log.e(TAG, "Failed to load nearby users: " + t.getMessage());
                if (!append) {
                    Toast.makeText(requireContext(), "Connection error", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void loadGalleryFeed(boolean append) {
        if (isLoadingFeed) return;
        isLoadingFeed = true;

        if (append) {
            progressLoadMore.setVisibility(View.VISIBLE);
        }

        apiService.getGalleryFeed(PAGE_SIZE, feedOffset)
                .enqueue(new Callback<ApiResponse<Map<String, Object>>>() {
            @Override
            public void onResponse(Call<ApiResponse<Map<String, Object>>> call,
                    Response<ApiResponse<Map<String, Object>>> response) {
                if (!isAdded()) return;
                isLoadingFeed = false;
                swipeRefreshLayout.setRefreshing(false);
                progressLoadMore.setVisibility(View.GONE);

                if (response.isSuccessful() && response.body() != null && response.body().isStatus()) {
                    Map<String, Object> data = response.body().getData();
                    Gson gson = new Gson();
                    String json = gson.toJson(data.get("posts"));
                    Type listType = new TypeToken<List<GalleryPost>>() {}.getType();
                    List<GalleryPost> posts = gson.fromJson(json, listType);

                    if (posts != null && !posts.isEmpty()) {
                        List<GalleryPost> newPosts = new ArrayList<>();
                        for (GalleryPost post : posts) {
                            if (!loadedPostIds.contains(post.getId())) {
                                loadedPostIds.add(post.getId());
                                newPosts.add(post);
                            }
                        }

                        if (!newPosts.isEmpty()) {
                            if (append) {
                                feedPosts.addAll(newPosts);
                                feedAdapter.notifyItemRangeInserted(
                                        feedPosts.size() - newPosts.size(), newPosts.size());
                            } else {
                                feedPosts.addAll(newPosts);
                                feedAdapter.notifyDataSetChanged();
                            }
                        }

                        feedOffset += posts.size();
                        hasMoreFeed = posts.size() >= PAGE_SIZE;

                        rvGalleryFeed.setVisibility(View.VISIBLE);
                        layoutEmptyFeed.setVisibility(View.GONE);
                    } else {
                        hasMoreFeed = false;
                        if (feedPosts.isEmpty()) {
                            rvGalleryFeed.setVisibility(View.GONE);
                            layoutEmptyFeed.setVisibility(View.VISIBLE);
                        }
                    }
                } else {
                    if (!append && feedPosts.isEmpty()) {
                        rvGalleryFeed.setVisibility(View.GONE);
                        layoutEmptyFeed.setVisibility(View.VISIBLE);
                    }
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<Map<String, Object>>> call, Throwable t) {
                if (!isAdded()) return;
                isLoadingFeed = false;
                swipeRefreshLayout.setRefreshing(false);
                progressLoadMore.setVisibility(View.GONE);
                Log.e(TAG, "Failed to load feed: " + t.getMessage());
                if (!append) {
                    Toast.makeText(requireContext(), "Connection error", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    private void showProfileDialog(ExploreUser user) {
        if (!isAdded()) return;

        android.app.Dialog dialog = new android.app.Dialog(requireContext());
        dialog.requestWindowFeature(android.view.Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_user_profile);

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(
                    new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));
            dialog.getWindow().setLayout(
                    android.view.ViewGroup.LayoutParams.WRAP_CONTENT,
                    android.view.ViewGroup.LayoutParams.WRAP_CONTENT);
        }

        android.widget.ImageView ivPhoto = dialog.findViewById(R.id.iv_profile_photo);
        android.widget.TextView tvNameAge = dialog.findViewById(R.id.tv_name_age);
        android.widget.TextView tvLocation = dialog.findViewById(R.id.tv_location);
        android.widget.TextView tvUsername = dialog.findViewById(R.id.tv_username);
        android.widget.TextView tvBio = dialog.findViewById(R.id.tv_bio);
        android.widget.TextView tvHobbies = dialog.findViewById(R.id.tv_hobbies);
        android.widget.TextView tvDreams = dialog.findViewById(R.id.tv_dreams);
        android.view.View layoutBio = dialog.findViewById(R.id.layout_bio);
        android.view.View layoutHobbies = dialog.findViewById(R.id.layout_hobbies);
        android.view.View layoutDreams = dialog.findViewById(R.id.layout_dreams);
        android.view.View btnClose = dialog.findViewById(R.id.btn_close);

        String nameAge = user.getName();
        if (user.getAge() > 0) nameAge += ", " + user.getAge();
        tvNameAge.setText(nameAge);

        String location = "";
        if (user.getCity() != null && !user.getCity().isEmpty()) location = user.getCity();
        if (user.getCountry() != null && !user.getCountry().isEmpty()) {
            location += location.isEmpty() ? user.getCountry() : ", " + user.getCountry();
        }
        tvLocation.setText(location.isEmpty() ? "Unknown location" : location);

        if (user.getUsername() != null && !user.getUsername().isEmpty()) {
            tvUsername.setText("@" + user.getUsername());
        } else {
            tvUsername.setVisibility(android.view.View.GONE);
        }

        if (user.getBio() != null && !user.getBio().isEmpty()) {
            tvBio.setText(user.getBio());
            layoutBio.setVisibility(android.view.View.VISIBLE);
        }
        if (user.getHobbies() != null && !user.getHobbies().isEmpty()) {
            tvHobbies.setText(user.getHobbies());
            layoutHobbies.setVisibility(android.view.View.VISIBLE);
        }
        if (user.getDreams() != null && !user.getDreams().isEmpty()) {
            tvDreams.setText(user.getDreams());
            layoutDreams.setVisibility(android.view.View.VISIBLE);
        }

        String photoUrl = user.getPrimaryPhoto();
        if (photoUrl != null && !photoUrl.isEmpty()) {
            String fullUrl = photoUrl.startsWith("http") ? photoUrl
                    : com.beeflirt.beetalk.utils.Constants.IMAGE_BASE_URL + photoUrl;
            com.bumptech.glide.Glide.with(requireContext())
                    .load(fullUrl)
                    .centerCrop()
                    .placeholder(R.drawable.placeholder_profile)
                    .into(ivPhoto);
        }

        btnClose.setOnClickListener(v -> dialog.dismiss());
        dialog.setCancelable(true);
        dialog.show();
    }
}
