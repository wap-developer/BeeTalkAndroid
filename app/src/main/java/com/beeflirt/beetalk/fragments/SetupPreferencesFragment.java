package com.beeflirt.beetalk.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.beeflirt.beetalk.R;
import com.google.android.material.slider.RangeSlider;
import com.google.android.material.slider.Slider;

import java.util.List;

public class SetupPreferencesFragment extends Fragment {

    private RadioGroup rgInterestedIn;
    private RangeSlider sliderAgeRange;
    private Slider sliderMaxDistance;
    private TextView tvAgeRange, tvMaxDistance;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_setup_preferences, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        rgInterestedIn    = view.findViewById(R.id.rg_interested_in);
        sliderAgeRange    = view.findViewById(R.id.slider_age_range);
        sliderMaxDistance = view.findViewById(R.id.slider_max_distance);
        tvAgeRange        = view.findViewById(R.id.tv_age_range);
        tvMaxDistance     = view.findViewById(R.id.tv_max_distance);

        // Set default slider values
        sliderAgeRange.setValues(18f, 35f);
        sliderMaxDistance.setValue(1000f);

        // Initialize labels
        updateAgeLabel(18, 35);
        updateDistanceLabel(1000);

        // Age range slider listener
        sliderAgeRange.addOnChangeListener((slider, value, fromUser) -> {
            List<Float> values = sliderAgeRange.getValues();
            updateAgeLabel(values.get(0).intValue(), values.get(1).intValue());
        });

        // Distance slider listener — shows "Anywhere" at maximum
        sliderMaxDistance.addOnChangeListener((slider, value, fromUser) -> {
            updateDistanceLabel((int) value);
        });
    }

    private void updateAgeLabel(int min, int max) {
        tvAgeRange.setText(String.format("%d - %d yrs", min, max));
    }

    private void updateDistanceLabel(int km) {
        if (km >= 1000) {
            tvMaxDistance.setText("Anywhere (1000+ km)");
        } else {
            tvMaxDistance.setText(String.format("%d km", km));
        }
    }

    public String getInterestedIn() {
        int selectedId = rgInterestedIn.getCheckedRadioButtonId();
        if (selectedId == R.id.rb_male) {
            return "male";
        } else if (selectedId == R.id.rb_female) {
            return "female";
        } else {
            return "both";
        }
    }

    public int getMinAge() {
        return sliderAgeRange.getValues().get(0).intValue();
    }

    public int getMaxAge() {
        return sliderAgeRange.getValues().get(1).intValue();
    }

    public int getMaxDistance() {
        return (int) sliderMaxDistance.getValue();
    }
}
