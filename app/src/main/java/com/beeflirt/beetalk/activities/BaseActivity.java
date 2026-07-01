package com.beeflirt.beetalk.activities;

import android.content.Context;
import android.os.Bundle;

import android.view.View;
import android.view.ViewGroup;

import androidx.activity.EdgeToEdge;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.beeflirt.beetalk.utils.LocaleHelper;

public class BaseActivity extends AppCompatActivity {
    
    // Set to false if an activity wants the content to go below the status bar (e.g. Splash, Login)
    protected boolean applyTopInset = true;
    // Set to true for activities that need keyboard-aware padding (e.g. ChatActivity)
    protected boolean applyImeInset = false;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Mengaktifkan tampilan Edge-to-Edge modern secara global
        EdgeToEdge.enable(this);
    }

    @Override
    public void setContentView(int layoutResID) {
        super.setContentView(layoutResID);
        setupEdgeToEdgeInsets();
    }

    @Override
    public void setContentView(View view) {
        super.setContentView(view);
        setupEdgeToEdgeInsets();
    }

    @Override
    public void setContentView(View view, ViewGroup.LayoutParams params) {
        super.setContentView(view, params);
        setupEdgeToEdgeInsets();
    }

    private void setupEdgeToEdgeInsets() {
        View rootView = findViewById(android.R.id.content);
        if (rootView != null && rootView instanceof ViewGroup) {
            View child = ((ViewGroup) rootView).getChildAt(0);
            if (child != null) {
                ViewCompat.setOnApplyWindowInsetsListener(child, (v, insets) -> {
                    Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                    int bottomPadding = systemBars.bottom;

                    // Jika activity butuh keyboard-aware, tambahkan IME inset
                    if (applyImeInset) {
                        Insets imeInsets = insets.getInsets(WindowInsetsCompat.Type.ime());
                        bottomPadding = Math.max(systemBars.bottom, imeInsets.bottom);
                    }

                    v.setPadding(
                            systemBars.left,
                            applyTopInset ? systemBars.top : 0,
                            systemBars.right,
                            bottomPadding
                    );
                    return insets;
                });
            }
        }
    }

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(LocaleHelper.onAttach(newBase));
    }
}
