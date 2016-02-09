package com.soundcloud.android.payments;

import android.webkit.JavascriptInterface;

class WebCheckoutInterface {

    public static final String PAYMENT_FORM_URL = "https://soundcloud.com/android_payment.html";
    public static final String JAVASCRIPT_OBJECT_NAME = "androidApp";

    private final Listener listener;
    private final String token;
    private final WebProduct product;

    interface Listener {
        void onLoad();
        void onPaymentSuccess();
    }

    public WebCheckoutInterface(Listener listener, String token, WebProduct product) {
        this.listener = listener;
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
        listener.onLoad();
    }

    @JavascriptInterface
    public void onPaymentSuccess() {
        listener.onPaymentSuccess();
    }

}
