package com.soundcloud.android.payments;

import butterknife.Bind;
import butterknife.ButterKnife;
import com.soundcloud.android.R;

import android.annotation.SuppressLint;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import javax.inject.Inject;

class WebCheckoutView {

    @Bind(R.id.payment_form) WebView webView;
    @Bind(R.id.loading) View loading;

    @Inject
    public WebCheckoutView() {}

    void setupContentView(AppCompatActivity activity) {
        ButterKnife.bind(this, activity);
        configureWebView();
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void configureWebView() {
        final WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setBlockNetworkImage(false);
        settings.setLoadsImagesAutomatically(true);
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                return false;
            }
        });
    }

    @SuppressLint("AddJavascriptInterface")
    public void setupJavaScriptInterface(String name, WebCheckoutInterface checkoutInterface) {
        webView.addJavascriptInterface(checkoutInterface, name);
    }

    public void loadUrl(String url) {
        webView.loadUrl(url);
    }

    public void setLoading(boolean isLoading) {
        loading.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        webView.setVisibility(isLoading ? View.INVISIBLE : View.VISIBLE);
    }

    public boolean handleBackPress() {
        if (webView.canGoBack()) {
            webView.goBack();
            return true;
        } else {
            return false;
        }
    }

}
