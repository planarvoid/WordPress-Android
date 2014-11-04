package com.soundcloud.android.payments.googleplay;

import static com.soundcloud.android.Expect.expect;

import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(SoundCloudTestRunner.class)
public class PlayBillingResultTest {

    @Test
    public void isForRequestIfRequestCodeMatches() {
        PlayBillingResult result = TestBillingResults.success();
        expect(result.isForRequest()).toBeTrue();
    }

    @Test
    public void isForRequestIsFalseForOtherRequestCodes() {
        PlayBillingResult result = TestBillingResults.wrongRequest();
        expect(result.isForRequest()).toBeFalse();
    }

    @Test
    public void isOkIfBothActivityAndBillingResultsAreOk() {
        PlayBillingResult result = TestBillingResults.success();
        expect(result.isOk()).toBeTrue();
    }

    @Test
    public void isOkIsFalseIfCancelled() {
        PlayBillingResult result = TestBillingResults.cancelled();
        expect(result.isOk()).toBeFalse();
    }

    @Test
    public void isOkIsFalseIfBillingError() {
        PlayBillingResult result = TestBillingResults.error();
        expect(result.isOk()).toBeFalse();
    }

    @Test
    public void failReasonForUserCancelled() {
        PlayBillingResult result = TestBillingResults.cancelled();
        expect(result.getFailReason()).toEqual("user cancelled");
    }

    @Test
    public void failReasonForOtherError() {
        PlayBillingResult result = TestBillingResults.error();
        expect(result.getFailReason()).toEqual("billing error: 6");
    }

}