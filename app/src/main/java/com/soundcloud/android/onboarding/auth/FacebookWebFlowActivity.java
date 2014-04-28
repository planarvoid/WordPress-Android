package com.soundcloud.android.onboarding.auth;

import com.soundcloud.android.api.PublicCloudAPI;
import com.soundcloud.android.R;
import com.soundcloud.android.api.PublicApi;
import com.soundcloud.android.utils.IOUtils;
import com.soundcloud.api.Endpoints;
import org.jetbrains.annotations.Nullable;

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

public class FacebookWebFlowActivity extends FacebookBaseActivity {
    private WebView webview;
    private PublicCloudAPI publicCloudAPI;

    //FIXME: way too long, break this method up
    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);

        setContentView(R.layout.facebook);
        publicCloudAPI = new PublicApi(this);

        webview = (WebView) findViewById(R.id.webview);
        webview.getSettings().setJavaScriptEnabled(true);
        webview.getSettings().setBlockNetworkImage(false);
        webview.getSettings().setLoadsImagesAutomatically(true);

        final ProgressDialog progress = new ProgressDialog(this);
        progress.setIndeterminate(false);
        progress.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        progress.setTitle(R.string.connect_progress);
        progress.setMax(100);

        webview.setScrollBarStyle(WebView.SCROLLBARS_OUTSIDE_OVERLAY); // fix white bar
        webview.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                progress.setProgress(newProgress);
            }
        });

        webview.setWebViewClient(new WebViewClient() {
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
                if (!isFinishing()) {
                    progress.show();
                }
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                if (!isFinishing() && progress != null) {
                    try {
                        progress.dismiss();
                    } catch (IllegalArgumentException ignored) {
                    }
                }
            }

            @Override
            public boolean shouldOverrideUrlLoading(final WebView view, String url) {
                if (url.startsWith(PublicCloudAPI.ANDROID_REDIRECT_URI.toString())) {
                    Uri result = Uri.parse(url);
                    String error = result.getQueryParameter("error");
                    String code = result.getQueryParameter("code");

                    if (!TextUtils.isEmpty(code) && error == null) {
                        Bundle params = new Bundle();
                        params.putString(TokenInformationGenerator.TokenKeys.CODE_EXTRA, code);
                        if ("1".equals(result.getQueryParameter("signed_up"))) {
                            params.putString(SignupVia.EXTRA, SignupVia.FACEBOOK_WEBFLOW.name);
                        }
                        login(params);
                    } else {
                        new AlertDialog.Builder(FacebookWebFlowActivity.this)
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
                        url.contains("/reset.php"))  /* password reset */ {
                    // launch in external browser
                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
                    return true;
                } else {
                    return false;
                }
            }
        });

        if (IOUtils.isConnected(this)) {
            removeAllCookies();
            String[] options = new String[TokenInformationGenerator.DEFAULT_SCOPES.length+1];
            options[0] = Endpoints.FACEBOOK_CONNECT;
            System.arraycopy(TokenInformationGenerator.DEFAULT_SCOPES, 0, options, 1, TokenInformationGenerator.DEFAULT_SCOPES.length);

            webview.loadUrl(publicCloudAPI.authorizationCodeUrl(options).toString());
        } else {
            showConnectionError(null);
        }
    }

    private void showConnectionError(@Nullable final String message) {
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
        webview.stopLoading();
    }

    // for testing
    public WebView getWebView() {
        return webview;
    }
}