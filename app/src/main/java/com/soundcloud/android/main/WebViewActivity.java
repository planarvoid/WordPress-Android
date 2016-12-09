package com.soundcloud.android.main;

import com.soundcloud.android.R;

import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.webkit.WebView;
import android.webkit.WebViewClient;

public class WebViewActivity extends RootActivity {

    @Nullable private WebView webView;

    public void onCreate(Bundle savedInstanceState) {
        final Uri uri = getIntent() != null ? getIntent().getData() : null;
        if (uri != null) {
            super.onCreate(savedInstanceState);
            webView = (WebView) findViewById(R.id.webview);
            webView.getSettings().setJavaScriptEnabled(true);
            webView.getSettings().setDomStorageEnabled(true);
            webView.getSettings().setBlockNetworkImage(false);
            webView.getSettings().setLoadsImagesAutomatically(true);
            webView.setWebViewClient(new WebViewClient() {
                @Override
                public boolean shouldOverrideUrlLoading(WebView view, String url) {
                    return false;
                }
            });
            webView.loadUrl(uri.toString());
        } else {
            finish();
        }
    }

    @Override
    public Screen getScreen() {
        return Screen.UNKNOWN;
    }

    @Override
    protected void setActivityContentView() {
        setContentView(R.layout.web_view);
    }

    @Override
    public void onBackPressed() {
        if (webView != null && webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }

}
