package com.soundcloud.android.main;

import com.soundcloud.android.R;

import android.net.Uri;
import android.os.Bundle;
import android.webkit.WebView;
import android.webkit.WebViewClient;

public class WebViewActivity extends TrackedActivity {

    public void onCreate(Bundle savedInstanceState) {
        final Uri uri = getIntent() != null ? getIntent().getData() : null;
        if (uri != null) {
            super.onCreate(savedInstanceState);
            WebView view = (WebView) findViewById(R.id.webview);
            view.getSettings().setJavaScriptEnabled(true);
            view.getSettings().setDomStorageEnabled(true);
            view.getSettings().setBlockNetworkImage(false);
            view.getSettings().setLoadsImagesAutomatically(true);
            view.setWebViewClient(new WebViewClient() {
                @Override
                public boolean shouldOverrideUrlLoading(WebView view, String url) {
                    return false;
                }
            });
            view.loadUrl(uri.toString());
        } else {
            finish();
        }
    }

    @Override
    protected void setActivityContentView() {
        setContentView(R.layout.web_view);
    }

}
