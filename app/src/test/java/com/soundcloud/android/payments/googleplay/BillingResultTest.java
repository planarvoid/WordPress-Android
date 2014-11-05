package com.soundcloud.android.payments.googleplay;

import static com.soundcloud.android.Expect.expect;

import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(SoundCloudTestRunner.class)
public class BillingResultTest {

    @Test
    public void isForRequestIfRequestCodeMatches() {
        BillingResult result = TestBillingResults.success();
        expect(result.isForRequest()).toBeTrue();
    }

    @Test
    public void isForRequestIsFalseForOtherRequestCodes() {
        BillingResult result = TestBillingResults.wrongRequest();
        expect(result.isForRequest()).toBeFalse();
    }

    @Test
    public void isOkIfBothActivityAndBillingResultsAreOk() {
        BillingResult result = TestBillingResults.success();
        expect(result.isOk()).toBeTrue();
    }

    @Test
    public void isOkIsFalseIfCancelled() {
        BillingResult result = TestBillingResults.cancelled();
        expect(result.isOk()).toBeFalse();
    }

    @Test
    public void isOkIsFalseIfBillingError() {
        BillingResult result = TestBillingResults.error();
        expect(result.isOk()).toBeFalse();
    }

    @Test
    public void failReasonForUserCancelled() {
        BillingResult result = TestBillingResults.cancelled();
        expect(result.getFailReason()).toEqual("user cancelled");
    }

    @Test
    public void failReasonForOtherError() {
        BillingResult result = TestBillingResults.error();
        expect(result.getFailReason()).toEqual("billing error: 6");
    }

}