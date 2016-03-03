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
import android.widget.Button;

import javax.inject.Inject;

class WebCheckoutView {

    @Bind(R.id.payment_form) WebView webView;
    @Bind(R.id.loading) View loading;
    @Bind(R.id.retry) View retry;
    @Bind(R.id.retry_button) Button retryButton;

    interface Listener {
        void onRetry();
    }

    @Inject
    public WebCheckoutView() {}

    void setupContentView(AppCompatActivity activity, final Listener listener) {
        ButterKnife.bind(this, activity);
        configureWebView();
        retryButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                listener.onRetry();
            }
        });
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
        retry.setVisibility(View.GONE);
        loading.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        webView.setVisibility(isLoading ? View.INVISIBLE : View.VISIBLE);
    }

    public void setRetry() {
        loading.setVisibility(View.GONE);
        retry.setVisibility(View.VISIBLE);
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
