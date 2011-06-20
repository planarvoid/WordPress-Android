package com.soundcloud.android.activity;

import com.soundcloud.android.R;

import android.app.Activity;
import android.os.Bundle;
import android.webkit.WebView;

public class About extends Activity {
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.about);

        WebView webview = (WebView) findViewById(R.id.about_webview);
        webview.loadUrl("file:///android_asset/about.html");
    }
}