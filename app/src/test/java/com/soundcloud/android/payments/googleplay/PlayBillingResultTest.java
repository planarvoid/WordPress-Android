package com.soundcloud.android.payments.googleplay;

import static com.soundcloud.android.Expect.expect;

import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import android.app.Activity;
import android.content.Intent;

@RunWith(SoundCloudTestRunner.class)
public class PlayBillingResultTest {

    private Intent success;
    private Intent fail;

    @Before
    public void setUp() throws Exception {
        success = new Intent();
        success.putExtra(PlayBillingUtil.RESPONSE_CODE, PlayBillingUtil.RESULT_OK);

        fail = new Intent();
        fail.putExtra(PlayBillingUtil.RESPONSE_CODE, PlayBillingUtil.RESULT_ERROR);
    }

    @Test
    public void isForRequestIfRequestCodeMatches() {
        PlayBillingResult result = new PlayBillingResult(PlayBillingResult.REQUEST_CODE, Activity.RESULT_OK, success);
        expect(result.isForRequest()).toBeTrue();
    }

    @Test
    public void isForRequestIsFalseForOtherRequestCodes() {
        PlayBillingResult result = new PlayBillingResult(1, Activity.RESULT_OK, success);
        expect(result.isForRequest()).toBeFalse();
    }

    @Test
    public void isOkIfBothActivityAndBillingResultsAreOk() {
        PlayBillingResult result = new PlayBillingResult(PlayBillingResult.REQUEST_CODE, Activity.RESULT_OK, success);
        expect(result.isOk()).toBeTrue();
    }

    @Test
    public void isOkIsFalseIfCancelled() {
        PlayBillingResult result = new PlayBillingResult(PlayBillingResult.REQUEST_CODE, Activity.RESULT_CANCELED, fail);
        expect(result.isOk()).toBeFalse();
    }

    @Test
    public void isOkIsFalseIfBillingError() {
        PlayBillingResult result = new PlayBillingResult(PlayBillingResult.REQUEST_CODE, Activity.RESULT_OK, fail);
        expect(result.isOk()).toBeFalse();
    }

}