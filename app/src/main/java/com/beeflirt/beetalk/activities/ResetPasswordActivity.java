package com.beeflirt.beetalk.activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.beeflirt.beetalk.dialogs.LoadingDialog;

import com.beeflirt.beetalk.R;
import com.beeflirt.beetalk.api.ApiClient;
import com.beeflirt.beetalk.api.ApiService;
import com.beeflirt.beetalk.models.ApiResponse;
import com.google.gson.JsonObject;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ResetPasswordActivity extends BaseActivity {

    private EditText etOtp;
    private Button btnVerifyOtp;
    private TextView tvResend;
    private LoadingDialog loadingDialog;
    private ImageView btnBack;

    private ApiService apiService;
    private String userEmail;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reset_password);

        // Get email from intent
        userEmail = getIntent().getStringExtra("email");
        if (userEmail == null) {
            Toast.makeText(this, "Error: Email not provided", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Initialize views
        etOtp = findViewById(R.id.et_otp);
        btnVerifyOtp = findViewById(R.id.btn_verify_otp);
        tvResend = findViewById(R.id.tv_resend);
        loadingDialog = new LoadingDialog(this);
        btnBack = findViewById(R.id.btn_back);

        // Initialize API
        apiService = ApiClient.getApiService(this);

        // Back button
        if (btnBack != null) btnBack.setOnClickListener(v -> finish());

        // Verify OTP button click
        btnVerifyOtp.setOnClickListener(v -> verifyOtp());

        // Resend code click
        tvResend.setOnClickListener(v -> resendCode());

        // Button press animation
        btnVerifyOtp.setOnTouchListener((v, event) -> {
            if (event.getAction() == android.view.MotionEvent.ACTION_DOWN) {
                v.animate().scaleX(0.95f).scaleY(0.95f).setDuration(100).start();
            } else if (event.getAction() == android.view.MotionEvent.ACTION_UP ||
                    event.getAction() == android.view.MotionEvent.ACTION_CANCEL) {
                v.animate().scaleX(1f).scaleY(1f).setDuration(100).start();
            }
            return false;
        });
    }

    private void verifyOtp() {
        String otp = etOtp.getText().toString().trim();

        // Validation
        if (TextUtils.isEmpty(otp)) {
            etOtp.setError(getString(R.string.error_otp_required));
            etOtp.requestFocus();
            return;
        }

        if (otp.length() != 6) {
            etOtp.setError(getString(R.string.error_otp_invalid));
            etOtp.requestFocus();
            return;
        }

        setLoading(true);

        JsonObject data = new JsonObject();
        data.addProperty("email", userEmail);
        data.addProperty("otp", otp);

        apiService.verifyResetOtp(data).enqueue(new Callback<ApiResponse<Object>>() {
            @Override
            public void onResponse(Call<ApiResponse<Object>> call, Response<ApiResponse<Object>> response) {
                setLoading(false);

                if (response.isSuccessful() && response.body() != null) {
                    ApiResponse<Object> apiResponse = response.body();

                    if (apiResponse.isStatus()) {
                        Toast.makeText(ResetPasswordActivity.this,
                                getString(R.string.success_otp_verified),
                                Toast.LENGTH_SHORT).show();

                        // Navigate to SetNewPasswordActivity
                        Intent intent = new Intent(ResetPasswordActivity.this, SetNewPasswordActivity.class);
                        intent.putExtra("email", userEmail);
                        intent.putExtra("otp", otp);
                        startActivity(intent);
                        finish();
                    } else {
                        Toast.makeText(ResetPasswordActivity.this, apiResponse.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(ResetPasswordActivity.this, getString(R.string.error_verify_failed), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<Object>> call, Throwable t) {
                setLoading(false);
                Toast.makeText(ResetPasswordActivity.this, "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void resendCode() {
        setLoading(true);

        JsonObject data = new JsonObject();
        data.addProperty("email", userEmail);

        apiService.forgotPassword(data).enqueue(new Callback<ApiResponse<Object>>() {
            @Override
            public void onResponse(Call<ApiResponse<Object>> call, Response<ApiResponse<Object>> response) {
                setLoading(false);

                if (response.isSuccessful() && response.body() != null) {
                    ApiResponse<Object> apiResponse = response.body();

                    if (apiResponse.isStatus()) {
                        Toast.makeText(ResetPasswordActivity.this, getString(R.string.success_code_resent), Toast.LENGTH_SHORT)
                                .show();
                        etOtp.setText("");
                        etOtp.requestFocus();
                    } else {
                        Toast.makeText(ResetPasswordActivity.this, apiResponse.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(ResetPasswordActivity.this, getString(R.string.error_resend_failed), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<Object>> call, Throwable t) {
                setLoading(false);
                Toast.makeText(ResetPasswordActivity.this, "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setLoading(boolean isLoading) {
        if (isLoading) {
            loadingDialog.show(getString(R.string.loading_verifying_code), getString(R.string.loading_please_wait));
            btnVerifyOtp.setEnabled(false);
            tvResend.setEnabled(false);
        } else {
            loadingDialog.dismiss();
            btnVerifyOtp.setEnabled(true);
            tvResend.setEnabled(true);
        }
    }
}
