package com.soundcloud.android.payments;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;

public final class BillingResponse {

    private static final int REQUEST_CODE = 1001;
    private static final String SUCCESS_SIGNATURE = "signature";

    private Activity activity;

    private int responseCode;
    private Intent data;

    public BillingResponse(Activity activity) {
        this.activity = activity;
    }

    public BillingResponse forSuccess() {
        responseCode = Activity.RESULT_OK;
        data = buildPayload(FakeResult.valid(getCheckoutUrn()));
        return this;
    }

    public BillingResponse forInvalid() {
        responseCode = Activity.RESULT_OK;
        data = buildPayload(FakeResult.invalid(getCheckoutUrn()));
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

    private String getCheckoutUrn() {
        return activity.getSharedPreferences("payments", Context.MODE_PRIVATE).getString("pending_transaction_urn", null);
    }

    private Intent buildPayload(FakeResult result) {
        Intent billingResponse = new Intent();
        billingResponse.putExtra("RESPONSE_CODE", 0);
        billingResponse.putExtra("INAPP_PURCHASE_DATA", serialize(result));
        billingResponse.putExtra("INAPP_DATA_SIGNATURE", SUCCESS_SIGNATURE);
        return billingResponse;
    }

    private String serialize(FakeResult result) {
        try {
            return new ObjectMapper().writeValueAsString(result);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Could not process BillingResponse JSON");
        }
    }

}
