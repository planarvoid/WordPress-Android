package com.soundcloud.android.activity;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.task.NewConnectionTask;

public class ConnectActivity extends ScActivity {
    static final String TAG = SoundCloudApplication.TAG;

    private WebView mWebView;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.authorize);
        mWebView = (WebView) findViewById(R.id.webview);
        mWebView.getSettings().setJavaScriptEnabled(true);
        mWebView.getSettings().setBlockNetworkImage(false);
        mWebView.getSettings().setLoadsImagesAutomatically(true);

        mWebView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                if (url.startsWith(NewConnectionTask.URL_SCHEME)) {
                    Uri uri = Uri.parse(url);
                    Log.v(TAG, "callback: " + uri);
                    boolean success = "1".equalsIgnoreCase(uri.getQueryParameter("success"));

                    if (success) showToast("Success");

                    startActivity(
                        (new Intent(ConnectActivity.this, Dashboard.class))
                            .setData(uri)
                            .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP));
                }

                return false;
            }
        });

        Intent i = getIntent();
        if (i.getData() != null) {
            Log.d(TAG, "loading " + i.getDataString());
            mWebView.loadUrl(i.getDataString());
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if ((keyCode == KeyEvent.KEYCODE_BACK) && mWebView.canGoBack()) {
            mWebView.goBack();
            return true;
        } else {
            return super.onKeyDown(keyCode, event);
        }
    }
}
