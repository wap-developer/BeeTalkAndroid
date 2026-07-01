package com.beeflirt.beetalk.activities;

import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.beeflirt.beetalk.dialogs.LoadingDialog;

import com.beeflirt.beetalk.R;
import com.beeflirt.beetalk.api.ApiClient;
import com.beeflirt.beetalk.api.ApiService;
import com.beeflirt.beetalk.models.ApiResponse;
import com.beeflirt.beetalk.utils.SharedPrefManager;
import com.google.gson.JsonObject;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class VerifyOtpActivity extends BaseActivity {

    private EditText etOtp;
    private Button btnVerify;
    private TextView tvUserEmail, tvResend, tvCountdown;
    private LoadingDialog loadingDialog;

    private ApiService apiService;
    private String userEmail;
    private CountDownTimer countDownTimer;
    private static final long COUNTDOWN_TIME = 60000; // 60 seconds

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_verify_otp);

        // Get email from intent
        userEmail = getIntent().getStringExtra("email");
        if (userEmail == null) {
            Toast.makeText(this, "Error: Email not provided", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Initialize views
        etOtp = findViewById(R.id.et_otp);
        btnVerify = findViewById(R.id.btn_verify);
        tvUserEmail = findViewById(R.id.tv_user_email);
        tvResend = findViewById(R.id.tv_resend);
        tvCountdown = findViewById(R.id.tv_countdown);
        loadingDialog = new LoadingDialog(this);

        // Initialize API
        apiService = ApiClient.getApiService(this);

        // Set email
        tvUserEmail.setText(userEmail);

        // Verify button click
        btnVerify.setOnClickListener(v -> verifyOtp());

        // Resend OTP click
        tvResend.setOnClickListener(v -> resendOtp());

        // Start countdown timer
        startCountdown();
    }

    private void startCountdown() {
        tvResend.setEnabled(false);
        tvResend.setAlpha(0.5f);
        tvCountdown.setVisibility(View.VISIBLE);

        countDownTimer = new CountDownTimer(COUNTDOWN_TIME, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                long secondsRemaining = millisUntilFinished / 1000;
                tvCountdown.setText("Resend available in " + secondsRemaining + "s");
            }

            @Override
            public void onFinish() {
                tvCountdown.setVisibility(View.GONE);
                tvResend.setEnabled(true);
                tvResend.setAlpha(1.0f);
            }
        }.start();
    }

    private void verifyOtp() {
        String otp = etOtp.getText().toString().trim();

        // Validation
        if (TextUtils.isEmpty(otp)) {
            etOtp.setError("Please enter OTP");
            etOtp.requestFocus();
            return;
        }

        if (otp.length() != 6) {
            etOtp.setError("OTP must be 6 digits");
            etOtp.requestFocus();
            return;
        }

        setLoading(true);

        JsonObject data = new JsonObject();
        data.addProperty("email", userEmail);
        data.addProperty("otp", otp);

        apiService.verifyOtp(data).enqueue(new Callback<ApiResponse<Object>>() {
            @Override
            public void onResponse(Call<ApiResponse<Object>> call, Response<ApiResponse<Object>> response) {
                setLoading(false);

                if (response.isSuccessful() && response.body() != null) {
                    ApiResponse<Object> apiResponse = response.body();

                    if (apiResponse.isStatus()) {
                        Toast.makeText(VerifyOtpActivity.this, "Email verified successfully!", Toast.LENGTH_SHORT)
                                .show();

                        // Auto-login after verification
                        autoLogin();
                    } else {
                        Toast.makeText(VerifyOtpActivity.this, apiResponse.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(VerifyOtpActivity.this, "Verification failed", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<Object>> call, Throwable t) {
                setLoading(false);
                Toast.makeText(VerifyOtpActivity.this, "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void autoLogin() {
        // Get password from intent (passed from RegisterActivity)
        String password = getIntent().getStringExtra("password");

        if (password == null) {
            // If no password, just go to login screen
            Intent intent = new Intent(VerifyOtpActivity.this, LoginActivity.class);
            intent.putExtra("email", userEmail);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
            return;
        }

        setLoading(true);

        JsonObject loginData = new JsonObject();
        loginData.addProperty("email", userEmail);
        loginData.addProperty("password", password);

        apiService.login(loginData).enqueue(new Callback<ApiResponse<java.util.Map<String, Object>>>() {
            @Override
            public void onResponse(Call<ApiResponse<java.util.Map<String, Object>>> call,
                    Response<ApiResponse<java.util.Map<String, Object>>> response) {
                setLoading(false);

                if (response.isSuccessful() && response.body() != null) {
                    ApiResponse<java.util.Map<String, Object>> apiResponse = response.body();

                    if (apiResponse.isStatus()) {
                        java.util.Map<String, Object> data = apiResponse.getData();

                        String token = (String) data.get("token");

                        // Handle user_id
                        int userId;
                        Object userIdObj = data.get("user_id");
                        if (userIdObj instanceof Double) {
                            userId = ((Double) userIdObj).intValue();
                        } else if (userIdObj instanceof Integer) {
                            userId = (Integer) userIdObj;
                        } else {
                            userId = Integer.parseInt(userIdObj.toString());
                        }

                        // Save session
                        SharedPrefManager prefManager = SharedPrefManager.getInstance(VerifyOtpActivity.this);
                        prefManager.saveUserSession(token, userId, userEmail);

                        // Navigate to ProfileSetupActivity
                        Intent intent = new Intent(VerifyOtpActivity.this, ProfileSetupActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                        finish();
                    } else {
                        // Login failed, go to login screen
                        Intent intent = new Intent(VerifyOtpActivity.this, LoginActivity.class);
                        intent.putExtra("email", userEmail);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                        finish();
                    }
                } else {
                    // Login failed, go to login screen
                    Intent intent = new Intent(VerifyOtpActivity.this, LoginActivity.class);
                    intent.putExtra("email", userEmail);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<java.util.Map<String, Object>>> call, Throwable t) {
                setLoading(false);
                // Login failed, go to login screen
                Intent intent = new Intent(VerifyOtpActivity.this, LoginActivity.class);
                intent.putExtra("email", userEmail);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
            }
        });
    }

    private void resendOtp() {
        setLoading(true);

        JsonObject data = new JsonObject();
        data.addProperty("email", userEmail);

        apiService.resendVerification(data).enqueue(new Callback<ApiResponse<Object>>() {
            @Override
            public void onResponse(Call<ApiResponse<Object>> call, Response<ApiResponse<Object>> response) {
                setLoading(false);

                if (response.isSuccessful() && response.body() != null) {
                    ApiResponse<Object> apiResponse = response.body();

                    if (apiResponse.isStatus()) {
                        Toast.makeText(VerifyOtpActivity.this, "OTP sent! Check your email.", Toast.LENGTH_SHORT)
                                .show();
                        etOtp.setText("");
                        etOtp.requestFocus();

                        // Restart countdown
                        if (countDownTimer != null) {
                            countDownTimer.cancel();
                        }
                        startCountdown();
                    } else {
                        Toast.makeText(VerifyOtpActivity.this, apiResponse.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(VerifyOtpActivity.this, "Failed to resend OTP", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<Object>> call, Throwable t) {
                setLoading(false);
                Toast.makeText(VerifyOtpActivity.this, "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setLoading(boolean isLoading) {
        if (isLoading) {
            loadingDialog.show("Verifying Code…", "Please wait a moment");
            btnVerify.setEnabled(false);
            tvResend.setEnabled(false);
        } else {
            loadingDialog.dismiss();
            btnVerify.setEnabled(true);
            // Don't enable resend if countdown is active
            if (countDownTimer == null || tvCountdown.getVisibility() == View.GONE) {
                tvResend.setEnabled(true);
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
    }
}
