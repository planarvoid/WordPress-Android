package com.soundcloud.android.payments.googleplay;

import android.app.Activity;
import android.content.Intent;

public final class TestBillingResults {

    public static PlayBillingResult success() {
        Intent content = new Intent();
        content.putExtra(PlayBillingUtil.RESPONSE_CODE, PlayBillingUtil.RESULT_OK);
        content.putExtra(PlayBillingUtil.RESPONSE_PURCHASE_DATA, "payload");
        content.putExtra(PlayBillingUtil.RESPONSE_SIGNATURE, "payload_signature");
        return new PlayBillingResult(PlayBillingResult.REQUEST_CODE, Activity.RESULT_OK, content);
    }

    public static PlayBillingResult wrongRequest() {
        return new PlayBillingResult(123, Activity.RESULT_OK, null);
    }

    public static PlayBillingResult error() {
        Intent content = new Intent();
        content.putExtra(PlayBillingUtil.RESPONSE_CODE, PlayBillingUtil.RESULT_ERROR);
        return new PlayBillingResult(PlayBillingResult.REQUEST_CODE, Activity.RESULT_OK, content);
    }

    public static PlayBillingResult cancelled() {
        return new PlayBillingResult(PlayBillingResult.REQUEST_CODE, Activity.RESULT_CANCELED, null);
    }

}
