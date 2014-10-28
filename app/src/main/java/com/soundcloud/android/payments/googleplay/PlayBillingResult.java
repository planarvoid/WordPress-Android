package com.soundcloud.android.payments.googleplay;

import android.app.Activity;
import android.content.Intent;

public class PlayBillingResult {

    public static final int REQUEST_CODE = 1001;

    private final int requestCode;
    private final int resultCode;
    private final Intent data;

    public PlayBillingResult(int requestCode, int responseCode, Intent data) {
        this.requestCode = requestCode;
        this.resultCode = responseCode;
        this.data = data;
    }

    public boolean isForRequest() {
        return requestCode == REQUEST_CODE;
    }

    public boolean isOk() {
        return resultCode == Activity.RESULT_OK
                && data != null
                && PlayBillingUtil.getResponseCodeFromIntent(data) == PlayBillingUtil.RESULT_OK;
    }

    public String getData() {
        return data.getStringExtra(PlayBillingUtil.RESPONSE_PURCHASE_DATA);
    }

    public String getSignature() {
        return data.getStringExtra(PlayBillingUtil.RESPONSE_SIGNATURE);
    }

}
