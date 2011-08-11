package com.soundcloud.android.utils;

import android.graphics.Bitmap;
import android.net.http.SslError;
import android.os.Message;
import android.util.Log;
import android.view.KeyEvent;
import android.webkit.HttpAuthHandler;
import android.webkit.SslErrorHandler;
import android.webkit.WebView;
import android.webkit.WebViewClient;

/** Useful for debugging WebView problems */
@SuppressWarnings({"UnusedDeclaration"})
public class LoggingWebViewClient extends WebViewClient {

    private void log(String s) {
        Log.d("LoggingWebViewClient", s);
    }

    @Override
    public boolean shouldOverrideUrlLoading(WebView view, String url) {
        log("shouldOverrideUrlLoading(" + url + ")");
        return super.shouldOverrideUrlLoading(view, url);
    }

    @Override
    public void onPageStarted(WebView view, String url, Bitmap favicon) {
        log("onPageStarted(" + url + ")");
        super.onPageStarted(view, url, favicon);
    }

    @Override
    public void onPageFinished(WebView view, String url) {
        log("onPageFinished(" + url + ")");
        super.onPageFinished(view, url);
    }

    @Override
    public void onLoadResource(WebView view, String url) {
        log("onLoadResource(" + url + ")");
        super.onLoadResource(view, url);
    }

    @Override
    public void onTooManyRedirects(WebView view, Message cancelMsg, Message continueMsg) {
        log("onTooManyRedirects(" + cancelMsg + "," + continueMsg + ")");
        super.onTooManyRedirects(view, cancelMsg, continueMsg);
    }

    @Override
    public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
        log("onReceivedError(" + errorCode + "," + description + "," + failingUrl + ")");
        super.onReceivedError(view, errorCode, description, failingUrl);
    }

    @Override
    public void onFormResubmission(WebView view, Message dontResend, Message resend) {
        log("onFormResubmission(" + dontResend + "," + resend + ")");
        super.onFormResubmission(view, dontResend, resend);
    }

    @Override
    public void doUpdateVisitedHistory(WebView view, String url, boolean isReload) {
        log("doUpdateVisitedHistory(" + url + "," + isReload + ")");
        super.doUpdateVisitedHistory(view, url, isReload);
    }

    @Override
    public void onReceivedSslError(WebView view, SslErrorHandler handler, SslError error) {
        log("onReceivedSslError(" + handler + "," + error + ")");
        super.onReceivedSslError(view, handler, error);
    }

    @Override
    public void onReceivedHttpAuthRequest(WebView view, HttpAuthHandler handler, String host, String realm) {
        log("onReceivedHttpAuthRequest(" + handler + "," + host + "," + realm + ")");
        super.onReceivedHttpAuthRequest(view, handler, host, realm);
    }

    @Override
    public boolean shouldOverrideKeyEvent(WebView view, KeyEvent event) {
        log("shouldOverrideKeyEvent(" + event + ")");
        return super.shouldOverrideKeyEvent(view, event);
    }

    @Override
    public void onUnhandledKeyEvent(WebView view, KeyEvent event) {
        log("onUnhandledKeyEvent(" + event + ")");
        super.onUnhandledKeyEvent(view, event);
    }

    @Override
    public void onScaleChanged(WebView view, float oldScale, float newScale) {
        log("onScaleChanged(" + oldScale + "," + newScale + ")");
        super.onScaleChanged(view, oldScale, newScale);
    }
}
