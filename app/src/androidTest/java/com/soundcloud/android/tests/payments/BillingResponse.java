package com.soundcloud.android.tests.payments;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.soundcloud.android.payments.UpgradeActivity;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.support.annotation.Nullable;

final class BillingResponse {

    private static final int REQUEST_CODE = 1001;
    private static final String SUCCESS_SIGNATURE = "signature";

    private enum ResponseType {
        SUCCESS, INVALID, CANCELLED
    }

    private final ResponseType responseType;

    private BillingResponse(ResponseType responseType) {
        this.responseType = responseType;
    }

    public static BillingResponse success() {
        return new BillingResponse(ResponseType.SUCCESS);
    }

    public static BillingResponse invalid() {
        return new BillingResponse(ResponseType.INVALID);
    }

    public static BillingResponse cancelled() {
        return new BillingResponse(ResponseType.CANCELLED);
    }

    public void insertInto(final Activity activity) {
        final UpgradeActivity subscribe = (UpgradeActivity) activity;
        subscribe.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                subscribe.onActivityResult(REQUEST_CODE, getResponseCode(), getData(activity));
            }
        });
    }

    private int getResponseCode() {
        switch (responseType) {
            case SUCCESS:
            case INVALID:
                return Activity.RESULT_OK;
            case CANCELLED:
            default:
                return Activity.RESULT_CANCELED;
        }
    }

    @Nullable
    private Intent getData(Context context) {
        final String checkourUrn = loadCheckoutUrn(context);
        switch (responseType) {
            case SUCCESS:
                return buildPayload(FakeResult.valid(checkourUrn));
            case INVALID:
                return buildPayload(FakeResult.invalid(checkourUrn));
            case CANCELLED:
            default:
                return null;
        }
    }

    private String loadCheckoutUrn(Context context) {
        return context.getSharedPreferences("payments", Context.MODE_PRIVATE).getString("pending_transaction_urn", null);
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
