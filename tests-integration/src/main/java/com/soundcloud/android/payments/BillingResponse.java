package com.soundcloud.android.payments;

import android.app.Activity;
import android.content.Intent;

public final class BillingResponse {

    private static final int REQUEST_CODE = 1001;

    private static final String SUCCESS_DATA = "{\"orderId\":\"12999763169054705758.1343597682978365\",\"packageName\":\"com.soundcloud.android\",\"productId\":\"android_test_product\",\"purchaseTime\":1414161345678,\"purchaseState\":0,\"developerPayload\":\"soundcloud:payments:orders:e72661985b8911e49a4200e081c198e9\",\"purchaseToken\":\"%s\"}";
    private static final String SUCCESS_SIGNATURE = "signature";

    private BillingResponse() {}

    public static void valid(Activity activity) {
        String token = "VALID_" + System.currentTimeMillis();
        insertResult(activity, Activity.RESULT_OK, buildPayload(token));
    }

    public static void invalid(Activity activity) {
        String token = "INVALID_" + System.currentTimeMillis();
        insertResult(activity, Activity.RESULT_OK, buildPayload(token));
    }

    public static Intent buildPayload(String token) {
        Intent billingResponse = new Intent();
        billingResponse.putExtra("RESPONSE_CODE", 0);
        billingResponse.putExtra("INAPP_PURCHASE_DATA", String.format(SUCCESS_DATA, token));
        billingResponse.putExtra("INAPP_DATA_SIGNATURE", SUCCESS_SIGNATURE);
        return billingResponse;
    }

    public static void cancel(Activity activity) {
        insertResult(activity, Activity.RESULT_CANCELED, null);
    }

    private static void insertResult(final Activity activity, final int responseCode, final Intent data) {
        final SubscribeActivity subscribe = (SubscribeActivity) activity;
        subscribe.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                subscribe.onActivityResult(REQUEST_CODE, responseCode, data);
            }
        });
    }

}
