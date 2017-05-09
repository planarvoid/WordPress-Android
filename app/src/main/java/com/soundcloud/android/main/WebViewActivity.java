package com.soundcloud.android.main;

import com.soundcloud.android.R;
import com.soundcloud.android.deeplinks.DeepLink;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.webkit.WebView;
import android.webkit.WebViewClient;

public class WebViewActivity extends RootActivity {

    private WebView webView;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        webView = (WebView) findViewById(R.id.webview);

        final Uri uri = getIntent() != null ? getIntent().getData() : null;
        if (uri != null) {
            webView.getSettings().setJavaScriptEnabled(true);
            webView.getSettings().setDomStorageEnabled(true);
            webView.getSettings().setBlockNetworkImage(false);
            webView.getSettings().setLoadsImagesAutomatically(true);
            webView.setWebViewClient(new WebViewClient() {
                // this method was deprecated and the new one added in SDK 24
                @Override
                public boolean shouldOverrideUrlLoading(WebView view, String url) {
                    Uri uri = Uri.parse(url);
                    if (DeepLink.isHierarchicalSoundCloudScheme(uri)) {
                        Intent intent = new Intent(Intent.ACTION_VIEW);
                        intent.setData(uri);
                        startActivity(intent);
                        return true;
                    }
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
        if (shouldWebViewGoBack() && webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }

    protected boolean shouldWebViewGoBack() {
        return true;
    }
}
