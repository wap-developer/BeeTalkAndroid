package com.beeflirt.beetalk.api;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.beeflirt.beetalk.activities.LoginActivity;
import com.beeflirt.beetalk.utils.Constants;
import com.beeflirt.beetalk.utils.SharedPrefManager;

import org.json.JSONObject;

import java.io.IOException;

import okhttp3.Interceptor;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class AuthInterceptor implements Interceptor {
    private static final String TAG = "AuthInterceptor";
    private final Context context;
    private final Object refreshLock = new Object();
    private volatile boolean isRefreshing = false;

    public AuthInterceptor(Context context) {
        this.context = context;
    }

    @Override
    public Response intercept(Chain chain) throws IOException {
        Request originalRequest = chain.request();

        String token = SharedPrefManager.getInstance(context).getToken();

        Request newRequest = originalRequest;
        if (token != null && !token.isEmpty()) {
            newRequest = originalRequest.newBuilder()
                    .header("Authorization", "Bearer " + token)
                    .build();
        }

        Response response = chain.proceed(newRequest);

        if (response.code() == 401 && !isRefreshRequest(originalRequest)) {
            synchronized (refreshLock) {
                // Check if another thread already refreshed
                String currentToken = SharedPrefManager.getInstance(context).getToken();
                if (currentToken != null && !currentToken.equals(token)) {
                    // Token was already refreshed by another thread, retry with new token
                    response.close();
                    Request retryRequest = originalRequest.newBuilder()
                            .header("Authorization", "Bearer " + currentToken)
                            .build();
                    return chain.proceed(retryRequest);
                }

                // Attempt refresh
                String newToken = attemptTokenRefresh(token);
                if (newToken != null) {
                    Log.d(TAG, "Token refreshed successfully");
                    SharedPrefManager.getInstance(context).saveToken(newToken);

                    // Retry original request with new token
                    response.close();
                    Request retryRequest = originalRequest.newBuilder()
                            .header("Authorization", "Bearer " + newToken)
                            .build();
                    return chain.proceed(retryRequest);
                } else {
                    // Refresh failed — force logout
                    Log.w(TAG, "Token refresh failed, logging out");
                    SharedPrefManager.getInstance(context).clearSession();
                    Intent intent = new Intent(context, LoginActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    context.startActivity(intent);
                }
            }
        }

        return response;
    }

    private boolean isRefreshRequest(Request request) {
        return request.url().encodedPath().contains("auth/refresh");
    }

    private String attemptTokenRefresh(String currentToken) {
        try {
            OkHttpClient client = new OkHttpClient.Builder().build();

            Request refreshRequest = new Request.Builder()
                    .url(Constants.BASE_URL + "auth/refresh")
                    .post(RequestBody.create("", MediaType.parse("application/json")))
                    .header("Authorization", "Bearer " + currentToken)
                    .build();

            Response refreshResponse = client.newCall(refreshRequest).execute();

            if (refreshResponse.isSuccessful() && refreshResponse.body() != null) {
                String body = refreshResponse.body().string();
                JSONObject json = new JSONObject(body);

                if (json.optBoolean("status", false)) {
                    JSONObject data = json.optJSONObject("data");
                    if (data != null && data.has("token")) {
                        return data.getString("token");
                    }
                }
            }
            refreshResponse.close();
        } catch (Exception e) {
            Log.e(TAG, "Token refresh error: " + e.getMessage());
        }
        return null;
    }
}
