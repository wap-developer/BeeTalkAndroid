package com.beeflirt.beetalk.activities;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;

import com.beeflirt.beetalk.R;
import com.beeflirt.beetalk.api.ApiClient;
import com.beeflirt.beetalk.api.ApiService;
import com.beeflirt.beetalk.models.ApiResponse;
import com.beeflirt.beetalk.models.Profile;
import com.beeflirt.beetalk.utils.Constants;
import com.bumptech.glide.Glide;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import de.hdodenhof.circleimageview.CircleImageView;
import android.widget.EditText;
import com.google.gson.JsonObject;

import java.io.File;
import java.util.Calendar;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class EditProfileActivity extends BaseActivity {
    
    private CircleImageView ivEditPrimaryPhoto;
    private View btnChangePrimaryPhoto;
    private android.widget.EditText etBio, etName, etBirthDate;
    private android.widget.EditText etEmail, etUsername, etHobbies, etDreams;
    private AutoCompleteTextView etGender;
    private TextView tvEmailVerifiedBadge, btnOpenEmailDialog;
    private String currentEmail = "";
    private boolean isEmailVerified = false;
    private MaterialButton btnSave;
    private ProgressBar progressBar;
    
    private ApiService apiService;
    private Profile currentProfile;
    
    private ActivityResultLauncher<Intent> imagePickerLauncher;
    private ActivityResultLauncher<String> permissionLauncher;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_profile);
        
        // Setup toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        toolbar.setNavigationOnClickListener(v -> finish());
        
        // Initialize API
        apiService = ApiClient.getApiService(this);
        
        // Initialize views
        ivEditPrimaryPhoto = findViewById(R.id.iv_edit_primary_photo);
        btnChangePrimaryPhoto = findViewById(R.id.btn_change_primary_photo);
        etBio = findViewById(R.id.et_bio);
        etName = findViewById(R.id.et_name);
        etGender = findViewById(R.id.et_gender);
        etBirthDate = findViewById(R.id.et_birth_date);
        etEmail = findViewById(R.id.et_email);
        etUsername = findViewById(R.id.et_username);
        etHobbies = findViewById(R.id.et_hobbies);
        etDreams = findViewById(R.id.et_dreams);
        tvEmailVerifiedBadge = findViewById(R.id.tv_email_verified_badge);
        btnOpenEmailDialog = findViewById(R.id.btn_open_email_dialog);
        btnSave = findViewById(R.id.btn_save);
        progressBar = findViewById(R.id.progress_bar);
        
        // Setup gender dropdown
        String[] genders = new String[]{"Male", "Female", "Other"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, genders);
        etGender.setAdapter(adapter);
        
        // Birth date picker
        etBirthDate.setOnClickListener(v -> showDatePicker());
        
        // Primary photo change button
        if (btnChangePrimaryPhoto != null) {
            btnChangePrimaryPhoto.setOnClickListener(v -> checkPermissionAndOpenPicker());
        }

        // Save button
        btnSave.setOnClickListener(v -> saveProfile());

        // Verify / Email dialog button
        if (btnOpenEmailDialog != null) {
            btnOpenEmailDialog.setOnClickListener(v -> showEmailDialog());
        }
        
        // Setup permission launcher
        permissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                isGranted -> {
                    if (isGranted) {
                        openImagePicker();
                    } else {
                        Toast.makeText(this, "Permission denied. Cannot access photos.", Toast.LENGTH_SHORT).show();
                    }
                }
        );
        
        // Setup image picker
        imagePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        Uri imageUri = result.getData().getData();
                        if (imageUri != null) {
                            uploadPhoto(imageUri);
                        }
                    }
                }
        );
        
        // Load current profile
        loadProfile();
    }
    
    private void checkPermissionAndOpenPicker() {
        // For Android 13+ (API 33+), use READ_MEDIA_IMAGES
        // For older versions, use READ_EXTERNAL_STORAGE
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Android 13+
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES) 
                    == PackageManager.PERMISSION_GRANTED) {
                openImagePicker();
            } else {
                permissionLauncher.launch(Manifest.permission.READ_MEDIA_IMAGES);
            }
        } else {
            // Android 12 and below
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) 
                    == PackageManager.PERMISSION_GRANTED) {
                openImagePicker();
            } else {
                permissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE);
            }
        }
    }
    
    private void openImagePicker() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        imagePickerLauncher.launch(intent);
    }
    
    private void uploadPhoto(Uri imageUri) {
        progressBar.setVisibility(View.VISIBLE);

        try {
            File photoFile = com.beeflirt.beetalk.utils.ImageUtils.compressImage(this, imageUri);

            RequestBody requestFile = RequestBody.create(MediaType.parse("image/jpeg"), photoFile);
            MultipartBody.Part body = MultipartBody.Part.createFormData("photo", photoFile.getName(), requestFile);
            
            apiService.uploadPhoto(body).enqueue(new Callback<ApiResponse<java.util.Map<String, Object>>>() {
                @Override
                public void onResponse(Call<ApiResponse<java.util.Map<String, Object>>> call, Response<ApiResponse<java.util.Map<String, Object>>> response) {
                    progressBar.setVisibility(View.GONE);
                    
                    if (photoFile.exists()) {
                        photoFile.delete();
                    }
                    
                    if (response.isSuccessful() && response.body() != null) {
                        ApiResponse<java.util.Map<String, Object>> apiResponse = response.body();
                        if (apiResponse.isStatus()) {
                            Toast.makeText(EditProfileActivity.this, "Photo uploaded successfully!", Toast.LENGTH_SHORT).show();
                            loadProfile(); // Reload to show new photo
                        } else {
                            String errorMsg = "Upload failed: " + apiResponse.getMessage();
                            Log.d("EditProfile", errorMsg);
                            Toast.makeText(EditProfileActivity.this, errorMsg, Toast.LENGTH_LONG).show();
                        }
                    } else {
                        String errorMsg = "Upload failed - Response code: " + response.code();
                        try {
                            if (response.errorBody() != null) {
                                errorMsg += "\nError: " + response.errorBody().string();
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        Log.d("EditProfile", errorMsg);
                        Toast.makeText(EditProfileActivity.this, "Failed to upload photo (Code: " + response.code() + ")", Toast.LENGTH_LONG).show();
                    }
                }
                
                @Override
                public void onFailure(Call<ApiResponse<java.util.Map<String, Object>>> call, Throwable t) {
                    progressBar.setVisibility(View.GONE);
                    
                    if (photoFile.exists()) {
                        photoFile.delete();
                    }
                    
                    String errorMsg = "Upload failed: " + t.getMessage();
                    Log.d("EditProfile", errorMsg, t);
                    Toast.makeText(EditProfileActivity.this, errorMsg, Toast.LENGTH_LONG).show();
                }
            });
        } catch (Exception e) {
            progressBar.setVisibility(View.GONE);
            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
    
    private File createTempFileFromUri(Uri uri) {
        try {
            java.io.InputStream inputStream = getContentResolver().openInputStream(uri);
            if (inputStream == null) return null;
            
            // Create temp file
            File tempFile = File.createTempFile("upload_", ".jpg", getCacheDir());
            
            // Copy stream to file
            java.io.FileOutputStream outputStream = new java.io.FileOutputStream(tempFile);
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
            
            outputStream.close();
            inputStream.close();
            
            return tempFile;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
    
    private void loadProfile() {
        progressBar.setVisibility(View.VISIBLE);
        
        apiService.getProfile().enqueue(new Callback<ApiResponse<Profile>>() {
            @Override
            public void onResponse(Call<ApiResponse<Profile>> call, Response<ApiResponse<Profile>> response) {
                progressBar.setVisibility(View.GONE);
                
                if (response.isSuccessful() && response.body() != null) {
                    ApiResponse<Profile> apiResponse = response.body();
                    
                    if (apiResponse.isStatus()) {
                        currentProfile = apiResponse.getData();
                        displayProfile(currentProfile);
                    } else {
                        Toast.makeText(EditProfileActivity.this, apiResponse.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(EditProfileActivity.this, "Failed to load profile", Toast.LENGTH_SHORT).show();
                }
            }
            
            @Override
            public void onFailure(Call<ApiResponse<Profile>> call, Throwable t) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(EditProfileActivity.this, "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
    
    private void displayProfile(Profile profile) {
        if (profile == null) return;
        
        if (profile.getName() != null) {
            etName.setText(profile.getName());
        }
        
        if (profile.getBio() != null) {
            etBio.setText(profile.getBio());
        }
        
        if (profile.getGender() != null) {
            etGender.setText(profile.getGender(), false);
        }
        
        if (profile.getBirthDate() != null) {
            etBirthDate.setText(profile.getBirthDate());
        }
        
        if (profile.getUsername() != null) {
            etUsername.setText(profile.getUsername());
        }
        
        if (profile.getHobbies() != null) {
            etHobbies.setText(profile.getHobbies());
        }
        
        if (profile.getDreams() != null) {
            etDreams.setText(profile.getDreams());
        }
        
        // Load email + verification status from /auth/me
        loadUserEmail();
        
        // Load primary photo into CircleImageView
        if (ivEditPrimaryPhoto != null) {
            String photoUrl = profile.getPrimaryPhoto();
            if (photoUrl == null || photoUrl.isEmpty()) {
                if (profile.getPhotos() != null && !profile.getPhotos().isEmpty()) {
                    photoUrl = profile.getPhotos().get(0).getPhotoUrl();
                }
            }
            if (photoUrl != null && !photoUrl.isEmpty()) {
                String fullUrl = photoUrl.startsWith("http") ? photoUrl : Constants.IMAGE_BASE_URL + photoUrl;
                Glide.with(this)
                        .load(fullUrl)
                        .placeholder(R.drawable.ic_person)
                        .error(R.drawable.ic_person)
                        .centerCrop()
                        .into(ivEditPrimaryPhoto);
            }
        }


    }
    
    private void showDatePicker() {
        int currentYear = Calendar.getInstance().get(Calendar.YEAR);
        int minYear = currentYear - 100;
        int maxYear = currentYear - 18;

        int defaultDay = 1;
        int defaultMonth = 0; // Jan
        int defaultYear = maxYear;

        // Parse existing date
        String currentDate = etBirthDate.getText().toString().trim();
        if (!currentDate.isEmpty()) {
            try {
                String[] parts = currentDate.split("-");
                defaultYear = Integer.parseInt(parts[0]);
                defaultMonth = Integer.parseInt(parts[1]) - 1;
                defaultDay = Integer.parseInt(parts[2]);
            } catch (Exception ignored) {}
        }

        // Custom dialog
        android.app.Dialog dialog = new android.app.Dialog(this);
        dialog.requestWindowFeature(android.view.Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_date_picker);

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(
                    new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));
            dialog.getWindow().setLayout(
                    android.view.ViewGroup.LayoutParams.WRAP_CONTENT,
                    android.view.ViewGroup.LayoutParams.WRAP_CONTENT);
        }

        android.widget.NumberPicker pickerDay = dialog.findViewById(R.id.picker_day);
        android.widget.NumberPicker pickerMonth = dialog.findViewById(R.id.picker_month);
        android.widget.NumberPicker pickerYear = dialog.findViewById(R.id.picker_year);
        View btnCancel = dialog.findViewById(R.id.btn_date_cancel);
        View btnConfirm = dialog.findViewById(R.id.btn_date_confirm);

        // Setup Day picker (1-31)
        pickerDay.setMinValue(1);
        pickerDay.setMaxValue(31);
        pickerDay.setValue(defaultDay);
        pickerDay.setWrapSelectorWheel(true);

        // Setup Month picker (display names)
        String[] months = {"Jan", "Feb", "Mar", "Apr", "May", "Jun",
                "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"};
        pickerMonth.setMinValue(0);
        pickerMonth.setMaxValue(11);
        pickerMonth.setDisplayedValues(months);
        pickerMonth.setValue(defaultMonth);
        pickerMonth.setWrapSelectorWheel(true);

        // Setup Year picker
        pickerYear.setMinValue(minYear);
        pickerYear.setMaxValue(maxYear);
        pickerYear.setValue(defaultYear);
        pickerYear.setWrapSelectorWheel(false);

        // Update max days when month/year changes
        android.widget.NumberPicker.OnValueChangeListener updateDays = (picker, oldVal, newVal) -> {
            int m = pickerMonth.getValue() + 1;
            int y = pickerYear.getValue();
            Calendar cal = Calendar.getInstance();
            cal.set(y, m - 1, 1);
            int maxDays = cal.getActualMaximum(Calendar.DAY_OF_MONTH);
            pickerDay.setMaxValue(maxDays);
            if (pickerDay.getValue() > maxDays) pickerDay.setValue(maxDays);
        };
        pickerMonth.setOnValueChangedListener(updateDays);
        pickerYear.setOnValueChangedListener(updateDays);

        // Set initial max days
        Calendar initCal = Calendar.getInstance();
        initCal.set(defaultYear, defaultMonth, 1);
        pickerDay.setMaxValue(initCal.getActualMaximum(Calendar.DAY_OF_MONTH));

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        btnConfirm.setOnClickListener(v -> {
            int day = pickerDay.getValue();
            int month = pickerMonth.getValue() + 1;
            int year = pickerYear.getValue();
            String date = year + "-" + String.format("%02d", month) + "-" + String.format("%02d", day);
            etBirthDate.setText(date);
            dialog.dismiss();
        });

        dialog.setCancelable(true);
        dialog.show();
    }
    
    private void saveProfile() {
        // Get input values
        String name = etName.getText().toString().trim();
        String bio = etBio.getText().toString().trim();
        String gender = etGender.getText().toString().trim();
        String birthDate = etBirthDate.getText().toString().trim();
        String hobbies = etHobbies.getText().toString().trim();
        String dreams = etDreams.getText().toString().trim();
        
        // Validate
        if (name.isEmpty()) {
            etName.setError("Name is required");
            etName.requestFocus();
            return;
        }
        
        // Create request body
        JsonObject requestBody = new JsonObject();
        requestBody.addProperty("name", name);
        requestBody.addProperty("bio", bio);
        requestBody.addProperty("gender", gender);
        requestBody.addProperty("birth_date", birthDate);
        requestBody.addProperty("hobbies", hobbies);
        requestBody.addProperty("dreams", dreams);
        
        // Show progress
        progressBar.setVisibility(View.VISIBLE);
        btnSave.setEnabled(false);
        
        // Call API
        apiService.updateProfile(requestBody).enqueue(new Callback<ApiResponse<Profile>>() {
            @Override
            public void onResponse(Call<ApiResponse<Profile>> call, Response<ApiResponse<Profile>> response) {
                progressBar.setVisibility(View.GONE);
                btnSave.setEnabled(true);
                
                if (response.isSuccessful() && response.body() != null) {
                    ApiResponse<Profile> apiResponse = response.body();
                    
                    if (apiResponse.isStatus()) {
                        Toast.makeText(EditProfileActivity.this, "Profile updated successfully!", Toast.LENGTH_SHORT).show();
                        finish(); // Close activity and return to profile
                    } else {
                        Toast.makeText(EditProfileActivity.this, apiResponse.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(EditProfileActivity.this, "Failed to update profile", Toast.LENGTH_SHORT).show();
                }
            }
            
            @Override
            public void onFailure(Call<ApiResponse<Profile>> call, Throwable t) {
                progressBar.setVisibility(View.GONE);
                btnSave.setEnabled(true);
                Toast.makeText(EditProfileActivity.this, "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
    
    /**
     * Load email dan status verifikasi dari /auth/me
     */
    private void loadUserEmail() {
        apiService.getCurrentUser().enqueue(new Callback<ApiResponse<com.beeflirt.beetalk.models.User>>() {
            @Override
            public void onResponse(Call<ApiResponse<com.beeflirt.beetalk.models.User>> call,
                    Response<ApiResponse<com.beeflirt.beetalk.models.User>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isStatus()) {
                    com.beeflirt.beetalk.models.User user = response.body().getData();
                    if (user != null) {
                        currentEmail = user.getEmail() != null ? user.getEmail() : "";
                        isEmailVerified = user.isVerified();
                        etEmail.setText(currentEmail);
                        updateEmailBadge();
                    }
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<com.beeflirt.beetalk.models.User>> call, Throwable t) {
                // Fallback: gunakan email dari SharedPref
                String email = com.beeflirt.beetalk.utils.SharedPrefManager.getInstance(EditProfileActivity.this).getEmail();
                if (email != null) {
                    currentEmail = email;
                    etEmail.setText(email);
                }
            }
        });
    }

    /**
     * Update badge tampilan: Verified (hijau) atau Verify (oranye)
     */
    private void updateEmailBadge() {
        if (isEmailVerified) {
            if (tvEmailVerifiedBadge != null) tvEmailVerifiedBadge.setVisibility(View.VISIBLE);
            if (btnOpenEmailDialog != null) btnOpenEmailDialog.setVisibility(View.GONE);
        } else {
            if (tvEmailVerifiedBadge != null) tvEmailVerifiedBadge.setVisibility(View.GONE);
            if (btnOpenEmailDialog != null) btnOpenEmailDialog.setVisibility(View.VISIBLE);
        }
    }

    /**
     * Dialog multi-state: View email -> Verify / Ubah Email -> OTP
     */
    private void showEmailDialog() {
        android.app.Dialog dialog = new android.app.Dialog(this);
        dialog.requestWindowFeature(android.view.Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_email_verify);

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(
                    new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));
            dialog.getWindow().setLayout(
                    android.view.ViewGroup.LayoutParams.WRAP_CONTENT,
                    android.view.ViewGroup.LayoutParams.WRAP_CONTENT);
        }

        // Views
        TextView tvTitle = dialog.findViewById(R.id.tv_dialog_title);
        TextView tvSubtitle = dialog.findViewById(R.id.tv_dialog_subtitle);
        View stateEmailView = dialog.findViewById(R.id.state_email_view);
        View stateChangeEmail = dialog.findViewById(R.id.state_change_email);
        View stateOtp = dialog.findViewById(R.id.state_otp);
        View loadingOverlay = dialog.findViewById(R.id.dialog_loading_overlay);

        // State 1 views
        EditText etDisplayEmail = dialog.findViewById(R.id.et_dialog_email_display);
        View btnVerifySend = dialog.findViewById(R.id.btn_verify_send);
        View btnSwitchToChange = dialog.findViewById(R.id.btn_switch_to_change);
        View btnCloseDialog = dialog.findViewById(R.id.btn_close_dialog);

        // State 2 views
        EditText etNewEmail = dialog.findViewById(R.id.et_new_email);
        View btnBackToView = dialog.findViewById(R.id.btn_back_to_view);
        View btnSaveNewEmail = dialog.findViewById(R.id.btn_save_new_email);

        // State 3 views
        EditText etOtpCode = dialog.findViewById(R.id.et_otp_code);
        View btnVerifyOtp = dialog.findViewById(R.id.btn_verify_otp);
        View btnResendOtp = dialog.findViewById(R.id.btn_resend_otp);

        // Init state 1
        etDisplayEmail.setText(currentEmail);

        // Helper: show/hide loading
        Runnable showLoading = () -> {
            loadingOverlay.setVisibility(View.VISIBLE);
            stateEmailView.setVisibility(View.GONE);
            stateChangeEmail.setVisibility(View.GONE);
            stateOtp.setVisibility(View.GONE);
            tvSubtitle.setText("Please wait...");
        };

        // Close
        btnCloseDialog.setOnClickListener(v -> dialog.dismiss());

        // ── Verify -> kirim OTP lalu pindah ke state 3 ──
        btnVerifySend.setOnClickListener(v -> {
            showLoading.run();
            tvTitle.setText("Sending OTP...");

            JsonObject body = new JsonObject();
            body.addProperty("email", currentEmail);

            apiService.resendVerification(body).enqueue(new Callback<ApiResponse<Object>>() {
                @Override
                public void onResponse(Call<ApiResponse<Object>> call, Response<ApiResponse<Object>> response) {
                    loadingOverlay.setVisibility(View.GONE);

                    if (response.isSuccessful() && response.body() != null && response.body().isStatus()) {
                        stateOtp.setVisibility(View.VISIBLE);
                        tvTitle.setText("Enter OTP");
                        tvSubtitle.setText("We sent a 6-digit code to\n" + currentEmail);
                    } else {
                        // Parse error message
                        String msg = "Failed to send OTP";
                        try {
                            if (response.body() != null && response.body().getMessage() != null) {
                                msg = response.body().getMessage();
                            }
                            if (!response.isSuccessful() && response.errorBody() != null) {
                                String errorJson = response.errorBody().string();
                                com.google.gson.Gson gson = new com.google.gson.Gson();
                                ApiResponse<?> err = gson.fromJson(errorJson, ApiResponse.class);
                                if (err != null && err.getMessage() != null) msg = err.getMessage();
                            }
                        } catch (Exception ignored) {}

                        stateEmailView.setVisibility(View.VISIBLE);
                        tvTitle.setText("Email Verification");
                        tvSubtitle.setText(msg);
                    }
                }

                @Override
                public void onFailure(Call<ApiResponse<Object>> call, Throwable t) {
                    loadingOverlay.setVisibility(View.GONE);
                    stateEmailView.setVisibility(View.VISIBLE);
                    tvTitle.setText("Email Verification");
                    tvSubtitle.setText("Connection error. Please try again.");
                }
            });
        });

        // ── Switch to change email state ──
        btnSwitchToChange.setOnClickListener(v -> {
            stateEmailView.setVisibility(View.GONE);
            stateChangeEmail.setVisibility(View.VISIBLE);
            tvTitle.setText("Change Email");
            tvSubtitle.setText("Enter your new email address");
            etNewEmail.setText(currentEmail);
            etNewEmail.requestFocus();
            etNewEmail.setSelection(etNewEmail.getText().length());
        });

        // ── Back to view state ──
        btnBackToView.setOnClickListener(v -> {
            stateChangeEmail.setVisibility(View.GONE);
            stateEmailView.setVisibility(View.VISIBLE);
            tvTitle.setText("Email Verification");
            tvSubtitle.setText("Verify your email or change to a new one");
        });

        // ── Save new email ──
        btnSaveNewEmail.setOnClickListener(v -> {
            String newEmail = etNewEmail.getText().toString().trim();

            if (newEmail.isEmpty()) {
                etNewEmail.setError("Email cannot be empty");
                return;
            }
            if (!android.util.Patterns.EMAIL_ADDRESS.matcher(newEmail).matches()) {
                etNewEmail.setError("Invalid email format");
                return;
            }
            if (newEmail.equals(currentEmail)) {
                etNewEmail.setError("Same as current email");
                return;
            }

            showLoading.run();
            tvTitle.setText("Updating email...");

            JsonObject emailBody = new JsonObject();
            emailBody.addProperty("email", newEmail);

            apiService.updateEmail(emailBody).enqueue(new Callback<ApiResponse<java.util.Map<String, Object>>>() {
                @Override
                public void onResponse(Call<ApiResponse<java.util.Map<String, Object>>> call,
                        Response<ApiResponse<java.util.Map<String, Object>>> response) {
                    loadingOverlay.setVisibility(View.GONE);

                    if (response.isSuccessful() && response.body() != null && response.body().isStatus()) {
                        currentEmail = newEmail;
                        isEmailVerified = false;
                        etEmail.setText(newEmail);
                        etDisplayEmail.setText(newEmail);
                        updateEmailBadge();

                        stateOtp.setVisibility(View.VISIBLE);
                        tvTitle.setText("Enter OTP");
                        tvSubtitle.setText("We sent a 6-digit code to\n" + newEmail);
                    } else {
                        String msg = "Failed to update email";
                        try {
                            if (!response.isSuccessful() && response.errorBody() != null) {
                                String errorJson = response.errorBody().string();
                                com.google.gson.Gson gson = new com.google.gson.Gson();
                                ApiResponse<java.util.Map<String, Object>> err = gson.fromJson(errorJson,
                                        new com.google.gson.reflect.TypeToken<ApiResponse<java.util.Map<String, Object>>>() {}.getType());
                                if (err != null && err.getMessage() != null) msg = err.getMessage();
                            }
                        } catch (Exception ignored) {}

                        stateChangeEmail.setVisibility(View.VISIBLE);
                        tvTitle.setText("Change Email");
                        tvSubtitle.setText(msg);
                    }
                }

                @Override
                public void onFailure(Call<ApiResponse<java.util.Map<String, Object>>> call, Throwable t) {
                    loadingOverlay.setVisibility(View.GONE);
                    stateChangeEmail.setVisibility(View.VISIBLE);
                    tvTitle.setText("Change Email");
                    tvSubtitle.setText("Connection error. Please try again.");
                }
            });
        });

        // ── Verify OTP ──
        btnVerifyOtp.setOnClickListener(v -> {
            String otp = etOtpCode.getText().toString().trim();
            if (otp.length() != 6) {
                etOtpCode.setError("Enter 6-digit code");
                return;
            }

            showLoading.run();
            tvTitle.setText("Verifying...");

            JsonObject otpBody = new JsonObject();
            otpBody.addProperty("email", currentEmail);
            otpBody.addProperty("otp", otp);

            apiService.verifyOtp(otpBody).enqueue(new Callback<ApiResponse<Object>>() {
                @Override
                public void onResponse(Call<ApiResponse<Object>> call, Response<ApiResponse<Object>> response) {
                    loadingOverlay.setVisibility(View.GONE);

                    boolean success = false;
                    String errorMsg = "Invalid or expired OTP";

                    if (response.isSuccessful() && response.body() != null) {
                        if (response.body().isStatus()) {
                            success = true;
                        } else if (response.body().getMessage() != null) {
                            errorMsg = response.body().getMessage();
                        }
                    } else {
                        // Parse error body
                        try {
                            if (response.errorBody() != null) {
                                String errorJson = response.errorBody().string();
                                com.google.gson.Gson gson = new com.google.gson.Gson();
                                ApiResponse<?> err = gson.fromJson(errorJson, ApiResponse.class);
                                if (err != null && err.getMessage() != null) errorMsg = err.getMessage();
                            }
                        } catch (Exception ignored) {}
                    }

                    if (success) {
                        isEmailVerified = true;
                        updateEmailBadge();
                        dialog.dismiss();
                        Toast.makeText(EditProfileActivity.this, "Email verified!", Toast.LENGTH_SHORT).show();
                    } else {
                        stateOtp.setVisibility(View.VISIBLE);
                        tvTitle.setText("Enter OTP");
                        tvSubtitle.setText(errorMsg);
                        etOtpCode.setText("");
                        etOtpCode.requestFocus();
                    }
                }

                @Override
                public void onFailure(Call<ApiResponse<Object>> call, Throwable t) {
                    loadingOverlay.setVisibility(View.GONE);
                    stateOtp.setVisibility(View.VISIBLE);
                    tvTitle.setText("Enter OTP");
                    tvSubtitle.setText("Connection error. Please try again.");
                }
            });
        });

        // ── Resend OTP ──
        btnResendOtp.setOnClickListener(v -> {
            showLoading.run();
            tvTitle.setText("Resending OTP...");

            JsonObject resendBody = new JsonObject();
            resendBody.addProperty("email", currentEmail);

            apiService.resendVerification(resendBody).enqueue(new Callback<ApiResponse<Object>>() {
                @Override
                public void onResponse(Call<ApiResponse<Object>> call, Response<ApiResponse<Object>> response) {
                    loadingOverlay.setVisibility(View.GONE);
                    stateOtp.setVisibility(View.VISIBLE);

                    if (response.isSuccessful() && response.body() != null && response.body().isStatus()) {
                        tvTitle.setText("Enter OTP");
                        tvSubtitle.setText("New OTP sent to " + currentEmail);
                        etOtpCode.setText("");
                        etOtpCode.requestFocus();
                    } else {
                        tvTitle.setText("Enter OTP");
                        tvSubtitle.setText("Failed to resend OTP. Try again.");
                    }
                }

                @Override
                public void onFailure(Call<ApiResponse<Object>> call, Throwable t) {
                    loadingOverlay.setVisibility(View.GONE);
                    stateOtp.setVisibility(View.VISIBLE);
                    tvTitle.setText("Enter OTP");
                    tvSubtitle.setText("Connection error. Try again.");
                }
            });
        });

        dialog.setCancelable(true);
        dialog.show();
    }
}
