package com.beeflirt.beetalk.adapters;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.beeflirt.beetalk.fragments.SetupGeoFragment;
import com.beeflirt.beetalk.fragments.SetupPhotosFragment;
import com.beeflirt.beetalk.fragments.SetupPreferencesFragment;

public class SetupPagerAdapter extends FragmentStateAdapter {
    
    private SetupPhotosFragment photosFragment;
    private SetupGeoFragment geoFragment;
    private SetupPreferencesFragment preferencesFragment;
    
    public SetupPagerAdapter(@NonNull FragmentActivity fragmentActivity) {
        super(fragmentActivity);
        photosFragment = new SetupPhotosFragment();
        geoFragment = new SetupGeoFragment();
        preferencesFragment = new SetupPreferencesFragment();
    }
    
    @NonNull
    @Override
    public Fragment createFragment(int position) {
        switch (position) {
            case 0:
                return photosFragment;
            case 1:
                return preferencesFragment;
            case 2:
                return geoFragment;
            default:
                return photosFragment;
        }
    }
    
    @Override
    public int getItemCount() {
        return 3;
    }
    
    public SetupPhotosFragment getPhotosFragment() {
        return photosFragment;
    }
    
    public SetupGeoFragment getGeoFragment() {
        return geoFragment;
    }
    
    public SetupPreferencesFragment getPreferencesFragment() {
        return preferencesFragment;
    }
}
