package com.beeflirt.beetalk.activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.beeflirt.beetalk.dialogs.LoadingDialog;

import com.beeflirt.beetalk.MainActivity;
import com.beeflirt.beetalk.R;
import com.beeflirt.beetalk.api.ApiClient;
import com.beeflirt.beetalk.api.ApiService;
import com.beeflirt.beetalk.models.ApiResponse;
import com.beeflirt.beetalk.utils.AnimationHelper;
import com.beeflirt.beetalk.utils.SharedPrefManager;
import com.google.gson.JsonObject;

import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LoginActivity extends BaseActivity {

    private EditText etEmail, etPassword;
    private Button btnLogin;
    private TextView tvRegister;
    private LoadingDialog loadingDialog;

    private ApiService apiService;
    private SharedPrefManager prefManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // Apply EdgeToEdge insets
        androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            androidx.core.graphics.Insets systemBars = insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, 0, systemBars.right, systemBars.bottom);
            
            // Berikan margin top pada tombol back agar tidak tertutup status bar
            View btnBack = findViewById(R.id.btn_back);
            if (btnBack != null) {
                android.view.ViewGroup.MarginLayoutParams params = (android.view.ViewGroup.MarginLayoutParams) btnBack.getLayoutParams();
                params.topMargin = systemBars.top + (int) (10 * getResources().getDisplayMetrics().density);
                btnBack.setLayoutParams(params);
            }
            return insets;
        });

        // Initialize views
        etEmail = findViewById(R.id.et_email);
        etPassword = findViewById(R.id.et_password);
        btnLogin = findViewById(R.id.btn_login);
        tvRegister = findViewById(R.id.tv_register);
        loadingDialog = new LoadingDialog(this);

        // Initialize API and SharedPreferences
        apiService = ApiClient.getApiService(this);
        prefManager = SharedPrefManager.getInstance(this);

        // Login button click
        btnLogin.setOnClickListener(v -> attemptLogin());

        // Register text click
        tvRegister.setOnClickListener(v -> {
            startActivity(new Intent(LoginActivity.this, RegisterActivity.class));
        });

        // Forgot password click
        TextView tvForgotPassword = findViewById(R.id.tv_forgot_password);
        tvForgotPassword.setOnClickListener(v -> {
            startActivity(new Intent(LoginActivity.this, ForgotPasswordActivity.class));
        });

        // Back button click
        View btnBack = findViewById(R.id.btn_back);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        // Apply entrance animations
        setupAnimations();
    }

    private void setupAnimations() {
        // Logo/Title animation
        TextView tvTitle = findViewById(R.id.tv_title);
        if (tvTitle != null) {
            AnimationHelper.scaleIn(tvTitle, 0);
        }

        // Email field animation
        AnimationHelper.slideUpFadeIn(etEmail, 100);

        // Password field animation
        AnimationHelper.slideUpFadeIn(etPassword, 200);

        // Forgot password animation
        TextView tvForgotPassword = findViewById(R.id.tv_forgot_password);
        if (tvForgotPassword != null) {
            AnimationHelper.slideUpFadeIn(tvForgotPassword, 300);
        }

        // Login button animation
        AnimationHelper.slideUpFadeIn(btnLogin, 400);

        // Register text animation
        AnimationHelper.slideUpFadeIn(tvRegister, 500);

        // Add button press effect
        btnLogin.setOnTouchListener((v, event) -> {
            if (event.getAction() == android.view.MotionEvent.ACTION_DOWN) {
                v.animate().scaleX(0.95f).scaleY(0.95f).setDuration(100).start();
            } else if (event.getAction() == android.view.MotionEvent.ACTION_UP ||
                    event.getAction() == android.view.MotionEvent.ACTION_CANCEL) {
                v.animate().scaleX(1f).scaleY(1f).setDuration(100).start();
            }
            return false;
        });
    }

    private void attemptLogin() {
        // Get input values
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        // Validate inputs
        if (TextUtils.isEmpty(email)) {
            etEmail.setError("Email is required");
            etEmail.requestFocus();
            AnimationHelper.animateShake(etEmail);
            return;
        }

        if (TextUtils.isEmpty(password)) {
            etPassword.setError("Password is required");
            etPassword.requestFocus();
            return;
        }

        // Show progress
        setLoading(true);

        // Create request body
        JsonObject loginData = new JsonObject();
        loginData.addProperty("email", email);
        loginData.addProperty("password", password);

        // Make API call
        apiService.login(loginData).enqueue(new Callback<ApiResponse<Map<String, Object>>>() {
            @Override
            public void onResponse(Call<ApiResponse<Map<String, Object>>> call,
                    Response<ApiResponse<Map<String, Object>>> response) {
                setLoading(false);

                if (response.isSuccessful() && response.body() != null) {
                    ApiResponse<Map<String, Object>> apiResponse = response.body();

                    if (apiResponse.isStatus()) {
                        // Login successful
                        Map<String, Object> data = apiResponse.getData();

                        String token = (String) data.get("token");
                        String userEmail = (String) data.get("email");

                        // Parse user_id safely (Gson returns Double for JSON numbers)
                        int userId = -1;
                        Object userIdObj = data.get("user_id");
                        if (userIdObj instanceof Number) {
                            userId = ((Number) userIdObj).intValue();
                        } else if (userIdObj instanceof String) {
                            try { userId = Integer.parseInt((String) userIdObj); } catch (NumberFormatException ignored) {}
                        }

                        // Save session
                        prefManager.saveUserSession(token, userId, userEmail);

                        // Navigate to MainActivity
                        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                        finish();

                        Toast.makeText(LoginActivity.this, "Login successful!", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(LoginActivity.this, apiResponse.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(LoginActivity.this, "Login failed. Please try again.", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<Map<String, Object>>> call, Throwable t) {
                setLoading(false);
                Toast.makeText(LoginActivity.this, "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setLoading(boolean isLoading) {
        if (isLoading) {
            loadingDialog.show("Signing in…", "Please wait a moment");
            btnLogin.setEnabled(false);
        } else {
            loadingDialog.dismiss();
            btnLogin.setEnabled(true);
        }
    }
}
