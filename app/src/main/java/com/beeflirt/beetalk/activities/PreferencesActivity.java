package com.beeflirt.beetalk.activities;

import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.beeflirt.beetalk.R;
import com.beeflirt.beetalk.api.ApiClient;
import com.beeflirt.beetalk.api.ApiService;
import com.beeflirt.beetalk.models.ApiResponse;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.slider.RangeSlider;
import com.google.android.material.slider.Slider;
import com.google.gson.JsonObject;

import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class PreferencesActivity extends BaseActivity {
    
    private RangeSlider sliderAge;
    private Slider sliderDistance;
    private RadioGroup rgGender;
    private RadioButton rbMale, rbFemale, rbBoth;
    private TextView tvAgeRange, tvDistance;
    private MaterialButton btnSave;
    private ProgressBar progressBar;
    
    private ApiService apiService;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_preferences);
        
        // Setup toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        toolbar.setNavigationOnClickListener(v -> finish());
        
        // Initialize API
        apiService = ApiClient.getApiService(this);
        
        // Initialize views
        sliderAge = findViewById(R.id.slider_age);
        sliderDistance = findViewById(R.id.slider_distance);
        rgGender = findViewById(R.id.rg_gender);
        rbMale = findViewById(R.id.rb_male);
        rbFemale = findViewById(R.id.rb_female);
        rbBoth = findViewById(R.id.rb_both);
        tvAgeRange = findViewById(R.id.tv_age_range);
        tvDistance = findViewById(R.id.tv_distance);
        btnSave = findViewById(R.id.btn_save);
        progressBar = findViewById(R.id.progress_bar);
        
        // Setup listeners
        setupSliderListeners();
        
        // Save button
        btnSave.setOnClickListener(v -> savePreferences());
        
        // Load current preferences
        loadPreferences();
    }
    
    private void setupSliderListeners() {
        // Age range slider
        sliderAge.addOnChangeListener((slider, value, fromUser) -> {
            List<Float> values = slider.getValues();
            int minAge = values.get(0).intValue();
            int maxAge = values.get(1).intValue();
            tvAgeRange.setText(minAge + " - " + maxAge);
        });
        
        // Distance slider
        sliderDistance.addOnChangeListener((slider, value, fromUser) -> {
            int distance = (int) value;
            tvDistance.setText(distance + " km");
        });
    }
    
    private void loadPreferences() {
        progressBar.setVisibility(View.VISIBLE);
        
        apiService.getPreferences().enqueue(new Callback<ApiResponse<java.util.Map<String, Object>>>() {
            @Override
            public void onResponse(Call<ApiResponse<java.util.Map<String, Object>>> call, Response<ApiResponse<java.util.Map<String, Object>>> response) {
                progressBar.setVisibility(View.GONE);
                
                if (response.isSuccessful() && response.body() != null) {
                    ApiResponse<java.util.Map<String, Object>> apiResponse = response.body();
                    
                    if (apiResponse.isStatus() && apiResponse.getData() != null) {
                        displayPreferences(apiResponse.getData());
                    }
                }
            }
            
            @Override
            public void onFailure(Call<ApiResponse<java.util.Map<String, Object>>> call, Throwable t) {
                progressBar.setVisibility(View.GONE);
                // Use default values on failure
            }
        });
    }
    
    private void displayPreferences(java.util.Map<String, Object> preferences) {
        // Age range
        if (preferences.containsKey("min_age") && preferences.containsKey("max_age")) {
            int minAge = getIntValue(preferences.get("min_age"));
            int maxAge = getIntValue(preferences.get("max_age"));
            
            // Defend against out-of-bounds bounds from API
            float valueFrom = sliderAge.getValueFrom();
            float valueTo = sliderAge.getValueTo();
            float safeMinAge = Math.max(valueFrom, Math.min(valueTo, (float) minAge));
            float safeMaxAge = Math.max(valueFrom, Math.min(valueTo, (float) maxAge));
            
            if (safeMinAge > safeMaxAge) {
                safeMinAge = safeMaxAge;
            }
            
            sliderAge.setValues(safeMinAge, safeMaxAge);
            tvAgeRange.setText((int) safeMinAge + " - " + (int) safeMaxAge);
        }
        
        // Distance
        if (preferences.containsKey("max_distance")) {
            int distance = getIntValue(preferences.get("max_distance"));
            
            // Defend against out-of-bounds bounds from API
            float valueFrom = sliderDistance.getValueFrom();
            float valueTo = sliderDistance.getValueTo();
            float safeDistance = Math.max(valueFrom, Math.min(valueTo, (float) distance));
            
            sliderDistance.setValue(safeDistance);
            tvDistance.setText((int) safeDistance + " km");
        }
        
        // Gender preference (backend uses "interested_in")
        String genderKey = preferences.containsKey("interested_in") ? "interested_in" : "gender_preference";
        if (preferences.containsKey(genderKey)) {
            String gender = (String) preferences.get(genderKey);
            switch (gender.toLowerCase()) {
                case "male":
                    rbMale.setChecked(true);
                    break;
                case "female":
                    rbFemale.setChecked(true);
                    break;
                case "both":
                default:
                    rbBoth.setChecked(true);
                    break;
            }
        }
    }
    
    private int getIntValue(Object value) {
        if (value instanceof Number) {
            return ((Number) value).intValue();
        } else if (value instanceof String) {
            try {
                return Integer.parseInt((String) value);
            } catch (NumberFormatException e) {
                return 0;
            }
        }
        return 0;
    }
    
    private void savePreferences() {
        // Get values
        List<Float> ageValues = sliderAge.getValues();
        int minAge = ageValues.get(0).intValue();
        int maxAge = ageValues.get(1).intValue();
        int maxDistance = (int) sliderDistance.getValue();
        
        String genderPreference = "both";
        int selectedId = rgGender.getCheckedRadioButtonId();
        if (selectedId == R.id.rb_male) {
            genderPreference = "male";
        } else if (selectedId == R.id.rb_female) {
            genderPreference = "female";
        }
        
        // Create request body
        JsonObject requestBody = new JsonObject();
        requestBody.addProperty("min_age", minAge);
        requestBody.addProperty("max_age", maxAge);
        requestBody.addProperty("max_distance", maxDistance);
        requestBody.addProperty("interested_in", genderPreference);
        
        // Show progress
        progressBar.setVisibility(View.VISIBLE);
        btnSave.setEnabled(false);
        
        // Call API
        apiService.updatePreferences(requestBody).enqueue(new Callback<ApiResponse<Map<String, Object>>>() {
            @Override
            public void onResponse(Call<ApiResponse<Map<String, Object>>> call, Response<ApiResponse<Map<String, Object>>> response) {
                progressBar.setVisibility(View.GONE);
                btnSave.setEnabled(true);
                
                if (response.isSuccessful() && response.body() != null) {
                    ApiResponse<Map<String, Object>> apiResponse = response.body();
                    
                    if (apiResponse.isStatus()) {
                        Toast.makeText(PreferencesActivity.this, "Preferences saved successfully!", Toast.LENGTH_SHORT).show();
                        // Beritahu SwipeFragment untuk memuat ulang data saat kembali
                        com.beeflirt.beetalk.fragments.SwipeFragment.reloadNeeded = true;
                        finish();
                    } else {
                        Toast.makeText(PreferencesActivity.this, apiResponse.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(PreferencesActivity.this, "Failed to save preferences", Toast.LENGTH_SHORT).show();
                }
            }
            
            @Override
            public void onFailure(Call<ApiResponse<Map<String, Object>>> call, Throwable t) {
                progressBar.setVisibility(View.GONE);
                btnSave.setEnabled(true);
                Toast.makeText(PreferencesActivity.this, "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}
