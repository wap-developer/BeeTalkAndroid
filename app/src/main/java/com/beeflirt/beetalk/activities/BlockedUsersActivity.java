package com.beeflirt.beetalk.activities;

import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.beeflirt.beetalk.R;
import com.beeflirt.beetalk.adapters.BlockedUsersAdapter;
import com.beeflirt.beetalk.api.ApiClient;
import com.beeflirt.beetalk.models.ApiResponse;
import com.beeflirt.beetalk.api.ApiService;
import com.beeflirt.beetalk.models.BlockedUser;
import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class BlockedUsersActivity extends BaseActivity {

    private RecyclerView rvBlockedUsers;
    private SwipeRefreshLayout swipeRefresh;
    private LinearLayout layoutEmpty;
    private BlockedUsersAdapter adapter;
    private List<BlockedUser> blockedUsersList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_blocked_users);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        toolbar.setNavigationOnClickListener(v -> finish());

        rvBlockedUsers = findViewById(R.id.rvBlockedUsers);
        swipeRefresh = findViewById(R.id.swipeRefresh);
        layoutEmpty = findViewById(R.id.layoutEmpty);

        blockedUsersList = new ArrayList<>();
        adapter = new BlockedUsersAdapter(this, blockedUsersList, this::unblockUser);

        rvBlockedUsers.setLayoutManager(new LinearLayoutManager(this));
        rvBlockedUsers.setAdapter(adapter);

        swipeRefresh.setOnRefreshListener(this::loadBlockedUsers);

        loadBlockedUsers();
    }

    private void loadBlockedUsers() {
        swipeRefresh.setRefreshing(true);

        ApiService apiService = ApiClient.getApiService(this);
        apiService.getBlockedUsers().enqueue(new Callback<ApiResponse<Map<String, Object>>>() {
            @Override
            public void onResponse(Call<ApiResponse<Map<String, Object>>> call,
                    Response<ApiResponse<Map<String, Object>>> response) {
                swipeRefresh.setRefreshing(false);

                if (response.isSuccessful() && response.body() != null) {
                    Map<String, Object> data = response.body().getData();
                    List<Map<String, Object>> usersData = (List<Map<String, Object>>) data.get("blocked_users");

                    blockedUsersList.clear();
                    if (usersData != null) {
                        Gson gson = new Gson();
                        for (Map<String, Object> userData : usersData) {
                            String json = gson.toJson(userData);
                            BlockedUser user = gson.fromJson(json, BlockedUser.class);
                            blockedUsersList.add(user);
                        }
                    }

                    adapter.notifyDataSetChanged();
                    updateEmptyState();
                } else {
                    Toast.makeText(BlockedUsersActivity.this, "Failed to load blocked users", Toast.LENGTH_SHORT)
                            .show();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<Map<String, Object>>> call, Throwable t) {
                swipeRefresh.setRefreshing(false);
                Toast.makeText(BlockedUsersActivity.this, "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void unblockUser(BlockedUser user, int position) {
        android.app.Dialog dialog = new android.app.Dialog(this);
        dialog.requestWindowFeature(android.view.Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_unblock);

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(
                    new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));
            dialog.getWindow().setLayout(
                    android.view.ViewGroup.LayoutParams.WRAP_CONTENT,
                    android.view.ViewGroup.LayoutParams.WRAP_CONTENT);
        }

        android.widget.TextView tvTitle = dialog.findViewById(R.id.tv_unblock_title);
        android.widget.TextView tvMessage = dialog.findViewById(R.id.tv_unblock_message);
        de.hdodenhof.circleimageview.CircleImageView ivPhoto = dialog.findViewById(R.id.iv_unblock_photo);
        android.view.View btnCancel = dialog.findViewById(R.id.btn_cancel_unblock);
        android.view.View btnConfirm = dialog.findViewById(R.id.btn_confirm_unblock);

        tvTitle.setText("Unblock " + user.getName() + "?");
        tvMessage.setText(user.getName() + " will be able to see you again and appear in your swipe stack.");

        // Load photo
        if (user.getPrimaryPhoto() != null && !user.getPrimaryPhoto().isEmpty()) {
            String photoUrl = user.getPrimaryPhoto().startsWith("http")
                    ? user.getPrimaryPhoto()
                    : com.beeflirt.beetalk.utils.Constants.IMAGE_BASE_URL + user.getPrimaryPhoto();
            com.bumptech.glide.Glide.with(this)
                    .load(photoUrl)
                    .placeholder(R.drawable.ic_person)
                    .error(R.drawable.ic_person)
                    .centerCrop()
                    .into(ivPhoto);
        }

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        btnConfirm.setOnClickListener(v -> {
            dialog.dismiss();

            ApiService apiService = ApiClient.getApiService(this);
            apiService.unblockUser(user.getBlockedId()).enqueue(new Callback<ApiResponse<Object>>() {
                @Override
                public void onResponse(Call<ApiResponse<Object>> call, Response<ApiResponse<Object>> response) {
                    if (response.isSuccessful()) {
                        blockedUsersList.remove(position);
                        adapter.notifyItemRemoved(position);
                        adapter.notifyItemRangeChanged(position, blockedUsersList.size());
                        updateEmptyState();
                        Toast.makeText(BlockedUsersActivity.this, user.getName() + " unblocked",
                                Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(BlockedUsersActivity.this, "Failed to unblock", Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onFailure(Call<ApiResponse<Object>> call, Throwable t) {
                    Toast.makeText(BlockedUsersActivity.this, "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        });

        dialog.setCancelable(true);
        dialog.show();
    }

    private void updateEmptyState() {
        if (blockedUsersList.isEmpty()) {
            rvBlockedUsers.setVisibility(View.GONE);
            layoutEmpty.setVisibility(View.VISIBLE);
        } else {
            rvBlockedUsers.setVisibility(View.VISIBLE);
            layoutEmpty.setVisibility(View.GONE);
        }
    }
}
