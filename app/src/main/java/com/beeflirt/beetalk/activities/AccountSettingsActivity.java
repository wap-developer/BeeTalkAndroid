package com.beeflirt.beetalk.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.beeflirt.beetalk.R;
import com.beeflirt.beetalk.api.ApiClient;
import com.beeflirt.beetalk.api.ApiService;
import com.beeflirt.beetalk.models.ApiResponse;
import com.beeflirt.beetalk.utils.SharedPrefManager;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.gson.JsonObject;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AccountSettingsActivity extends BaseActivity {
    
    private TextInputEditText etCurrentPassword, etNewPassword, etConfirmPassword;
    private MaterialButton btnChangePassword, btnDeleteAccount;
    private ProgressBar progressBar;
    
    private ApiService apiService;
    private SharedPrefManager prefManager;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_account_settings);
        
        // Setup toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        toolbar.setNavigationOnClickListener(v -> finish());
        
        // Initialize
        apiService = ApiClient.getApiService(this);
        prefManager = SharedPrefManager.getInstance(this);
        
        // Initialize views
        etCurrentPassword = findViewById(R.id.et_current_password);
        etNewPassword = findViewById(R.id.et_new_password);
        etConfirmPassword = findViewById(R.id.et_confirm_password);
        btnChangePassword = findViewById(R.id.btn_change_password);
        btnDeleteAccount = findViewById(R.id.btn_delete_account);
        progressBar = findViewById(R.id.progress_bar);
        
        // Button listeners
        btnChangePassword.setOnClickListener(v -> changePassword());
        btnDeleteAccount.setOnClickListener(v -> confirmDeleteAccount());
    }
    
    private void changePassword() {
        // Get values
        String currentPassword = etCurrentPassword.getText().toString().trim();
        String newPassword = etNewPassword.getText().toString().trim();
        String confirmPassword = etConfirmPassword.getText().toString().trim();
        
        // Validate
        if (currentPassword.isEmpty()) {
            etCurrentPassword.setError("Current password is required");
            etCurrentPassword.requestFocus();
            return;
        }
        
        if (newPassword.isEmpty()) {
            etNewPassword.setError("New password is required");
            etNewPassword.requestFocus();
            return;
        }
        
        if (newPassword.length() < 8) {
            etNewPassword.setError("Password must be at least 8 characters");
            etNewPassword.requestFocus();
            return;
        }
        
        if (!newPassword.equals(confirmPassword)) {
            etConfirmPassword.setError("Passwords do not match");
            etConfirmPassword.requestFocus();
            return;
        }
        
        // Create request body
        JsonObject requestBody = new JsonObject();
        requestBody.addProperty("current_password", currentPassword);
        requestBody.addProperty("new_password", newPassword);
        requestBody.addProperty("confirm_password", confirmPassword);
        
        // Show progress
        progressBar.setVisibility(View.VISIBLE);
        btnChangePassword.setEnabled(false);
        
        // Call API
        apiService.changePassword(requestBody).enqueue(new Callback<ApiResponse<Object>>() {
            @Override
            public void onResponse(Call<ApiResponse<Object>> call, Response<ApiResponse<Object>> response) {
                progressBar.setVisibility(View.GONE);
                btnChangePassword.setEnabled(true);
                
                if (response.isSuccessful() && response.body() != null) {
                    ApiResponse<Object> apiResponse = response.body();
                    
                    if (apiResponse.isStatus()) {
                        Toast.makeText(AccountSettingsActivity.this, "Password changed successfully!", Toast.LENGTH_SHORT).show();
                        
                        // Clear input fields
                        etCurrentPassword.setText("");
                        etNewPassword.setText("");
                        etConfirmPassword.setText("");
                        
                        finish();
                    } else {
                        Toast.makeText(AccountSettingsActivity.this, apiResponse.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(AccountSettingsActivity.this, "Failed to change password", Toast.LENGTH_SHORT).show();
                }
            }
            
            @Override
            public void onFailure(Call<ApiResponse<Object>> call, Throwable t) {
                progressBar.setVisibility(View.GONE);
                btnChangePassword.setEnabled(true);
                Toast.makeText(AccountSettingsActivity.this, "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
    
    private void confirmDeleteAccount() {
        android.app.Dialog dialog = new android.app.Dialog(this);
        dialog.requestWindowFeature(android.view.Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_delete_account);

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(
                    new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));
            dialog.getWindow().setLayout(
                    android.view.ViewGroup.LayoutParams.WRAP_CONTENT,
                    android.view.ViewGroup.LayoutParams.WRAP_CONTENT);
        }

        android.widget.EditText etConfirm = dialog.findViewById(R.id.et_confirm_delete);
        com.google.android.material.button.MaterialButton btnConfirm = dialog.findViewById(R.id.btn_confirm_delete);
        View btnCancel = dialog.findViewById(R.id.btn_cancel_delete);

        // Enable delete button only when user types "DELETE"
        etConfirm.addTextChangedListener(new android.text.TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override
            public void afterTextChanged(android.text.Editable s) {
                boolean match = "DELETE".equals(s.toString().trim());
                btnConfirm.setEnabled(match);
                btnConfirm.setAlpha(match ? 1f : 0.4f);
            }
        });

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        btnConfirm.setOnClickListener(v -> {
            dialog.dismiss();
            deleteAccount();
        });

        dialog.setCancelable(true);
        dialog.show();
    }
    
    private void deleteAccount() {
        // Show progress
        progressBar.setVisibility(View.VISIBLE);
        btnDeleteAccount.setEnabled(false);
        
        // Call API
        apiService.deleteAccount().enqueue(new Callback<ApiResponse<Object>>() {
            @Override
            public void onResponse(Call<ApiResponse<Object>> call, Response<ApiResponse<Object>> response) {
                progressBar.setVisibility(View.GONE);
                btnDeleteAccount.setEnabled(true);
                
                if (response.isSuccessful() && response.body() != null) {
                    ApiResponse<Object> apiResponse = response.body();
                    
                    if (apiResponse.isStatus()) {
                        Toast.makeText(AccountSettingsActivity.this, "Account deleted successfully", Toast.LENGTH_SHORT).show();

                        // Clear Room database
                        java.util.concurrent.Executors.newSingleThreadExecutor().execute(() -> {
                            com.beeflirt.beetalk.db.AppDatabase.getInstance(AccountSettingsActivity.this).clearAllTables();
                        });

                        // Clear session and go to login
                        prefManager.clearSession();
                        ApiClient.resetApiService();
                        Intent intent = new Intent(AccountSettingsActivity.this, com.beeflirt.beetalk.activities.LoginActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                        finish();
                    } else {
                        Toast.makeText(AccountSettingsActivity.this, apiResponse.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(AccountSettingsActivity.this, "Failed to delete account", Toast.LENGTH_SHORT).show();
                }
            }
            
            @Override
            public void onFailure(Call<ApiResponse<Object>> call, Throwable t) {
                progressBar.setVisibility(View.GONE);
                btnDeleteAccount.setEnabled(true);
                Toast.makeText(AccountSettingsActivity.this, "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}
