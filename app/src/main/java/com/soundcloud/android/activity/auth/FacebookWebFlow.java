package com.soundcloud.android.activity.auth;

import com.soundcloud.android.AndroidCloudAPI;
import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.utils.IOUtils;
import com.soundcloud.api.Endpoints;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.net.http.SslError;
import android.os.Bundle;
import android.text.TextUtils;
import android.webkit.CookieManager;
import android.webkit.CookieSyncManager;
import android.webkit.SslErrorHandler;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;

@SuppressLint("NewApi")
public class FacebookWebFlow extends AbstractLoginActivity {
    private WebView mWebview;

    protected void build() {
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

        mWebview.setScrollBarStyle(WebView.SCROLLBARS_OUTSIDE_OVERLAY); // fix white bar
        mWebview.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                progress.setProgress(newProgress);
            }
        });

        mWebview.setWebViewClient(new WebViewClient() {
            @Override
            public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                showConnectionError(description);
            }

            @Override
            public void onReceivedSslError(WebView view, SslErrorHandler handler, SslError error) {
                handler.cancel();
                showConnectionError(getString(R.string.authentication_log_in_with_facebook_ssl_error));
            }

            @Override
            public void onPageStarted(WebView view, String u, Bitmap favicon) {
                showDialog(progress);
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                dismissDialog(progress);
            }

            @Override
            public boolean shouldOverrideUrlLoading(final WebView view, String url) {
                if (url.startsWith(AndroidCloudAPI.REDIRECT_URI.toString())) {
                    Uri result = Uri.parse(url);
                    String error = result.getQueryParameter("error");
                    String code = result.getQueryParameter("code");

                    if (!TextUtils.isEmpty(code) && error == null) {
                        Bundle params = new Bundle();
                        params.putString(CODE_EXTRA, code);
                        if ("1".equals(result.getQueryParameter("signed_up"))) {
                            params.putString(SignupVia.EXTRA, SignupVia.FACEBOOK_WEBFLOW.name);
                        }
                        login(params);
                    } else {
                        new AlertDialog.Builder(FacebookWebFlow.this)
                                .setTitle(R.string.facebook_authentication_failed_title)
                                .setMessage(R.string.facebook_authentication_failed_message)
                                .setIcon(android.R.drawable.ic_dialog_alert)
                                .setPositiveButton(android.R.string.ok, new Dialog.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        finish();
                                    }
                                })
                                .show();
                    }
                    return true;
                } else if (url.startsWith("http://www.facebook.com/apps") || /* link to app */
                           url.contains("/r.php") ||    /* signup */
                           url.contains("/reset.php"))  /* password reset */
                {
                    // launch in external browser
                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
                    return true;
                } else {
                    return false;
                }
            }
        });

        if (IOUtils.isConnected(this)) {
            final SoundCloudApplication app = (SoundCloudApplication) getApplication();
            removeAllCookies();
            String[] options = new String[SCOPES_TO_REQUEST.length+1];
            options[0] = Endpoints.FACEBOOK_CONNECT;
            System.arraycopy(SCOPES_TO_REQUEST, 0, options, 1, SCOPES_TO_REQUEST.length);

            mWebview.loadUrl(app.authorizationCodeUrl(options).toString());
        } else {
            showConnectionError(null);
        }
    }

    private void showConnectionError(final String message) {
        if (isFinishing()) return;

        String error = getString(R.string.facebook_authentication_error_no_connection_message);
        if (!TextUtils.isEmpty(message)) {
            error += " ("+message+")";
        }
        new AlertDialog.Builder(this).
            setMessage(error).
            setTitle(R.string.authentication_error_no_connection_title).
            setIcon(android.R.drawable.ic_dialog_alert).
            setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    finish();
                }
            }).
            create().
            show();
    }

    private void removeAllCookies() {
        CookieSyncManager.createInstance(this);
        CookieManager.getInstance().removeAllCookie();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mWebview.stopLoading();
    }
}