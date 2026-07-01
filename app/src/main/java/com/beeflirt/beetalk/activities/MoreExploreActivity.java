package com.beeflirt.beetalk.activities;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.beeflirt.beetalk.R;
import com.beeflirt.beetalk.api.ApiClient;
import com.beeflirt.beetalk.api.ApiService;
import com.beeflirt.beetalk.models.ApiResponse;
import com.beeflirt.beetalk.models.ExploreUser;
import com.beeflirt.beetalk.utils.Constants;
import com.bumptech.glide.Glide;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MoreExploreActivity extends BaseActivity {

    private RecyclerView rvUsers;
    private View progressBar;
    private List<ExploreUser> users = new ArrayList<>();
    private RecyclerView.Adapter<?> adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_more_explore);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        toolbar.setNavigationOnClickListener(v -> finish());

        rvUsers = findViewById(R.id.rv_users);
        progressBar = findViewById(R.id.progress_bar);

        setupAdapter();
        rvUsers.setLayoutManager(new GridLayoutManager(this, 2));
        rvUsers.setAdapter(adapter);

        loadUsers();
    }

    private void setupAdapter() {
        adapter = new RecyclerView.Adapter<RecyclerView.ViewHolder>() {
            @Override
            public RecyclerView.ViewHolder onCreateViewHolder(android.view.ViewGroup parent, int viewType) {
                View v = getLayoutInflater().inflate(R.layout.item_explore_grid, parent, false);
                return new RecyclerView.ViewHolder(v) {};
            }

            @Override
            public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
                ExploreUser user = users.get(position);

                TextView tvName = holder.itemView.findViewById(R.id.tv_name);
                TextView tvInfo = holder.itemView.findViewById(R.id.tv_info);
                ImageView ivPhoto = holder.itemView.findViewById(R.id.iv_photo);

                String nameAge = user.getName();
                if (user.getAge() > 0) nameAge += ", " + user.getAge();
                tvName.setText(nameAge);

                if (user.getDistance() > 0) {
                    tvInfo.setText(String.format("%.1f km away", user.getDistance()));
                } else if (user.getCity() != null && !user.getCity().isEmpty()) {
                    tvInfo.setText(user.getCity());
                } else {
                    tvInfo.setText("Nearby");
                }

                String photoUrl = user.getPrimaryPhoto();
                if (photoUrl != null && !photoUrl.isEmpty()) {
                    String fullUrl = photoUrl.startsWith("http") ? photoUrl : Constants.IMAGE_BASE_URL + photoUrl;
                    Glide.with(MoreExploreActivity.this)
                            .load(fullUrl)
                            .centerCrop()
                            .placeholder(R.drawable.placeholder_profile)
                            .error(R.drawable.placeholder_error)
                            .into(ivPhoto);
                } else {
                    ivPhoto.setImageResource(R.drawable.placeholder_profile);
                }

                // Click -> show profile dialog
                holder.itemView.setOnClickListener(v -> showProfileDialog(user));

                // Animate
                holder.itemView.setAlpha(0f);
                holder.itemView.setTranslationY(30f);
                holder.itemView.animate().alpha(1f).translationY(0f)
                        .setDuration(300).setStartDelay(position * 40L).start();
            }

            @Override
            public int getItemCount() { return users.size(); }
        };
    }

    private void loadUsers() {
        progressBar.setVisibility(View.VISIBLE);

        ApiService apiService = ApiClient.getApiService(this);
        apiService.getNearbyUsers(100, 0).enqueue(new Callback<ApiResponse<Map<String, Object>>>() {
            @Override
            public void onResponse(Call<ApiResponse<Map<String, Object>>> call,
                    Response<ApiResponse<Map<String, Object>>> response) {
                progressBar.setVisibility(View.GONE);

                if (response.isSuccessful() && response.body() != null && response.body().isStatus()) {
                    Map<String, Object> data = response.body().getData();
                    Gson gson = new Gson();
                    String json = gson.toJson(data.get("users"));
                    Type listType = new TypeToken<List<ExploreUser>>() {}.getType();
                    List<ExploreUser> result = gson.fromJson(json, listType);

                    if (result != null) {
                        users.clear();
                        users.addAll(result);
                        adapter.notifyDataSetChanged();
                    }
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<Map<String, Object>>> call, Throwable t) {
                progressBar.setVisibility(View.GONE);
            }
        });
    }

    private void showProfileDialog(ExploreUser user) {
        android.app.Dialog dialog = new android.app.Dialog(this);
        dialog.requestWindowFeature(android.view.Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_user_profile);

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(
                    new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));
            dialog.getWindow().setLayout(
                    android.view.ViewGroup.LayoutParams.WRAP_CONTENT,
                    android.view.ViewGroup.LayoutParams.WRAP_CONTENT);
        }

        // Views
        ImageView ivPhoto = dialog.findViewById(R.id.iv_profile_photo);
        TextView tvNameAge = dialog.findViewById(R.id.tv_name_age);
        TextView tvLocation = dialog.findViewById(R.id.tv_location);
        TextView tvUsername = dialog.findViewById(R.id.tv_username);
        TextView tvBio = dialog.findViewById(R.id.tv_bio);
        TextView tvHobbies = dialog.findViewById(R.id.tv_hobbies);
        TextView tvDreams = dialog.findViewById(R.id.tv_dreams);
        View layoutBio = dialog.findViewById(R.id.layout_bio);
        View layoutHobbies = dialog.findViewById(R.id.layout_hobbies);
        View layoutDreams = dialog.findViewById(R.id.layout_dreams);
        View btnClose = dialog.findViewById(R.id.btn_close);

        // Set data
        String nameAge = user.getName();
        if (user.getAge() > 0) nameAge += ", " + user.getAge();
        tvNameAge.setText(nameAge);

        // Location
        String location = "";
        if (user.getCity() != null && !user.getCity().isEmpty()) location = user.getCity();
        if (user.getCountry() != null && !user.getCountry().isEmpty()) {
            location += location.isEmpty() ? user.getCountry() : ", " + user.getCountry();
        }
        if (user.getDistance() > 0) {
            location += " (" + String.format("%.1f km", user.getDistance()) + ")";
        }
        tvLocation.setText(location.isEmpty() ? "Unknown location" : location);

        // Username
        if (user.getUsername() != null && !user.getUsername().isEmpty()) {
            tvUsername.setText("@" + user.getUsername());
        } else {
            tvUsername.setVisibility(View.GONE);
        }

        // Bio
        if (user.getBio() != null && !user.getBio().isEmpty()) {
            tvBio.setText(user.getBio());
            layoutBio.setVisibility(View.VISIBLE);
        }

        // Hobbies
        if (user.getHobbies() != null && !user.getHobbies().isEmpty()) {
            tvHobbies.setText(user.getHobbies());
            layoutHobbies.setVisibility(View.VISIBLE);
        }

        // Dreams
        if (user.getDreams() != null && !user.getDreams().isEmpty()) {
            tvDreams.setText(user.getDreams());
            layoutDreams.setVisibility(View.VISIBLE);
        }

        // Photo
        String photoUrl = user.getPrimaryPhoto();
        if (photoUrl != null && !photoUrl.isEmpty()) {
            String fullUrl = photoUrl.startsWith("http") ? photoUrl : Constants.IMAGE_BASE_URL + photoUrl;
            Glide.with(this)
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
