package com.soundcloud.android.activity;

import com.soundcloud.android.R;

import android.app.Activity;
import android.net.Uri;
import android.os.Bundle;
import android.webkit.WebView;
import android.webkit.WebViewClient;

public class WebViewActivity extends Activity {

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final Uri uri = getIntent() != null ? getIntent().getData() : null;
        if (uri != null) {
            setContentView(R.layout.web_view);
            WebView view = (WebView) findViewById(R.id.webview);
            view.getSettings().setJavaScriptEnabled(true);
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
}