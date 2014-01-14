package com.soundcloud.android.preferences;

import com.soundcloud.android.R;
import com.soundcloud.android.main.TrackedActivity;

import android.os.Bundle;
import android.webkit.WebView;

public class AboutActivity extends TrackedActivity {

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.about);

        WebView webview = (WebView) findViewById(R.id.about_webview);
        webview.loadUrl("file:///android_asset/about.html");
    }

    @Override
    public boolean onNavigateUp() {
        finish();
        return true;
    }
}