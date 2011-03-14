package com.soundcloud.android.activity;

import com.soundcloud.android.R;
import com.soundcloud.android.task.NewConnectionTask;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.view.KeyEvent;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;

public class Connect extends Activity {
    public static final int MAKE_CONNECTION = 1000;

    private WebView mWebView;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.authorize);
        mWebView = (WebView) findViewById(R.id.webview);
        mWebView.getSettings().setJavaScriptEnabled(true);
        mWebView.getSettings().setBlockNetworkImage(false);
        mWebView.getSettings().setLoadsImagesAutomatically(true);

        final String service =
                getIntent().hasExtra("service") ?
                        getIntent().getStringExtra("service") : null;

        final String url = getIntent().getDataString();

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
                if (url.startsWith(NewConnectionTask.URL_SCHEME)) {
                    Uri uri = Uri.parse(url);
                    boolean success = "1".equalsIgnoreCase(uri.getQueryParameter("success"));

                    setResult(RESULT_OK, new Intent()
                            .setData(uri)
                            .putExtra("success", success)
                            .putExtra("service", service));
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
