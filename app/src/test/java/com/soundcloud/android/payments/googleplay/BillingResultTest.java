package com.soundcloud.android.payments.googleplay;

import static org.assertj.core.api.Assertions.assertThat;

import com.soundcloud.android.testsupport.AndroidUnitTest;
import org.junit.Test;

public class BillingResultTest extends AndroidUnitTest {

    @Test
    public void isForRequestIfRequestCodeMatches() {
        BillingResult result = TestBillingResults.success();
        assertThat(result.isForRequest()).isTrue();
    }

    @Test
    public void isForRequestIsFalseForOtherRequestCodes() {
        BillingResult result = TestBillingResults.wrongRequest();
        assertThat(result.isForRequest()).isFalse();
    }

    @Test
    public void isOkIfBothActivityAndBillingResultsAreOk() {
        BillingResult result = TestBillingResults.success();
        assertThat(result.isOk()).isTrue();
    }

    @Test
    public void isOkIsFalseIfCancelled() {
        BillingResult result = TestBillingResults.cancelled();
        assertThat(result.isOk()).isFalse();
    }

    @Test
    public void isOkIsFalseIfBillingError() {
        BillingResult result = TestBillingResults.error();
        assertThat(result.isOk()).isFalse();
    }

    @Test
    public void failReasonForUserCancelled() {
        BillingResult result = TestBillingResults.cancelled();
        assertThat(result.getFailReason()).isEqualTo("payment failed");
    }

    @Test
    public void failReasonForOtherError() {
        BillingResult result = TestBillingResults.error();
        assertThat(result.getFailReason()).isEqualTo("billing error: 6");
    }

    @Test
    public void extractsPayloadFromResponse() {
        BillingResult result = TestBillingResults.success();
        assertThat(result.getPayload().data).isEqualTo("payload");
        assertThat(result.getPayload().signature).isEqualTo("payload_signature");
    }

}