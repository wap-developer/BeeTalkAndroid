package com.beeflirt.beetalk.activities;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.Toolbar;

import com.beeflirt.beetalk.R;
import com.beeflirt.beetalk.api.ApiClient;
import com.beeflirt.beetalk.api.ApiService;
import com.beeflirt.beetalk.models.ApiResponse;
import com.beeflirt.beetalk.utils.BillingHelper;
import com.beeflirt.beetalk.utils.PremiumManager;
import com.google.android.material.button.MaterialButton;
import com.google.gson.JsonObject;

import java.util.Arrays;
import java.util.Map;
import java.util.UUID;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Paywall + boost.
 * Tries Play Billing first; falls back to dev token if products not on Play Console.
 */
public class PremiumActivity extends BaseActivity implements BillingHelper.PurchaseListener {

    private TextView tvStatus, tvStatusSub, tvBillingNote, tvBoostStatus;
    private MaterialButton btnWeekly, btnMonthly, btnYearly, btnBoost;
    private PremiumManager premiumManager;
    private ApiService api;
    private BillingHelper billing;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_premium);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        toolbar.setNavigationOnClickListener(v -> finish());

        premiumManager = PremiumManager.getInstance(this);
        api = ApiClient.getApiService(this);

        tvStatus = findViewById(R.id.tv_status);
        tvStatusSub = findViewById(R.id.tv_status_sub);
        tvBillingNote = findViewById(R.id.tv_billing_note);
        tvBoostStatus = findViewById(R.id.tv_boost_status);
        btnWeekly = findViewById(R.id.btn_weekly);
        btnMonthly = findViewById(R.id.btn_monthly);
        btnYearly = findViewById(R.id.btn_yearly);
        btnBoost = findViewById(R.id.btn_boost);

        btnWeekly.setOnClickListener(v -> buy(premiumManager.getProductWeekly()));
        btnMonthly.setOnClickListener(v -> buy(premiumManager.getProductMonthly()));
        btnYearly.setOnClickListener(v -> buy(premiumManager.getProductYearly()));
        if (btnBoost != null) {
            btnBoost.setOnClickListener(v -> onBoostClick());
        }

        billing = new BillingHelper(this, this);
        billing.start();

        refreshStatus();
        loadEntitlements();
    }

    @Override
    protected void onDestroy() {
        if (billing != null) billing.destroy();
        super.onDestroy();
    }

    private void buy(String productId) {
        Toast.makeText(this, "Opening store…", Toast.LENGTH_SHORT).show();
        billing.buy(productId);
    }

    /** Premium: free boost API. Free: buy boost product via Play / dev. */
    private void onBoostClick() {
        if (premiumManager.isPremium()) {
            Toast.makeText(this, "Activating boost…", Toast.LENGTH_SHORT).show();
            api.activateBoost().enqueue(new Callback<ApiResponse<Map<String, Object>>>() {
                @Override
                public void onResponse(Call<ApiResponse<Map<String, Object>>> call,
                                       Response<ApiResponse<Map<String, Object>>> response) {
                    if (!response.isSuccessful() || response.body() == null
                            || response.body().getData() == null) {
                        Toast.makeText(PremiumActivity.this,
                                "Boost failed — try again later", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    applyEntitlements(response.body().getData());
                    refreshStatus();
                    Toast.makeText(PremiumActivity.this, "Boost active!", Toast.LENGTH_SHORT).show();
                }

                @Override
                public void onFailure(Call<ApiResponse<Map<String, Object>>> call, Throwable t) {
                    Toast.makeText(PremiumActivity.this, "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
            return;
        }
        buy(premiumManager.getProductBoost());
    }

    @Override
    public void onBillingReady(boolean ok) {
        runOnUiThread(() -> {
            if (tvBillingNote != null) {
                tvBillingNote.setText(ok
                        ? "Google Play Billing ready. Products must exist in Play Console."
                        : "Play Billing unavailable — using dev activate for testing.");
            }
            if (ok) {
                billing.queryProducts(Arrays.asList(
                        premiumManager.getProductWeekly(),
                        premiumManager.getProductMonthly(),
                        premiumManager.getProductYearly(),
                        premiumManager.getProductBoost()
                ));
            }
        });
    }

    @Override
    public void onPurchaseSuccess(@NonNull String productId, @NonNull String purchaseToken) {
        runOnUiThread(() -> verifyOnServer(productId, purchaseToken));
    }

    @Override
    public void onPurchaseError(@NonNull String message) {
        runOnUiThread(() -> Toast.makeText(this, message, Toast.LENGTH_SHORT).show());
    }

    @Override
    public void onDevFallback(@NonNull String productId, @NonNull String reason) {
        runOnUiThread(() -> {
            Toast.makeText(this, "Dev activate: " + reason, Toast.LENGTH_SHORT).show();
            String token = "dev_" + productId + "_" + UUID.randomUUID();
            verifyOnServer(productId, token);
        });
    }

    private void verifyOnServer(String productId, String token) {
        JsonObject body = new JsonObject();
        body.addProperty("product_id", productId);
        body.addProperty("purchase_token", token);
        body.addProperty("platform", "android");

        boolean isBoost = productId != null && productId.toLowerCase().contains("boost");
        Toast.makeText(this, isBoost ? "Activating boost…" : "Activating…", Toast.LENGTH_SHORT).show();

        api.verifyPurchase(body).enqueue(new Callback<ApiResponse<Map<String, Object>>>() {
            @Override
            public void onResponse(Call<ApiResponse<Map<String, Object>>> call,
                                   Response<ApiResponse<Map<String, Object>>> response) {
                if (!response.isSuccessful() || response.body() == null
                        || response.body().getData() == null) {
                    Toast.makeText(PremiumActivity.this, "Purchase failed", Toast.LENGTH_SHORT).show();
                    return;
                }
                applyEntitlements(response.body().getData());
                refreshStatus();
                Toast.makeText(PremiumActivity.this,
                        isBoost ? "Boost active!" : "Premium activated!",
                        Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onFailure(Call<ApiResponse<Map<String, Object>>> call, Throwable t) {
                Toast.makeText(PremiumActivity.this, "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void refreshStatus() {
        if (premiumManager.isPremium()) {
            tvStatus.setText("Premium active");
            String until = premiumManager.getPremiumUntil();
            tvStatusSub.setText(until != null ? "Until " + until + " · Ad-free on" : "Ad-free on");
            btnWeekly.setEnabled(false);
            btnMonthly.setEnabled(false);
            btnYearly.setEnabled(false);
        } else {
            tvStatus.setText("Free plan");
            tvStatusSub.setText("Upgrade to see who likes you, more super likes, no banner ads.");
            btnWeekly.setEnabled(true);
            btnMonthly.setEnabled(true);
            btnYearly.setEnabled(true);
        }

        if (tvBoostStatus != null) {
            if (premiumManager.isBoostActive()) {
                tvBoostStatus.setVisibility(View.VISIBLE);
                tvBoostStatus.setText("Boost active until " + premiumManager.getBoostUntil());
                if (btnBoost != null) btnBoost.setText("Extend boost");
            } else {
                tvBoostStatus.setVisibility(View.VISIBLE);
                tvBoostStatus.setText("Boost puts you on top of the swipe stack for 30 minutes.");
                if (btnBoost != null) btnBoost.setText("Boost profile 30 min");
            }
        }
    }

    private void loadEntitlements() {
        api.getEntitlements().enqueue(new Callback<ApiResponse<Map<String, Object>>>() {
            @Override
            public void onResponse(Call<ApiResponse<Map<String, Object>>> call,
                                   Response<ApiResponse<Map<String, Object>>> response) {
                if (!response.isSuccessful() || response.body() == null || response.body().getData() == null) {
                    return;
                }
                applyEntitlements(response.body().getData());
                // Prefetch products for Billing
                if (billing != null && billing.isReady()) {
                    billing.queryProducts(Arrays.asList(
                            premiumManager.getProductWeekly(),
                            premiumManager.getProductMonthly(),
                            premiumManager.getProductYearly(),
                            premiumManager.getProductBoost()
                    ));
                }
                refreshStatus();
            }

            @Override
            public void onFailure(Call<ApiResponse<Map<String, Object>>> call, Throwable t) {
                // keep cache
            }
        });
    }

    @SuppressWarnings("unchecked")
    private void applyEntitlements(Map<String, Object> data) {
        boolean premium = asBool(data.get("is_premium"));
        String until = data.get("premium_until") != null ? String.valueOf(data.get("premium_until")) : null;
        String boostUntil = data.get("boost_until") != null ? String.valueOf(data.get("boost_until")) : null;

        int superLike = 5;
        int undo = 1;
        Object quotas = data.get("quotas");
        if (quotas instanceof Map) {
            Map<String, Object> q = (Map<String, Object>) quotas;
            superLike = asInt(q.get("super_like_daily"), 5);
            undo = asInt(q.get("undo_daily"), 1);
        }

        String weekly = "premium_weekly";
        String monthly = "premium_monthly";
        String yearly = "premium_yearly";
        String boost = "boost_30m";
        Object products = data.get("products");
        if (products instanceof Map) {
            Map<String, Object> p = (Map<String, Object>) products;
            if (p.get("weekly") != null) weekly = String.valueOf(p.get("weekly"));
            if (p.get("monthly") != null) monthly = String.valueOf(p.get("monthly"));
            if (p.get("yearly") != null) yearly = String.valueOf(p.get("yearly"));
            if (p.get("boost") != null) boost = String.valueOf(p.get("boost"));
        }

        boolean adFree = premium;
        Object features = data.get("features");
        if (features instanceof Map) {
            Object af = ((Map<?, ?>) features).get("ad_free");
            if (af != null) adFree = asBool(af);
        }

        boolean premiumEnabled = asBool(data.get("premium_enabled"));
        premiumManager.applySnapshot(premiumEnabled, premium, until, superLike, undo,
                weekly, monthly, yearly, boost, adFree, boostUntil);
    }

    private static boolean asBool(Object o) {
        if (o instanceof Boolean) return (Boolean) o;
        if (o instanceof Number) return ((Number) o).intValue() == 1;
        return false;
    }

    private static int asInt(Object o, int def) {
        if (o instanceof Number) return ((Number) o).intValue();
        try { return Integer.parseInt(String.valueOf(o)); } catch (Exception e) { return def; }
    }
}
