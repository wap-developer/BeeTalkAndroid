package com.beeflirt.beetalk.fragments;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.beeflirt.beetalk.R;
import com.beeflirt.beetalk.activities.AppActivity;
import com.beeflirt.beetalk.activities.MainSelectActivity;
import com.beeflirt.beetalk.adsconfig.Ads;
import com.beeflirt.beetalk.adsconfig.SettingAds;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;

public class SetupGeoFragment extends Fragment {

    // ─── Callback so Activity can react to location grant/deny ───────────────
    public interface OnLocationStatusChangedListener {
        void onLocationStatusChanged(boolean enabled);
    }

    private OnLocationStatusChangedListener locationStatusListener;

    public void setOnLocationStatusChangedListener(OnLocationStatusChangedListener listener) {
        this.locationStatusListener = listener;
    }
    // ─────────────────────────────────────────────────────────────────────────

    private TextView tvLocationStatus;
    private View viewStatusDot;
    private Button btnEnableLocation;
    private FusedLocationProviderClient fusedLocationClient;
    private ActivityResultLauncher<String> locationPermissionLauncher;

    private double latitude = 0.0;
    private double longitude = 0.0;
    private boolean locationEnabled = false;
    
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_setup_geo, container, false);
    }
    
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        tvLocationStatus = view.findViewById(R.id.tv_location_status);
        viewStatusDot    = view.findViewById(R.id.view_status_dot);
        btnEnableLocation = view.findViewById(R.id.btn_enable_location);
        
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity());
        
        // Setup permission launcher
        locationPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                isGranted -> {
                    if (isGranted) {
                        getCurrentLocation();
                    } else {
                        Toast.makeText(requireContext(), "Location permission denied", Toast.LENGTH_SHORT).show();
                        setStatusError();
                    }
                }
        );

        // Button click listener
        btnEnableLocation.setOnClickListener(v -> {
            // Short pulse animation on click
            v.animate().scaleX(0.96f).scaleY(0.96f).setDuration(80)
                    .withEndAction(() -> v.animate().scaleX(1f).scaleY(1f).setDuration(80).start())
                    .start();
            Ads.ShowInterstitialAds(getActivity(), SettingAds.PLACEMENT_INTERSTITIAL_GEO, () -> {
                requestLocationPermission();
            });
        });

        Ads.LoadInterstitialAds(getActivity(), SettingAds.PLACEMENT_INTERSTITIAL_GEO);

        // Auto-check if permission already granted
        checkLocationPermission();
    }
    
    private void checkLocationPermission() {

        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) 
                == PackageManager.PERMISSION_GRANTED) {
            getCurrentLocation();
        }
    }

    private void requestLocationPermission() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) 
                == PackageManager.PERMISSION_GRANTED) {
            getCurrentLocation();
        } else {
            locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION);
        }
    }
    
    private void getCurrentLocation() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) 
                != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        // Show "detecting" state
        tvLocationStatus.setText("Detecting location...");
        tvLocationStatus.setTextColor(getResources().getColor(R.color.warning, null));
        if (viewStatusDot != null) {
            viewStatusDot.setBackgroundTintList(
                    getResources().getColorStateList(R.color.warning, null));
        }
        
        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(location -> {
                    if (location != null) {
                        latitude = location.getLatitude();
                        longitude = location.getLongitude();
                        locationEnabled = true;
                        setStatusSuccess();
                    } else {
                        Toast.makeText(requireContext(), "Unable to get location. Please try again.", Toast.LENGTH_SHORT).show();
                        setStatusError();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(requireContext(), "Error getting location: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    setStatusError();
                });
    }

    private void setStatusSuccess() {
        locationEnabled = true;  // mark FIRST so listener fires with correct state

        tvLocationStatus.setText("✓ Location enabled");
        tvLocationStatus.setTextColor(getResources().getColor(R.color.success, null));

        if (viewStatusDot != null) {
            viewStatusDot.setBackgroundTintList(
                    getResources().getColorStateList(R.color.success, null));
        }

        // Animate button to "done" state
        btnEnableLocation.setText("✓  Location Enabled");
        btnEnableLocation.setEnabled(false);
        btnEnableLocation.setAlpha(0.7f);

        // Notify Activity
        if (locationStatusListener != null) {
            locationStatusListener.onLocationStatusChanged(true);
        }
    }

    private void setStatusError() {
        locationEnabled = false;

        tvLocationStatus.setText("Location not enabled");
        tvLocationStatus.setTextColor(getResources().getColor(R.color.error, null));

        if (viewStatusDot != null) {
            viewStatusDot.setBackgroundTintList(
                    getResources().getColorStateList(R.color.error, null));
        }

        // Notify Activity
        if (locationStatusListener != null) {
            locationStatusListener.onLocationStatusChanged(false);
        }
    }
    
    public String getBio() {
        return "";
    }

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public boolean isLocationEnabled() {
        return locationEnabled;
    }

    /**
     * Called by ProfileSetupActivity when the user taps "Enable Now" inside
     * the location-required dialog. Triggers the system permission prompt.
     */
    public void requestLocationPermissionPublic() {
        requestLocationPermission();
    }
}
