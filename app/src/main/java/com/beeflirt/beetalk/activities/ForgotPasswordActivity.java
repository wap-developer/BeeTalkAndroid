package com.beeflirt.beetalk.activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.beeflirt.beetalk.R;
import com.beeflirt.beetalk.api.ApiClient;
import com.beeflirt.beetalk.api.ApiService;
import com.beeflirt.beetalk.models.ApiResponse;
import com.beeflirt.beetalk.utils.AnimationHelper;
import com.google.gson.JsonObject;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ForgotPasswordActivity extends BaseActivity {

    private EditText etEmail;
    private Button btnSendCode;
    private TextView tvBackToLogin;
    private ProgressBar progressBar;
    private ImageView btnBack;

    private ApiService apiService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forgot_password);

        // Initialize views
        etEmail = findViewById(R.id.et_email);
        btnSendCode = findViewById(R.id.btn_send_code);
        tvBackToLogin = findViewById(R.id.tv_back_to_login);
        progressBar = findViewById(R.id.progress_bar);
        btnBack = findViewById(R.id.btn_back);

        // Initialize API
        apiService = ApiClient.getApiService(this);

        // Back button
        if (btnBack != null) btnBack.setOnClickListener(v -> finish());

        // Send code button click
        btnSendCode.setOnClickListener(v -> sendResetCode());

        // Back to login click
        tvBackToLogin.setOnClickListener(v -> finish());

        // Entrance animation for button
        btnSendCode.setOnTouchListener((v, event) -> {
            if (event.getAction() == android.view.MotionEvent.ACTION_DOWN) {
                v.animate().scaleX(0.95f).scaleY(0.95f).setDuration(100).start();
            } else if (event.getAction() == android.view.MotionEvent.ACTION_UP ||
                    event.getAction() == android.view.MotionEvent.ACTION_CANCEL) {
                v.animate().scaleX(1f).scaleY(1f).setDuration(100).start();
            }
            return false;
        });
    }

    private void sendResetCode() {
        String email = etEmail.getText().toString().trim();

        // Validation
        if (TextUtils.isEmpty(email)) {
            etEmail.setError(getString(R.string.error_email_required));
            etEmail.requestFocus();
            return;
        }

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etEmail.setError(getString(R.string.error_invalid_email));
            etEmail.requestFocus();
            return;
        }

        setLoading(true);

        JsonObject data = new JsonObject();
        data.addProperty("email", email);

        apiService.forgotPassword(data).enqueue(new Callback<ApiResponse<Object>>() {
            @Override
            public void onResponse(Call<ApiResponse<Object>> call, Response<ApiResponse<Object>> response) {
                setLoading(false);

                if (response.isSuccessful() && response.body() != null) {
                    ApiResponse<Object> apiResponse = response.body();

                    if (apiResponse.isStatus()) {
                        String successMsg = apiResponse.getMessage() != null && !apiResponse.getMessage().isEmpty() ? 
                                            apiResponse.getMessage() : getString(R.string.success_code_sent);
                                            
                        cn.pedant.SweetAlert.SweetAlertDialog pDialog = new cn.pedant.SweetAlert.SweetAlertDialog(ForgotPasswordActivity.this, cn.pedant.SweetAlert.SweetAlertDialog.SUCCESS_TYPE);
                        pDialog.setTitleText("Success");
                        pDialog.setContentText(successMsg);
                        pDialog.setConfirmText("OK");
                        pDialog.setConfirmClickListener(sDialog -> {
                            sDialog.dismissWithAnimation();
                            // Navigate to ResetPasswordActivity
                            Intent intent = new Intent(ForgotPasswordActivity.this, ResetPasswordActivity.class);
                            intent.putExtra("email", email);
                            startActivity(intent);
                            finish();
                        });
                        pDialog.setConfirmButtonBackgroundColor(getResources().getColor(R.color.primary));
                        pDialog.setCancelable(false);
                        pDialog.show();
                        
                    } else {
                        AnimationHelper.animateShake(etEmail);
                        etEmail.setError(apiResponse.getMessage());
                        
                        cn.pedant.SweetAlert.SweetAlertDialog eDialog = new cn.pedant.SweetAlert.SweetAlertDialog(ForgotPasswordActivity.this, cn.pedant.SweetAlert.SweetAlertDialog.ERROR_TYPE);
                        eDialog.setTitleText("Failed");
                        eDialog.setContentText(apiResponse.getMessage());
                        eDialog.setConfirmButtonBackgroundColor(getResources().getColor(R.color.primary));
                        eDialog.show();
                    }
                } else {
                    // Parse error body for message
                    String errorMsg = getString(R.string.error_send_code_failed);
                    try {
                        if (response.errorBody() != null) {
                            String errorJson = response.errorBody().string();
                            com.google.gson.Gson gson = new com.google.gson.Gson();
                            ApiResponse<Object> errResponse = gson.fromJson(errorJson,
                                    new com.google.gson.reflect.TypeToken<ApiResponse<Object>>() {}.getType());
                            if (errResponse != null && errResponse.getMessage() != null) {
                                errorMsg = errResponse.getMessage();
                            }
                        }
                    } catch (Exception ignored) {}

                    AnimationHelper.animateShake(etEmail);

                    cn.pedant.SweetAlert.SweetAlertDialog eDialog = new cn.pedant.SweetAlert.SweetAlertDialog(ForgotPasswordActivity.this, cn.pedant.SweetAlert.SweetAlertDialog.ERROR_TYPE);
                    eDialog.setTitleText("Account Not Found");
                    eDialog.setContentText(errorMsg);
                    eDialog.setConfirmText("OK");
                    eDialog.setConfirmButtonBackgroundColor(getResources().getColor(R.color.primary));
                    eDialog.show();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<Object>> call, Throwable t) {
                setLoading(false);
                Toast.makeText(ForgotPasswordActivity.this, "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setLoading(boolean isLoading) {
        if (isLoading) {
            progressBar.setVisibility(View.VISIBLE);
            btnSendCode.setEnabled(false);
        } else {
            progressBar.setVisibility(View.GONE);
            btnSendCode.setEnabled(true);
        }
    }
}
