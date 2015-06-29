package com.soundcloud.android.sync;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import com.soundcloud.android.testsupport.PlatformUnitTest;
import org.junit.Test;
import org.mockito.Mock;

import android.net.Uri;

import java.util.Random;
import java.util.concurrent.TimeUnit;

public class ApiSyncResultTest extends PlatformUnitTest {

    private static final Uri URI = Uri.parse("some/uri");
    @Mock private Random random;

    @Test
    public void fromSuccessfulChangeIsSuccessful() throws Exception {
        final ApiSyncResult apiSyncResult = ApiSyncResult.fromSuccessfulChange(URI);
        assertThat(apiSyncResult.success).isTrue();
    }

    @Test
    public void fromSuccessfulChangeIsChanged() throws Exception {
        final ApiSyncResult apiSyncResult = ApiSyncResult.fromSuccessfulChange(URI);
        assertThat(apiSyncResult.change).isEqualTo(ApiSyncResult.CHANGED);
    }

    @Test
    public void fromSuccessfulChangeHasTimestamp() throws Exception {
        final ApiSyncResult apiSyncResult = ApiSyncResult.fromSuccessfulChange(URI);
        assertThat(apiSyncResult.synced_at).isGreaterThan(0L);
    }

    @Test
    public void fromSuccessWithoutChangeIsSuccessful() throws Exception {
        final ApiSyncResult apiSyncResult = ApiSyncResult.fromSuccessWithoutChange(URI);
        assertThat(apiSyncResult.success).isTrue();
    }

    @Test
    public void fromSuccessWithoutChangeIsUnchanged() throws Exception {
        final ApiSyncResult apiSyncResult = ApiSyncResult.fromSuccessWithoutChange(URI);
        assertThat(apiSyncResult.change).isEqualTo(ApiSyncResult.UNCHANGED);
    }

    @Test
    public void fromSuccessWithoutHasTimestamp() throws Exception {
        final ApiSyncResult apiSyncResult = ApiSyncResult.fromSuccessWithoutChange(URI);
        assertThat(apiSyncResult.synced_at).isGreaterThan(0L);
    }

    @Test
    public void fromAuthExceptionIsUnsuccessful() throws Exception {
        final ApiSyncResult apiSyncResult = ApiSyncResult.fromAuthException(URI);
        assertThat(apiSyncResult.success).isFalse();
    }

    @Test
    public void fromAuthExceptionIncreasesAuthExceptionCounter() throws Exception {
        final ApiSyncResult apiSyncResult = ApiSyncResult.fromAuthException(URI);
        assertThat(apiSyncResult.syncResult.stats.numAuthExceptions).isEqualTo(1L);
    }

    @Test
    public void fromIOExceptionIsUnsuccessful() throws Exception {
        final ApiSyncResult apiSyncResult = ApiSyncResult.fromIOException(URI);
        assertThat(apiSyncResult.success).isFalse();
    }

    @Test
    public void fromIOExceptionIncreasesAuthExceptionCounter() throws Exception {
        final ApiSyncResult apiSyncResult = ApiSyncResult.fromIOException(URI);
        assertThat(apiSyncResult.syncResult.stats.numIoExceptions).isEqualTo(1L);
    }

    @Test
    public void fromUnexpectedResponseCodesAddsMaxDelayTimeFor5XX() throws Exception {
        final int inclusiveRange = ApiSyncResult.UNEXPECTED_RESPONSE_DELAY_RANGE + 1;
        when(random.nextInt(inclusiveRange)).thenReturn(ApiSyncResult.UNEXPECTED_RESPONSE_DELAY_RANGE);

        final ApiSyncResult apiSyncResult = ApiSyncResult.fromUnexpectedResponse(URI, 500, random);

        final int expectedDurationsInMinutes = ApiSyncResult.UNEXPECTED_RESPONSE_MINIMUM_DELAY + ApiSyncResult.UNEXPECTED_RESPONSE_DELAY_RANGE;
        assertThat(apiSyncResult.syncResult.delayUntil).isEqualTo(TimeUnit.MINUTES.toSeconds(expectedDurationsInMinutes));
    }

    @Test
    public void fromUnexpectedResponseCodesAddsMinDelayTimeFor5XX() throws Exception {
        final int inclusiveRange = ApiSyncResult.UNEXPECTED_RESPONSE_DELAY_RANGE + 1;
        when(random.nextInt(inclusiveRange)).thenReturn(0);

        final ApiSyncResult apiSyncResult = ApiSyncResult.fromUnexpectedResponse(URI, 500, random);

        final int expectedDurationsInMinutes = ApiSyncResult.UNEXPECTED_RESPONSE_MINIMUM_DELAY;
        assertThat(apiSyncResult.syncResult.delayUntil).isEqualTo(TimeUnit.MINUTES.toSeconds(expectedDurationsInMinutes));
    }

    @Test
    public void fromUnexpectedResponseCodesDoesNotAddDelayTimeFor4XX() throws Exception {
        final ApiSyncResult apiSyncResult = ApiSyncResult.fromUnexpectedResponse(URI, 404, random);
        verifyZeroInteractions(random);
        assertThat(apiSyncResult.syncResult.delayUntil).isEqualTo(0L);
    }

    @Test
    public void fromGeneralFailureIsUnsuccessful() throws Exception {
        final ApiSyncResult apiSyncResult = ApiSyncResult.fromGeneralFailure(URI, random);
        assertThat(apiSyncResult.success).isFalse();
    }

    @Test
    public void fromGeneralFailureAddsMinDelayTime() throws Exception {
        final int inclusiveRange = ApiSyncResult.GENERAL_ERROR_DELAY_RANGE + 1;
        when(random.nextInt(inclusiveRange)).thenReturn(0);

        final ApiSyncResult apiSyncResult = ApiSyncResult.fromGeneralFailure(URI, random);

        assertThat(apiSyncResult.syncResult.delayUntil).isEqualTo(TimeUnit.MINUTES.toSeconds(ApiSyncResult.GENERAL_ERROR_MINIMUM_DELAY));
    }

    @Test
    public void fromGeneralFailureAddsMaxDelayTime() throws Exception {
        final int inclusiveRange = ApiSyncResult.GENERAL_ERROR_DELAY_RANGE + 1;
        when(random.nextInt(inclusiveRange)).thenReturn(ApiSyncResult.GENERAL_ERROR_DELAY_RANGE);

        final ApiSyncResult apiSyncResult = ApiSyncResult.fromGeneralFailure(URI, random);

        long expectedTimeInSeconds = ApiSyncResult.GENERAL_ERROR_MINIMUM_DELAY + ApiSyncResult.GENERAL_ERROR_DELAY_RANGE;
        assertThat(apiSyncResult.syncResult.delayUntil).isEqualTo(TimeUnit.MINUTES.toSeconds(expectedTimeInSeconds));
    }
}