package com.soundcloud.android.activity.auth;

import static com.soundcloud.android.SoundCloudApplication.TAG;

import com.soundcloud.android.R;
import com.soundcloud.api.CloudAPI;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;

public class Facebook extends Activity {
    public static final String URL_SCHEME = "soundcloud-facebook://";

    private WebView mWebView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.facebook);

        mWebView = (WebView) findViewById(R.id.webview);
        mWebView.getSettings().setJavaScriptEnabled(true);
        mWebView.getSettings().setBlockNetworkImage(false);
        mWebView.getSettings().setLoadsImagesAutomatically(true);


        final ProgressDialog progress = new ProgressDialog(this);
        progress.setIndeterminate(false);
        progress.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        progress.setTitle(R.string.connect_progress);
        progress.setMax(100);

        mWebView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                progress.setProgress(newProgress);
            }
        });

        final String url = "http://soundcloud.com/login/facebook/new"; //?redirect_uri="+URL_SCHEME+"android";

        mWebView.getSettings().setUserAgentString(CloudAPI.USER_AGENT);
        mWebView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageStarted(WebView view, String u, Bitmap favicon) {
                if (url.equals(u)) progress.show();
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                progress.dismiss();
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                Log.d(TAG, "shouldOverrideUrlLoading("+url+")");

                if (url.startsWith(URL_SCHEME)) {
                    Uri uri = Uri.parse(url);
                    boolean success = "1".equalsIgnoreCase(uri.getQueryParameter("success"));

                    setResult(RESULT_OK, new Intent()
                            .setData(uri)
                            .putExtra("success", success));
                    finish();
                }
                return false;
            }
        });
        mWebView.loadUrl(url);
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
