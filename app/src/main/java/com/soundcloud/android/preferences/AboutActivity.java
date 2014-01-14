package com.soundcloud.android.preferences;

import com.soundcloud.android.R;

import android.app.Activity;
import android.os.Bundle;
import android.webkit.WebView;

public class AboutActivity extends Activity {
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.about);

        WebView webview = (WebView) findViewById(R.id.about_webview);
        webview.loadUrl("file:///android_asset/about.html");
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    public boolean onNavigateUp() {
        finish();
        return true;
    }
}