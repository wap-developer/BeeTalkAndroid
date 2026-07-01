package com.beeflirt.beetalk.activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.beeflirt.beetalk.dialogs.LoadingDialog;

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

public class RegisterActivity extends BaseActivity {

    private EditText etEmail, etUsername, etPassword, etConfirmPassword, etName, etBirthDate, etBio;
    private RadioGroup rgGender;
    private Button btnRegister;
    private TextView tvLogin, tvPrivacyPolicy, tvTerms;
    private TextView btnGenderMale, btnGenderFemale, btnGenderOther;
    private android.widget.CheckBox cbAgreeTerms;
    private LoadingDialog loadingDialog;
    private ImageView btnBack;
    private String selectedGender = "";

    private ApiService apiService;
    private SharedPrefManager prefManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        // Initialize views
        etEmail = findViewById(R.id.et_email);
        etUsername = findViewById(R.id.et_username);
        etPassword = findViewById(R.id.et_password);
        etConfirmPassword = findViewById(R.id.et_confirm_password);
        etName = findViewById(R.id.et_name);
        etBirthDate = findViewById(R.id.et_birth_date);
        etBio = findViewById(R.id.et_bio);
        rgGender = findViewById(R.id.rg_gender);
        btnRegister = findViewById(R.id.btn_register);
        tvLogin = findViewById(R.id.tv_login);
        loadingDialog = new LoadingDialog(this);
        cbAgreeTerms = findViewById(R.id.cb_agree_terms);
        tvPrivacyPolicy = findViewById(R.id.tv_privacy_policy);
        tvTerms = findViewById(R.id.tv_terms);
        btnGenderMale = findViewById(R.id.btn_gender_male);
        btnGenderFemale = findViewById(R.id.btn_gender_female);
        btnGenderOther = findViewById(R.id.btn_gender_other);
        btnBack = findViewById(R.id.btn_back);

        // Initialize API and SharedPreferences
        apiService = ApiClient.getApiService(this);
        prefManager = SharedPrefManager.getInstance(this);

        // Setup Material DatePicker for birth date
        etBirthDate.setOnClickListener(v -> showDatePicker());

        // Register button click
        btnRegister.setOnClickListener(v -> attemptRegister());

        // Login text click
        tvLogin.setOnClickListener(v -> finish());

        // Back button
        if (btnBack != null) btnBack.setOnClickListener(v -> finish());

        // Privacy Policy link click
        tvPrivacyPolicy.setOnClickListener(v -> {
            Intent intent = new Intent(RegisterActivity.this, PolicyViewerActivity.class);
            intent.putExtra(PolicyViewerActivity.EXTRA_POLICY_TYPE, PolicyViewerActivity.POLICY_TYPE_PRIVACY);
            startActivity(intent);
        });

        // Terms of Service link click
        tvTerms.setOnClickListener(v -> {
            Intent intent = new Intent(RegisterActivity.this, PolicyViewerActivity.class);
            intent.putExtra(PolicyViewerActivity.EXTRA_POLICY_TYPE, PolicyViewerActivity.POLICY_TYPE_TERMS);
            startActivity(intent);
        });

        // Gender toggle buttons
        setupGenderToggle();

