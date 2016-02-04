package com.soundcloud.android.payments;

import butterknife.Bind;
import butterknife.ButterKnife;
import com.soundcloud.android.Navigator;
import com.soundcloud.android.R;
import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.UpgradeTrackingEvent;
import com.soundcloud.lightcycle.DefaultActivityLightCycle;
import com.soundcloud.rx.eventbus.EventBus;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import javax.inject.Inject;

class WebCheckoutPresenter extends DefaultActivityLightCycle<AppCompatActivity> {

    // TODO: Switch to final, non-staging URL once form has been deployed
    public static final String PAYMENT_FORM_URL = "https://soundcloud.com/android_payment.html?stage=diurnalist";
    public static final String JAVASCRIPT_OBJECT_NAME = "androidApp";

    @Bind(R.id.payment_form) WebView webView;
    @Bind(R.id.loading) View loading;

    private final AccountOperations operations;
    private final Navigator navigator;
    private final EventBus eventBus;

    private Activity activity;

    @Inject
    public WebCheckoutPresenter(AccountOperations operations, Navigator navigator, EventBus eventBus) {
        this.operations = operations;
        this.navigator = navigator;
        this.eventBus = eventBus;
    }

    @SuppressLint("AddJavascriptInterface")
    @Override
    public void onCreate(AppCompatActivity activity, Bundle bundle) {
        ButterKnife.bind(this, activity);
        this.activity = activity;

        final PaymentFormInterface paymentFormInterface = new PaymentFormInterface(
                operations.getSoundCloudToken().getAccessToken(),
                (WebProduct) activity.getIntent().getParcelableExtra(WebConversionPresenter.PRODUCT_INFO));

        generalWebViewConfig();
        webView.addJavascriptInterface(paymentFormInterface, JAVASCRIPT_OBJECT_NAME);
        webView.loadUrl(PAYMENT_FORM_URL);
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void generalWebViewConfig() {
        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setDomStorageEnabled(true);
        webView.getSettings().setBlockNetworkImage(false);
        webView.getSettings().setLoadsImagesAutomatically(true);
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                return false;
            }
        });
    }

    public boolean onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack();
            return true;
        } else {
            return false;
        }
    }

    @Override
    public void onDestroy(AppCompatActivity activity) {
        this.activity = null;
    }

    private void setLoading(boolean isLoading) {
        loading.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        webView.setVisibility(isLoading ? View.INVISIBLE : View.VISIBLE);
    }

    private class PaymentFormInterface {
        private final String token;
        private final WebProduct product;

        public PaymentFormInterface(String token, WebProduct product) {
            this.token = token;
            this.product = product;
        }

        @JavascriptInterface
        public String getToken() {
            return token;
        }

        @JavascriptInterface
        public String getPrice() {
            return product.getPrice();
        }

        @JavascriptInterface
        public int getTrialDays() {
            return product.getTrialDays();
        }

        @JavascriptInterface
        public String getExpiryDate() {
            return product.getExpiryDate();
        }

        @JavascriptInterface
        public String getPackageUrn() {
            return product.getPackageUrn();
        }

        @JavascriptInterface
        public void onLoad() {
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    setLoading(false);
                }
            });
        }

        @JavascriptInterface
        public void onPaymentSuccess() {
            eventBus.publish(EventQueue.TRACKING, UpgradeTrackingEvent.forUpgradeSuccess());

            navigator.restartForAccountUpgrade(activity);
            activity.finish();
        }
    }
}
