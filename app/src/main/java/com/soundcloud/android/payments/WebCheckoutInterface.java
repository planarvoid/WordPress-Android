package com.soundcloud.android.payments;

import android.webkit.JavascriptInterface;

class WebCheckoutInterface {
    public static final String JAVASCRIPT_OBJECT_NAME = "AndroidApp";

    private final Listener listener;

    interface Listener {
        void onFormReady();
        void onPaymentSuccess();
    }

    public WebCheckoutInterface(Listener listener) {
        this.listener = listener;
    }

    @JavascriptInterface
    public void onFormReady() {
        listener.onFormReady();
    }

    @JavascriptInterface
    public void onPaymentSuccess() {
        listener.onPaymentSuccess();
    }
}
