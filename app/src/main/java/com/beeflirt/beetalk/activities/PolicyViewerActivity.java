package com.beeflirt.beetalk.activities;

import android.os.Bundle;
import android.webkit.WebView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.beeflirt.beetalk.R;

public class PolicyViewerActivity extends BaseActivity {

    public static final String EXTRA_POLICY_TYPE = "policy_type";
    public static final String POLICY_TYPE_PRIVACY = "privacy";
    public static final String POLICY_TYPE_TERMS = "terms";

    private WebView webView;
    private TextView toolbarTitle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_policy_viewer);

        // Initialize views
        Toolbar toolbar = findViewById(R.id.toolbar);
        toolbarTitle = findViewById(R.id.toolbar_title);
        webView = findViewById(R.id.webview);

        // Setup toolbar
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }

        toolbar.setNavigationOnClickListener(v -> finish());

        // Get policy type from intent
        String policyType = getIntent().getStringExtra(EXTRA_POLICY_TYPE);

        // Load appropriate policy
        if (POLICY_TYPE_PRIVACY.equals(policyType)) {
            toolbarTitle.setText("Privacy Policy");
            loadPrivacyPolicy();
        } else if (POLICY_TYPE_TERMS.equals(policyType)) {
            toolbarTitle.setText("Terms of Service");
            loadTermsOfService();
        }

        // Configure WebView
        webView.getSettings().setJavaScriptEnabled(false);
        webView.getSettings().setBuiltInZoomControls(true);
        webView.getSettings().setDisplayZoomControls(false);
    }

    private void loadPrivacyPolicy() {
        webView.loadUrl("file:///android_asset/privacy_policy.html");
    }

    private void loadTermsOfService() {
        webView.loadUrl("file:///android_asset/terms_of_service.html");
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
