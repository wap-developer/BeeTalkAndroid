package com.beeflirt.beetalk.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.beeflirt.beetalk.R;
import com.beeflirt.beetalk.adapters.WhoLikedAdapter;
import com.beeflirt.beetalk.api.ApiClient;
import com.beeflirt.beetalk.api.ApiService;
import com.beeflirt.beetalk.models.ApiResponse;
import com.beeflirt.beetalk.utils.PremiumManager;
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class WhoLikedMeActivity extends BaseActivity {

    private SwipeRefreshLayout swipeRefresh;
    private RecyclerView rvLikes;
    private LinearLayout layoutEmpty;
    private TextView tvUpsell;
    private MaterialButton btnUnlock;
    private WhoLikedAdapter adapter;
    private final List<Map<String, Object>> items = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_who_liked_me);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        toolbar.setNavigationOnClickListener(v -> finish());

        swipeRefresh = findViewById(R.id.swipeRefresh);
        rvLikes = findViewById(R.id.rvLikes);
        layoutEmpty = findViewById(R.id.layoutEmpty);
        tvUpsell = findViewById(R.id.tv_upsell);
        btnUnlock = findViewById(R.id.btn_unlock);

        adapter = new WhoLikedAdapter(items, item -> {
            if (asBool(item.get("blurred")) && PremiumManager.getInstance(this).isPremiumFeatureEnabled()) {
                startActivity(new Intent(this, PremiumActivity.class));
            }
        });
        rvLikes.setLayoutManager(new LinearLayoutManager(this));
        rvLikes.setAdapter(adapter);

        btnUnlock.setOnClickListener(v -> {
            if (PremiumManager.getInstance(this).isPremiumFeatureEnabled()) {
                startActivity(new Intent(this, PremiumActivity.class));
            }
        });

        swipeRefresh.setOnRefreshListener(this::load);
        load();
    }

    @Override
    protected void onResume() {
        super.onResume();
        load();
    }

    private void load() {
        swipeRefresh.setRefreshing(true);
        ApiService api = ApiClient.getApiService(this);
        api.getWhoLikedMe().enqueue(new Callback<ApiResponse<Map<String, Object>>>() {
            @Override
            public void onResponse(Call<ApiResponse<Map<String, Object>>> call,
                                   Response<ApiResponse<Map<String, Object>>> response) {
                swipeRefresh.setRefreshing(false);
                if (!response.isSuccessful() || response.body() == null || response.body().getData() == null) {
                    Toast.makeText(WhoLikedMeActivity.this, "Failed to load likes", Toast.LENGTH_SHORT).show();
                    return;
                }

                Map<String, Object> data = response.body().getData();
                boolean premiumEnabled = asBool(data.get("premium_enabled"));
                boolean premium = asBool(data.get("is_premium"));
                boolean unlocked = asBool(data.get("unlocked"));
                PremiumManager.getInstance(WhoLikedMeActivity.this)
                        .applySnapshot(premiumEnabled,
                                premium,
                                null,
                                PremiumManager.getInstance(WhoLikedMeActivity.this).getSuperLikeDaily(),
                                PremiumManager.getInstance(WhoLikedMeActivity.this).getUndoDaily(),
                                null, null, null, null, premium, null);

                int total = asInt(data.get("total"));
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> likes = (List<Map<String, Object>>) data.get("likes");

                items.clear();
                if (likes != null) items.addAll(likes);
                adapter.notifyDataSetChanged();

                boolean empty = items.isEmpty() && total == 0;
                layoutEmpty.setVisibility(empty ? View.VISIBLE : View.GONE);
                rvLikes.setVisibility(empty ? View.GONE : View.VISIBLE);

                if (premiumEnabled && !premium && !unlocked) {
                    tvUpsell.setVisibility(View.VISIBLE);
                    btnUnlock.setVisibility(View.VISIBLE);
                    Object upsell = data.get("upsell");
                    if (upsell instanceof Map) {
                        Object msg = ((Map<?, ?>) upsell).get("message");
                        if (msg != null) tvUpsell.setText(String.valueOf(msg));
                    } else if (total > 0) {
                        tvUpsell.setText(total + " people like you. Unlock Premium to see them.");
                    }
                } else {
                    tvUpsell.setVisibility(View.GONE);
                    btnUnlock.setVisibility(View.GONE);
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<Map<String, Object>>> call, Throwable t) {
                swipeRefresh.setRefreshing(false);
                Toast.makeText(WhoLikedMeActivity.this, "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private static boolean asBool(Object o) {
        if (o instanceof Boolean) return (Boolean) o;
        if (o instanceof Number) return ((Number) o).intValue() == 1;
        return false;
    }

    private static int asInt(Object o) {
        if (o instanceof Number) return ((Number) o).intValue();
        try { return Integer.parseInt(String.valueOf(o)); } catch (Exception e) { return 0; }
    }
}
