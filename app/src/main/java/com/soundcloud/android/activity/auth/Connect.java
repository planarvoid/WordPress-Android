package com.soundcloud.android.activity.auth;

import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.activity.ScActivity;
import com.soundcloud.android.task.create.NewConnectionTask;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.net.http.SslError;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.webkit.CookieManager;
import android.webkit.CookieSyncManager;
import android.webkit.SslErrorHandler;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;

public class Connect extends ScActivity {

    private WebView mWebView;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.connect);
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

        mWebView.setScrollBarStyle(WebView.SCROLLBARS_OUTSIDE_OVERLAY); // fix white bar
        mWebView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                progress.setProgress(newProgress);
            }
        });

        mWebView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageStarted(WebView view, String u, Bitmap favicon) {
                if (url.equals(u) && !isFinishing()) progress.show();
            }

            @Override
            public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                showConnectionError(description);
            }

            @Override
            public void onReceivedSslError(WebView view, SslErrorHandler handler, SslError error) {
                handler.cancel();
                showConnectionError(error.toString());
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                if (!isFinishing() && progress.isShowing()) {
                    /*
                     this often throws an IllegalArgumentException even with the check,
                     probably because we are calling this in response to the webview lifecycle.
                      */
                    try {
                        progress.dismiss();
                    } catch (IllegalArgumentException e){
                        Log.e(SoundCloudApplication.TAG,"Error dismissing dialog: ", e);
                    }
                }
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

        removeAllCookies();
        mWebView.loadUrl(url);
    }

    @Override
    protected int getSelectedMenuId() {
        return -1;
    }

    private void removeAllCookies() {
        CookieSyncManager.createInstance(this);
        CookieManager.getInstance().removeAllCookie();
    }

     private void showConnectionError(final String message) {
        if (!isFinishing()) {
            new AlertDialog.Builder(this).
                    setMessage(message).
                    setIcon(android.R.drawable.ic_dialog_alert)
                    .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            finish();
                        }
                    }).create()
                    .show();
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
