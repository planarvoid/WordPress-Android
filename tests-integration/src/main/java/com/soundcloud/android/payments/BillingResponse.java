package com.soundcloud.android.payments;

import android.app.Activity;
import android.content.Intent;

public final class BillingResponse {

    private static final int REQUEST_CODE = 1001;

    private static final String SUCCESS_DATA = "{\"orderId\":\"12999763169054705758.1343597682978365\",\"packageName\":\"com.soundcloud.android\",\"productId\":\"android_test_product\",\"purchaseTime\":1414161345678,\"purchaseState\":0,\"developerPayload\":\"soundcloud:payments:orders:e72661985b8911e49a4200e081c198e9\",\"purchaseToken\":\"%s\"}";
    private static final String SUCCESS_SIGNATURE = "signature";

    private Activity activity;

    private int responseCode;
    private Intent data;

    public BillingResponse(Activity activity) {
        this.activity = activity;
    }

    public BillingResponse forSuccess() {
        responseCode = Activity.RESULT_OK;
        data = buildPayload("VALID_" + System.currentTimeMillis());
        return this;
    }

    public BillingResponse forInvalid() {
        responseCode = Activity.RESULT_OK;
        data = buildPayload("INVALID_" + System.currentTimeMillis());
        return this;
    }

    public BillingResponse forCancel() {
        responseCode = Activity.RESULT_CANCELED;
        return this;
    }

    public  void insert() {
        final SubscribeActivity subscribe = (SubscribeActivity) activity;
        subscribe.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                subscribe.onActivityResult(REQUEST_CODE, responseCode, data);
            }
        });
    }

    private Intent buildPayload(String token) {
        Intent billingResponse = new Intent();
        billingResponse.putExtra("RESPONSE_CODE", 0);
        billingResponse.putExtra("INAPP_PURCHASE_DATA", String.format(SUCCESS_DATA, token));
        billingResponse.putExtra("INAPP_DATA_SIGNATURE", SUCCESS_SIGNATURE);
        return billingResponse;
    }

}
