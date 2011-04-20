package com.soundcloud.android.activity.auth;

import static com.soundcloud.android.SoundCloudApplication.TAG;

import com.soundcloud.android.AndroidCloudAPI;
import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.utils.CloudUtils;
import com.soundcloud.api.CloudAPI;

import android.app.ProgressDialog;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;

public class Facebook extends AuthenticationActivity {
    private static final String URL_SCHEME   = "soundcloud-facebook://android";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.facebook);

        final WebView view = (WebView) findViewById(R.id.webview);
        view.getSettings().setJavaScriptEnabled(true);
        view.getSettings().setBlockNetworkImage(false);
        view.getSettings().setLoadsImagesAutomatically(true);

        final ProgressDialog progress = new ProgressDialog(this);
        progress.setIndeterminate(false);
        progress.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        progress.setTitle(R.string.connect_progress);
        progress.setMax(100);

        view.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                progress.setProgress(newProgress);
            }
        });

        final SoundCloudApplication app = (SoundCloudApplication) getApplication();
        final String host = app.getEnvironment().host.toHostString();
        final String facebookLogin = "http://"+host+"/login/facebook/new?redirect_uri="+URL_SCHEME;
        final String oldUserAgent = view.getSettings().getUserAgentString();
        view.getSettings().setUserAgentString(CloudAPI.USER_AGENT);
        view.setWebViewClient(new WebViewClient() {
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
                if (url.startsWith(URL_SCHEME)) {
                    view.stopLoading();
                    view.post(new Runnable() {
                        @Override
                        public void run() {
                            view.getSettings().setUserAgentString(oldUserAgent);
                            view.loadUrl(app.getConnectUrl());
                        }
                    });
                    return true;
                } else if (url.startsWith(AndroidCloudAPI.REDIRECT_URI)) {
                    Log.d(TAG, "got " + url);
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
        view.loadUrl(facebookLogin);
    }
}
