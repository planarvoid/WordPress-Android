package com.soundcloud.android.activity;

import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.tracking.Page;
import com.soundcloud.android.tracking.Tracking;

import android.app.Activity;
import android.os.Bundle;
import android.webkit.WebView;


@Tracking(page = Page.Settings_about)
public class About extends Activity {
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.about);

        WebView webview = (WebView) findViewById(R.id.about_webview);
        webview.loadUrl("file:///android_asset/about.html");
    }

    @Override
    protected void onResume() {
        super.onResume();
        ((SoundCloudApplication)getApplication()).track(getClass());
    }
}