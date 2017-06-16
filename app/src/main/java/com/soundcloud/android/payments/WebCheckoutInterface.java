package com.soundcloud.android.payments;

import android.webkit.JavascriptInterface;

class WebCheckoutInterface {
    static final String JAVASCRIPT_OBJECT_NAME = "AndroidApp";

    private final Listener listener;

    interface Listener {
        void onWebAppReady();

        void onPaymentSuccess();

        void onPaymentError(String errorType);
    }

    WebCheckoutInterface(Listener listener) {
        this.listener = listener;
    }

    @JavascriptInterface
    public void onWebAppReady() {
        listener.onWebAppReady();
    }

    @JavascriptInterface
    public void onPaymentSuccess() {
        listener.onPaymentSuccess();
    }

    @JavascriptInterface
    public void onPaymentError(String errorType) {
        listener.onPaymentError(errorType);
    }
}
