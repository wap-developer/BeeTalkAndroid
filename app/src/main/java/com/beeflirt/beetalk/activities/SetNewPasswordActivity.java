package com.beeflirt.beetalk.activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
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

public class SetNewPasswordActivity extends BaseActivity {

    private EditText etNewPassword, etConfirmPassword;
    private Button btnSetPassword;
    private LoadingDialog loadingDialog;
    private ImageView btnBack;

    private ApiService apiService;
    private String userEmail;
    private String userOtp;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_set_new_password);

        // Get email and OTP from intent
        userEmail = getIntent().getStringExtra("email");
        userOtp = getIntent().getStringExtra("otp");

        if (userEmail == null || userOtp == null) {
            Toast.makeText(this, getString(R.string.error_missing_data), Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Initialize views
        etNewPassword = findViewById(R.id.et_new_password);
        etConfirmPassword = findViewById(R.id.et_confirm_password);
        btnSetPassword = findViewById(R.id.btn_set_password);
        loadingDialog = new LoadingDialog(this);
        btnBack = findViewById(R.id.btn_back);

        // Initialize API
        apiService = ApiClient.getApiService(this);

        // Back button
        if (btnBack != null) btnBack.setOnClickListener(v -> finish());

        // Set password button click
        btnSetPassword.setOnClickListener(v -> setNewPassword());

        // Button press animation
        btnSetPassword.setOnTouchListener((v, event) -> {
            if (event.getAction() == android.view.MotionEvent.ACTION_DOWN) {
                v.animate().scaleX(0.95f).scaleY(0.95f).setDuration(100).start();
            } else if (event.getAction() == android.view.MotionEvent.ACTION_UP ||
                    event.getAction() == android.view.MotionEvent.ACTION_CANCEL) {
                v.animate().scaleX(1f).scaleY(1f).setDuration(100).start();
            }
            return false;
        });
    }

    private void setNewPassword() {
        String newPassword = etNewPassword.getText().toString().trim();
        String confirmPassword = etConfirmPassword.getText().toString().trim();

        // Validation
        if (TextUtils.isEmpty(newPassword)) {
            etNewPassword.setError(getString(R.string.error_password_required));
            etNewPassword.requestFocus();
            return;
        }

        if (newPassword.length() < 8) {
            etNewPassword.setError(getString(R.string.error_password_min_length));
            etNewPassword.requestFocus();
            return;
        }

        if (!newPassword.equals(confirmPassword)) {
            etConfirmPassword.setError(getString(R.string.error_passwords_dont_match));
            etConfirmPassword.requestFocus();
            return;
        }

        setLoading(true);

        JsonObject data = new JsonObject();
        data.addProperty("email", userEmail);
        data.addProperty("otp", userOtp);
        data.addProperty("new_password", newPassword);

        apiService.resetPassword(data).enqueue(new Callback<ApiResponse<Object>>() {
            @Override
            public void onResponse(Call<ApiResponse<Object>> call, Response<ApiResponse<Object>> response) {
                setLoading(false);

                if (response.isSuccessful() && response.body() != null) {
                    ApiResponse<Object> apiResponse = response.body();

                    if (apiResponse.isStatus()) {
                        Toast.makeText(SetNewPasswordActivity.this,
                                getString(R.string.success_password_reset),
                                Toast.LENGTH_LONG).show();

                        // Navigate to LoginActivity
                        Intent intent = new Intent(SetNewPasswordActivity.this, LoginActivity.class);
                        intent.putExtra("email", userEmail);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                        finish();
                    } else {
                        Toast.makeText(SetNewPasswordActivity.this, apiResponse.getMessage(), Toast.LENGTH_SHORT)
                                .show();
                    }
                } else {
                    Toast.makeText(SetNewPasswordActivity.this, getString(R.string.error_password_reset_failed), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<Object>> call, Throwable t) {
                setLoading(false);
                Toast.makeText(SetNewPasswordActivity.this, "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setLoading(boolean isLoading) {
        if (isLoading) {
            loadingDialog.show(getString(R.string.loading_saving_password), getString(R.string.loading_please_wait));
            btnSetPassword.setEnabled(false);
        } else {
            loadingDialog.dismiss();
            btnSetPassword.setEnabled(true);
        }
    }
}
