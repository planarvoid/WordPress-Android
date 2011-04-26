package com.soundcloud.android.activity.auth;

import com.soundcloud.android.AndroidCloudAPI;
import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.utils.CloudUtils;

import android.app.ProgressDialog;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import java.net.URI;

public class Facebook extends LoginActivity {
    private WebView mWebview;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.facebook);

        mWebview = (WebView) findViewById(R.id.webview);
        mWebview.getSettings().setJavaScriptEnabled(true);
        mWebview.getSettings().setBlockNetworkImage(false);
        mWebview.getSettings().setLoadsImagesAutomatically(true);

        final ProgressDialog progress = new ProgressDialog(this);
        progress.setIndeterminate(false);
        progress.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        progress.setTitle(R.string.connect_progress);
        progress.setMax(100);

        mWebview.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                progress.setProgress(newProgress);
            }
        });

        final SoundCloudApplication app = (SoundCloudApplication) getApplication();
        final URI facebookLogin = app.loginViaFacebook();
        mWebview.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageStarted(WebView view, String u, Bitmap favicon) {
                progress.show();
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                progress.dismiss();
            }

            @Override
            public boolean shouldOverrideUrlLoading(final WebView view, String url) {
                if (url.startsWith(AndroidCloudAPI.REDIRECT_URI.toString())) {
                    Uri result = Uri.parse(url);
                    String error = result.getQueryParameter("error");
                    String code = result.getQueryParameter("code");
                    if (!TextUtils.isEmpty(code) && error == null) {
                        login(code);
                    } else {
                        CloudUtils.showToast(Facebook.this, error);
                        finish();
                    }
                    return true;
                } else {
                    return false;
                }
            }
        });
        mWebview.loadUrl(facebookLogin.toString());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mWebview.stopLoading();
    }
}