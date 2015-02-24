package com.soundcloud.android.payments.googleplay;

import android.app.Activity;
import android.content.Intent;

public final class TestBillingResults {

    public static BillingResult success() {
        Intent content = new Intent();
        content.putExtra(BillingUtil.RESPONSE_CODE, BillingUtil.RESULT_OK);
        content.putExtra(BillingUtil.RESPONSE_PURCHASE_DATA, "payload");
        content.putExtra(BillingUtil.RESPONSE_SIGNATURE, "payload_signature");
        return new BillingResult(BillingResult.REQUEST_CODE, Activity.RESULT_OK, content);
    }

    public static BillingResult wrongRequest() {
        return new BillingResult(123, Activity.RESULT_OK, null);
    }

    public static BillingResult error() {
        Intent content = new Intent();
        content.putExtra(BillingUtil.RESPONSE_CODE, BillingUtil.RESULT_ERROR);
        return new BillingResult(BillingResult.REQUEST_CODE, Activity.RESULT_OK, content);
    }

    public static BillingResult cancelled() {
        return new BillingResult(BillingResult.REQUEST_CODE, Activity.RESULT_CANCELED, null);
    }

}