        // Apply entrance animations
        setupAnimations();
    }

    private void setupGenderToggle() {
        if (btnGenderMale == null || btnGenderFemale == null || btnGenderOther == null) return;
        btnGenderMale.setOnClickListener(v -> selectGender("male"));
        btnGenderFemale.setOnClickListener(v -> selectGender("female"));
        btnGenderOther.setOnClickListener(v -> selectGender("other"));
    }

    private void selectGender(String gender) {
        selectedGender = gender;
        // Reset all
        btnGenderMale.setBackgroundResource(R.drawable.bg_gender_option);
        btnGenderFemale.setBackgroundResource(R.drawable.bg_gender_option);
        btnGenderOther.setBackgroundResource(R.drawable.bg_gender_option);
        btnGenderMale.setTextColor(getResources().getColor(R.color.primary));
        btnGenderFemale.setTextColor(getResources().getColor(R.color.primary));
        btnGenderOther.setTextColor(getResources().getColor(R.color.primary));
        // Highlight selected
        TextView selected = gender.equals("male") ? btnGenderMale :
                gender.equals("female") ? btnGenderFemale : btnGenderOther;
        selected.setBackgroundResource(R.drawable.bg_gender_option_selected);
        selected.setTextColor(android.graphics.Color.WHITE);
        // Sync RadioGroup
        if (rgGender != null) {
            int radioId = gender.equals("male") ? R.id.rb_male :
                    gender.equals("female") ? R.id.rb_female : R.id.rb_other;
            rgGender.check(radioId);
        }
    }

    private void setupAnimations() {
        // Form fields with stagger
        AnimationHelper.slideUpFadeIn(etEmail, 100);
        AnimationHelper.slideUpFadeIn(etUsername, 150);
        AnimationHelper.slideUpFadeIn(etName, 200);
        AnimationHelper.slideUpFadeIn(etPassword, 250);
        AnimationHelper.slideUpFadeIn(etConfirmPassword, 300);
        AnimationHelper.slideUpFadeIn(etBirthDate, 350);
        AnimationHelper.slideUpFadeIn(etBio, 400);

        // Checkbox and links
        if (cbAgreeTerms != null) {
            AnimationHelper.slideUpFadeIn(cbAgreeTerms, 450);
        }

        // Register button
        AnimationHelper.slideUpFadeIn(btnRegister, 500);

        // Login text
        AnimationHelper.slideUpFadeIn(tvLogin, 550);

        // Button press effect
        btnRegister.setOnTouchListener((v, event) -> {
            if (event.getAction() == android.view.MotionEvent.ACTION_DOWN) {
                v.animate().scaleX(0.95f).scaleY(0.95f).setDuration(100).start();
            } else if (event.getAction() == android.view.MotionEvent.ACTION_UP ||
                    event.getAction() == android.view.MotionEvent.ACTION_CANCEL) {
                v.animate().scaleX(1f).scaleY(1f).setDuration(100).start();
            }
            return false;
        });
    }

    private void attemptRegister() {
        // Get input values
        String email = etEmail.getText().toString().trim();
        String username = etUsername.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        String confirmPassword = etConfirmPassword.getText().toString().trim();
        String name = etName.getText().toString().trim();
        String birthDate = etBirthDate.getText().toString().trim();
        String bio = etBio.getText().toString().trim();

        // Get gender from visual toggle (selectedGender field), fallback to RadioGroup
        String gender = selectedGender;
        if (gender.isEmpty() && rgGender != null) {
            int selectedGenderId = rgGender.getCheckedRadioButtonId();
            RadioButton selectedGenderButton = findViewById(selectedGenderId);
            gender = selectedGenderButton != null ? selectedGenderButton.getText().toString().toLowerCase() : "";
        }

        // Validate inputs
        // Check terms agreement first
        if (!cbAgreeTerms.isChecked()) {
            Toast.makeText(this, "Please agree to Terms of Service and Privacy Policy", Toast.LENGTH_SHORT).show();
            return;
        }

        if (TextUtils.isEmpty(email)) {
            etEmail.setError("Email is required");
            etEmail.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(username)) {
            etUsername.setError("Username is required");
            etUsername.requestFocus();
            return;
        }

        if (username.length() < 3) {
            etUsername.setError("Username must be at least 3 characters");
            etUsername.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(password)) {
            etPassword.setError("Password is required");
            etPassword.requestFocus();
            return;
        }

        if (password.length() < 8) {
            etPassword.setError("Password must be at least 8 characters");
            etPassword.requestFocus();
            return;
        }

        if (!password.equals(confirmPassword)) {
            etConfirmPassword.setError("Passwords do not match");
            etConfirmPassword.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(name)) {
            etName.setError("Name is required");
            etName.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(birthDate)) {
            etBirthDate.setError("Birth date is required (YYYY-MM-DD)");
            etBirthDate.requestFocus();
            return;
        }

        // Validate age - must be at least 18 years old
        try {
            String[] dateParts = birthDate.split("-");
            int year = Integer.parseInt(dateParts[0]);
            int month = Integer.parseInt(dateParts[1]);
            int day = Integer.parseInt(dateParts[2]);

            java.util.Calendar birthCalendar = java.util.Calendar.getInstance();
            birthCalendar.set(year, month - 1, day);

            java.util.Calendar today = java.util.Calendar.getInstance();
            int age = today.get(java.util.Calendar.YEAR) - birthCalendar.get(java.util.Calendar.YEAR);

            if (today.get(java.util.Calendar.DAY_OF_YEAR) < birthCalendar.get(java.util.Calendar.DAY_OF_YEAR)) {
                age--;
            }

            if (age < 18) {
                Toast.makeText(this, "You must be at least 18 years old to register", Toast.LENGTH_LONG).show();
                etBirthDate.setError("Must be 18 or older");
                etBirthDate.requestFocus();
                return;
            }
        } catch (Exception e) {
            etBirthDate.setError("Invalid date format");
            etBirthDate.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(gender)) {
            Toast.makeText(this, "Please select gender", Toast.LENGTH_SHORT).show();
            return;
        }

        // Show progress
        setLoading(true);

        // Create request body
        JsonObject registerData = new JsonObject();
        registerData.addProperty("email", email);
        registerData.addProperty("username", username);
        registerData.addProperty("password", password);
        registerData.addProperty("name", name);
        registerData.addProperty("gender", gender);
        registerData.addProperty("birth_date", birthDate);
        if (!TextUtils.isEmpty(bio)) {
            registerData.addProperty("bio", bio);
        }

        // Make API call
        apiService.register(registerData).enqueue(new Callback<ApiResponse<Map<String, Object>>>() {
            @Override
            public void onResponse(Call<ApiResponse<Map<String, Object>>> call,
                    Response<ApiResponse<Map<String, Object>>> response) {
                setLoading(false);

                if (response.isSuccessful() && response.body() != null) {
                    ApiResponse<Map<String, Object>> apiResponse = response.body();

                    if (apiResponse.isStatus()) {
                        // Registration successful - auto login
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

                        if (userId == -1) {
                            Toast.makeText(RegisterActivity.this, "Registration error: invalid user ID", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        // Save session (auto-login)
                        prefManager.saveUserSession(token, userId, userEmail);

                        Toast.makeText(RegisterActivity.this,
                                "Welcome to BeeTalk Lite!",
                                Toast.LENGTH_SHORT).show();

                        // Navigate directly to ProfileSetup (skip OTP)
                        Intent intent = new Intent(RegisterActivity.this, ProfileSetupActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                        finish();
                    } else {
                        // API returned error in response body
                        handleRegistrationError(apiResponse.getMessage());
                    }
                } else {
                    // HTTP error (400, 422, etc.) - try to parse error body
                    try {
                        if (response.errorBody() != null) {
                            String errorJson = response.errorBody().string();
                            com.google.gson.Gson gson = new com.google.gson.Gson();
                            ApiResponse<Map<String, Object>> errorResponse = gson.fromJson(errorJson,
                                    new com.google.gson.reflect.TypeToken<ApiResponse<Map<String, Object>>>() {
                                    }.getType());

                            if (errorResponse != null) {
                                // Check if there are validation errors in data.errors
                                if (errorResponse.getData() != null && errorResponse.getData().containsKey("errors")) {
                                    Map<String, Object> errors = (Map<String, Object>) errorResponse.getData()
                                            .get("errors");
                                    if (errors != null && errors.containsKey("email")) {
                                        String emailError = errors.get("email").toString();
                                        handleRegistrationError(emailError);
                                        return;
                                    }
                                    if (errors != null && errors.containsKey("username")) {
                                        String usernameError = errors.get("username").toString();
                                        handleRegistrationError("Username: " + usernameError);
                                        return;
                                    }
                                }

                                // Fallback to message
                                if (errorResponse.getMessage() != null) {
                                    handleRegistrationError(errorResponse.getMessage());
                                } else {
                                    Toast.makeText(RegisterActivity.this, "Registration failed. Please try again.",
                                            Toast.LENGTH_SHORT).show();
                                }
                            } else {
                                Toast.makeText(RegisterActivity.this, "Registration failed. Please try again.",
                                        Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            Toast.makeText(RegisterActivity.this, "Registration failed. Please try again.",
                                    Toast.LENGTH_SHORT).show();
                        }
                    } catch (Exception e) {
                        Toast.makeText(RegisterActivity.this, "Registration failed. Please try again.",
                                Toast.LENGTH_SHORT).show();
                    }
                }
            }

            private void handleRegistrationError(String errorMessage) {
                // Check if it's a duplicate email error
                if (errorMessage != null && errorMessage.toLowerCase().contains("already registered")) {
                    // Show modern SweetAlert Dialog
                    cn.pedant.SweetAlert.SweetAlertDialog dialog = new cn.pedant.SweetAlert.SweetAlertDialog(
                            RegisterActivity.this, cn.pedant.SweetAlert.SweetAlertDialog.ERROR_TYPE);
                    dialog.setTitleText("Email Already Registered")
                            .setContentText("This email is already registered. Please use another email or login.")
                            .setConfirmText("OK")
                            .setConfirmClickListener(sDialog -> {
                                sDialog.dismissWithAnimation();
                                etEmail.requestFocus();
                            });

                    // Customize button color to primary color
                    dialog.setConfirmButtonBackgroundColor(
                            getResources().getColor(com.beeflirt.beetalk.R.color.primary));
                    dialog.show();
                } else {
                    // For other errors, show warning dialog
                    cn.pedant.SweetAlert.SweetAlertDialog dialog = new cn.pedant.SweetAlert.SweetAlertDialog(
                            RegisterActivity.this, cn.pedant.SweetAlert.SweetAlertDialog.WARNING_TYPE);
                    dialog.setTitleText("Registration Failed")
                            .setContentText(errorMessage)
                            .setConfirmText("OK");

                    // Customize button color to primary color
                    dialog.setConfirmButtonBackgroundColor(
                            getResources().getColor(com.beeflirt.beetalk.R.color.primary));
                    dialog.show();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<Map<String, Object>>> call, Throwable t) {
                setLoading(false);
                Toast.makeText(RegisterActivity.this, "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showDatePicker() {
        // Calculate 18 years ago from today
        java.util.Calendar calendar = java.util.Calendar.getInstance();
        calendar.add(java.util.Calendar.YEAR, -18);
        long eighteenYearsAgo = calendar.getTimeInMillis();

        // Set constraints - user must be at least 18 years old
        com.google.android.material.datepicker.CalendarConstraints.Builder constraintsBuilder = new com.google.android.material.datepicker.CalendarConstraints.Builder();
        constraintsBuilder.setEnd(eighteenYearsAgo); // Max date is 18 years ago

        // Build date picker with constraints
        com.google.android.material.datepicker.MaterialDatePicker<Long> datePicker = com.google.android.material.datepicker.MaterialDatePicker.Builder
                .datePicker()
                .setTitleText("Select Birth Date (Must be 18+)")
                .setSelection(eighteenYearsAgo) // Default to 18 years ago
                .setCalendarConstraints(constraintsBuilder.build())
                .build();

        // Handle date selection
        datePicker.addOnPositiveButtonClickListener(selection -> {
            // Convert timestamp to date string (YYYY-MM-DD)
            java.util.Calendar selectedCalendar = java.util.Calendar.getInstance(java.util.TimeZone.getTimeZone("UTC"));
            selectedCalendar.setTimeInMillis(selection);

            int year = selectedCalendar.get(java.util.Calendar.YEAR);
            int month = selectedCalendar.get(java.util.Calendar.MONTH) + 1; // Month is 0-based
            int day = selectedCalendar.get(java.util.Calendar.DAY_OF_MONTH);

            String dateString = String.format(java.util.Locale.US, "%04d-%02d-%02d", year, month, day);
            etBirthDate.setText(dateString);
        });

        // Show the picker
        datePicker.show(getSupportFragmentManager(), "DATE_PICKER");
    }

    private void setLoading(boolean isLoading) {
        if (isLoading) {
            loadingDialog.show("Creating Account…", "Setting up your profile");
            btnRegister.setEnabled(false);
        } else {
            loadingDialog.dismiss();
            btnRegister.setEnabled(true);
        }
    }
}
