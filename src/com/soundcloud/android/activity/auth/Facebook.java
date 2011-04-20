package com.soundcloud.android.activity.auth;

import static com.soundcloud.android.SoundCloudApplication.TAG;

import com.soundcloud.android.AndroidCloudAPI;
import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.api.CloudAPI;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;

public class Facebook extends Activity {
    public static final String URL_SCHEME   = "soundcloud-facebook://android";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.facebook);

        WebView view = (WebView) findViewById(R.id.webview);
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
                    if (error == null) {
                        //soundcloud://auth?&state=&code=0000000NKReaxPXjIYHGBr34f8GNQobh
                        setResult(RESULT_OK, new Intent()
                                .setData(result)
                                .putExtra("state", result.getQueryParameter("state"))
                                .putExtra("code", result.getQueryParameter("code")));
                    } else {
                        setResult(RESULT_CANCELED, new Intent()
                                 .setData(result)
                                 .putExtra("error", error));
                    }
                    finish();
                    return true;
                }
                return false;
            }
        });
        view.loadUrl(facebookLogin);
    }
}
