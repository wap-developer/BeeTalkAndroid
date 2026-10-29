package com.beeflirt.beetalk.utils;

import android.app.Activity;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.android.billingclient.api.AcknowledgePurchaseParams;
import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.BillingClientStateListener;
import com.android.billingclient.api.BillingFlowParams;
import com.android.billingclient.api.BillingResult;
import com.android.billingclient.api.PendingPurchasesParams;
import com.android.billingclient.api.ProductDetails;
import com.android.billingclient.api.Purchase;
import com.android.billingclient.api.QueryProductDetailsParams;
import com.android.billingclient.api.QueryPurchasesParams;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Thin Play Billing wrapper.
 * - Query product details
 * - Launch purchase flow
 * - Acknowledge purchases
 * Falls back to listener.onDevFallback when Billing not ready / product missing.
 */
public class BillingHelper {

    public interface PurchaseListener {
        void onBillingReady(boolean ok);

        /** Real Play purchase token ready to send to backend. */
        void onPurchaseSuccess(@NonNull String productId, @NonNull String purchaseToken);

        void onPurchaseError(@NonNull String message);

        /**
         * Called when Play Billing cannot complete (no product, not connected).
         * UI may use dev stub token for local testing.
         */
        void onDevFallback(@NonNull String productId, @NonNull String reason);
    }

    private static final String TAG = "BillingHelper";

    private final Activity activity;
    private final PurchaseListener listener;
    private BillingClient client;
    private final Map<String, ProductDetails> productCache = new HashMap<>();
    private boolean ready;

    public BillingHelper(@NonNull Activity activity, @NonNull PurchaseListener listener) {
        this.activity = activity;
        this.listener = listener;
    }

    public void start() {
        client = BillingClient.newBuilder(activity)
                .setListener((billingResult, purchases) -> {
                    if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK
                            && purchases != null) {
                        for (Purchase p : purchases) {
                            handlePurchase(p);
                        }
                    } else if (billingResult.getResponseCode()
                            == BillingClient.BillingResponseCode.USER_CANCELED) {
                        listener.onPurchaseError("Purchase canceled");
                    } else if (billingResult.getResponseCode()
                            != BillingClient.BillingResponseCode.OK) {
                        listener.onPurchaseError("Billing error: " + billingResult.getDebugMessage());
                    }
                })
                .enablePendingPurchases(
                        PendingPurchasesParams.newBuilder().enableOneTimeProducts().build())
                .build();

        client.startConnection(new BillingClientStateListener() {
            @Override
            public void onBillingSetupFinished(@NonNull BillingResult billingResult) {
                ready = billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK;
                Log.d(TAG, "setup finished: " + billingResult.getResponseCode()
                        + " " + billingResult.getDebugMessage());
                listener.onBillingReady(ready);
                if (ready) {
                    queryOwned();
                }
            }

            @Override
            public void onBillingServiceDisconnected() {
                ready = false;
                Log.w(TAG, "billing disconnected");
            }
        });
    }

    public void queryProducts(@NonNull List<String> productIds) {
        if (!ready || client == null || productIds.isEmpty()) return;

        List<QueryProductDetailsParams.Product> products = new ArrayList<>();
        for (String id : productIds) {
            products.add(QueryProductDetailsParams.Product.newBuilder()
                    .setProductId(id)
                    .setProductType(BillingClient.ProductType.INAPP)
                    .build());
        }

        QueryProductDetailsParams params = QueryProductDetailsParams.newBuilder()
                .setProductList(products)
                .build();

        client.queryProductDetailsAsync(params, (billingResult, productDetailsList) -> {
            if (billingResult.getResponseCode() != BillingClient.BillingResponseCode.OK
                    || productDetailsList == null) {
                Log.w(TAG, "query products failed: " + billingResult.getDebugMessage());
                return;
            }
            for (ProductDetails pd : productDetailsList) {
                productCache.put(pd.getProductId(), pd);
            }
            Log.d(TAG, "cached products: " + productCache.keySet());
        });
    }

    public void buy(@NonNull String productId) {
        if (!ready || client == null) {
            listener.onDevFallback(productId, "Billing not ready");
            return;
        }

        ProductDetails details = productCache.get(productId);
        if (details == null) {
            // Try one-shot query then buy
            queryThenBuy(productId);
            return;
        }
        launch(details);
    }

    private void queryThenBuy(@NonNull String productId) {
        QueryProductDetailsParams params = QueryProductDetailsParams.newBuilder()
                .setProductList(Collections.singletonList(
                        QueryProductDetailsParams.Product.newBuilder()
                                .setProductId(productId)
                                .setProductType(BillingClient.ProductType.INAPP)
                                .build()))
                .build();

        client.queryProductDetailsAsync(params, (billingResult, list) -> {
            if (billingResult.getResponseCode() != BillingClient.BillingResponseCode.OK
                    || list == null || list.isEmpty()) {
                listener.onDevFallback(productId,
                        "Product not found on Play: " + productId);
                return;
            }
            ProductDetails pd = list.get(0);
            productCache.put(pd.getProductId(), pd);
            launch(pd);
        });
    }

    private void launch(@NonNull ProductDetails details) {
        List<BillingFlowParams.ProductDetailsParams> productDetailsParamsList =
                Collections.singletonList(
                        BillingFlowParams.ProductDetailsParams.newBuilder()
                                .setProductDetails(details)
                                .build());

        BillingFlowParams flowParams = BillingFlowParams.newBuilder()
                .setProductDetailsParamsList(productDetailsParamsList)
                .build();

        BillingResult result = client.launchBillingFlow(activity, flowParams);
        if (result.getResponseCode() != BillingClient.BillingResponseCode.OK) {
            listener.onPurchaseError("Launch failed: " + result.getDebugMessage());
        }
    }

    private void handlePurchase(@NonNull Purchase purchase) {
        if (purchase.getPurchaseState() != Purchase.PurchaseState.PURCHASED) {
            return;
        }

        String productId = purchase.getProducts().isEmpty()
                ? ""
                : purchase.getProducts().get(0);
        String token = purchase.getPurchaseToken();

        if (!purchase.isAcknowledged()) {
            AcknowledgePurchaseParams ack = AcknowledgePurchaseParams.newBuilder()
                    .setPurchaseToken(token)
                    .build();
            client.acknowledgePurchase(ack, billingResult ->
                    Log.d(TAG, "ack: " + billingResult.getResponseCode()));
        }

        if (!productId.isEmpty() && token != null) {
            listener.onPurchaseSuccess(productId, token);
        }
    }

    private void queryOwned() {
        client.queryPurchasesAsync(
                QueryPurchasesParams.newBuilder()
                        .setProductType(BillingClient.ProductType.INAPP)
                        .build(),
                (billingResult, purchases) -> {
                    if (billingResult.getResponseCode() != BillingClient.BillingResponseCode.OK
                            || purchases == null) {
                        return;
                    }
                    for (Purchase p : purchases) {
                        if (p.getPurchaseState() == Purchase.PurchaseState.PURCHASED
                                && !p.isAcknowledged()) {
                            handlePurchase(p);
                        }
                    }
                });
    }

    public void destroy() {
        if (client != null) {
            client.endConnection();
            client = null;
        }
        ready = false;
    }

    public boolean isReady() {
        return ready;
    }

    @Nullable
    public ProductDetails getProduct(@NonNull String productId) {
        return productCache.get(productId);
    }
}
