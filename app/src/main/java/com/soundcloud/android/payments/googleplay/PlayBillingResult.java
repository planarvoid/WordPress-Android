package com.soundcloud.android.payments.googleplay;

import android.app.Activity;
import android.content.Intent;

public class PlayBillingResult {

    public static final int REQUEST_CODE = 1001;

    private static final String FAIL_REASON_CANCELLED = "user cancelled";
    private static final String FAIL_REASON_ERROR = "billing error: ";
    private static final String FAIL_REASON_UNKNOWN = "unknown";

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
        return resultCode == Activity.RESULT_OK && isBillingResultOk();
    }

    private boolean isBillingResultOk() {
        return PlayBillingUtil.getResponseCodeFromIntent(data) == PlayBillingUtil.RESULT_OK;
    }

    public String getData() {
        return data.getStringExtra(PlayBillingUtil.RESPONSE_PURCHASE_DATA);
    }

    public String getSignature() {
        return data.getStringExtra(PlayBillingUtil.RESPONSE_SIGNATURE);
    }

    public String getFailReason() {
        if (resultCode == Activity.RESULT_CANCELED) {
            return FAIL_REASON_CANCELLED;
        } else if (!isBillingResultOk()) {
            return FAIL_REASON_ERROR + PlayBillingUtil.getResponseCodeFromIntent(data);
        } else {
            return FAIL_REASON_UNKNOWN;
        }
    }

}
