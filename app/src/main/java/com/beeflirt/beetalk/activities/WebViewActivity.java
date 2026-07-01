package com.beeflirt.beetalk.activities;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.beeflirt.beetalk.R;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.progressindicator.LinearProgressIndicator;

public class WebViewActivity extends BaseActivity {

    private WebView webView;
    private LinearProgressIndicator progressBar;
    private LinearLayout loadingOverlay;
    private LinearLayout errorView;
    private MaterialButton btnRetry;
    private MaterialToolbar toolbar;

    private String currentUrl;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_webview);

        // Get URL and Title from intent
        currentUrl = getIntent().getStringExtra("url");
        String title = getIntent().getStringExtra("title");
        if (title == null) title = "Web Browser";

        // Initialize views
        webView      = findViewById(R.id.webView);
        progressBar  = findViewById(R.id.progressBar);
        loadingOverlay = findViewById(R.id.loadingOverlay);
        errorView    = findViewById(R.id.errorView);
        btnRetry     = findViewById(R.id.btnRetry);
        toolbar      = findViewById(R.id.toolbar);

        // Setup Toolbar
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(title);
        }
        toolbar.setNavigationOnClickListener(v -> onBackPressed());

        // Retry button
        btnRetry.setOnClickListener(v -> {
            if (currentUrl != null) {
                showLoading();
                webView.loadUrl(currentUrl);
            }
        });

        // Configure WebView
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);
        settings.setSupportZoom(true);
        settings.setBuiltInZoomControls(true);
        settings.setDisplayZoomControls(false);
        settings.setLoadWithOverviewMode(true);
        settings.setUseWideViewPort(true);

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                super.onPageStarted(view, url, favicon);
                progressBar.setVisibility(View.VISIBLE);
                errorView.setVisibility(View.GONE);
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                progressBar.setVisibility(View.GONE);
                loadingOverlay.setVisibility(View.GONE);

                // Set page title from web if not provided
                if (getIntent().getStringExtra("title") == null) {
                    if (getSupportActionBar() != null) {
                        getSupportActionBar().setTitle(view.getTitle());
                    }
                }
            }

            @Override
            public void onReceivedError(WebView view, WebResourceRequest request,
                                        WebResourceError error) {
                super.onReceivedError(view, request, error);
                if (request.isForMainFrame()) {
                    loadingOverlay.setVisibility(View.GONE);
                    progressBar.setVisibility(View.GONE);
                    errorView.setVisibility(View.VISIBLE);
                }
            }
        });

        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                progressBar.setProgress(newProgress);
                if (newProgress == 100) {
                    progressBar.setVisibility(View.GONE);
                }
            }

            @Override
            public void onReceivedTitle(WebView view, String title) {
                super.onReceivedTitle(view, title);
                if (getIntent().getStringExtra("title") == null && getSupportActionBar() != null) {
                    getSupportActionBar().setTitle(title);
                }
            }
        });

        // Load URL
        if (currentUrl != null) {
            showLoading();
            webView.loadUrl(currentUrl);
        } else {
            finish();
        }
    }

    private void showLoading() {
        loadingOverlay.setVisibility(View.VISIBLE);
        errorView.setVisibility(View.GONE);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_webview, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_refresh) {
            webView.reload();
            return true;
        } else if (id == R.id.action_open_browser) {
            if (currentUrl != null) {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(currentUrl)));
            }
            return true;
        } else if (id == R.id.action_share) {
            if (currentUrl != null) {
                Intent shareIntent = new Intent(Intent.ACTION_SEND);
                shareIntent.setType("text/plain");
                shareIntent.putExtra(Intent.EXTRA_TEXT, currentUrl);
                startActivity(Intent.createChooser(shareIntent, "Share link"));
            }
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }
}
