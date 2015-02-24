package com.soundcloud.android.sync;

import static com.soundcloud.android.Expect.expect;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import android.net.Uri;

import java.util.Random;
import java.util.concurrent.TimeUnit;

@RunWith(SoundCloudTestRunner.class)
public class ApiSyncResultTest {

    private static final Uri URI = Uri.parse("some/uri");
    @Mock private Random random;

    @Test
    public void fromSuccessfulChangeIsSuccessful() throws Exception {
        final ApiSyncResult apiSyncResult = ApiSyncResult.fromSuccessfulChange(URI);
        expect(apiSyncResult.success).toBeTrue();
    }

    @Test
    public void fromSuccessfulChangeIsChanged() throws Exception {
        final ApiSyncResult apiSyncResult = ApiSyncResult.fromSuccessfulChange(URI);
        expect(apiSyncResult.change).toEqual(ApiSyncResult.CHANGED);
    }

    @Test
    public void fromSuccessfulChangeHasTimestamp() throws Exception {
        final ApiSyncResult apiSyncResult = ApiSyncResult.fromSuccessfulChange(URI);
        expect(apiSyncResult.synced_at).toBeGreaterThan(0L);
    }

    @Test
    public void fromSuccessWithoutChangeIsSuccessful() throws Exception {
        final ApiSyncResult apiSyncResult = ApiSyncResult.fromSuccessWithoutChange(URI);
        expect(apiSyncResult.success).toBeTrue();
    }

    @Test
    public void fromSuccessWithoutChangeIsUnchanged() throws Exception {
        final ApiSyncResult apiSyncResult = ApiSyncResult.fromSuccessWithoutChange(URI);
        expect(apiSyncResult.change).toEqual(ApiSyncResult.UNCHANGED);
    }

    @Test
    public void fromSuccessWithoutHasTimestamp() throws Exception {
        final ApiSyncResult apiSyncResult = ApiSyncResult.fromSuccessWithoutChange(URI);
        expect(apiSyncResult.synced_at).toBeGreaterThan(0L);
    }

    @Test
    public void fromAuthExceptionIsUnsuccessful() throws Exception {
        final ApiSyncResult apiSyncResult = ApiSyncResult.fromAuthException(URI);
        expect(apiSyncResult.success).toBeFalse();
    }

    @Test
    public void fromAuthExceptionIncreasesAuthExceptionCounter() throws Exception {
        final ApiSyncResult apiSyncResult = ApiSyncResult.fromAuthException(URI);
        expect(apiSyncResult.syncResult.stats.numAuthExceptions).toEqual(1L);
    }

    @Test
    public void fromIOExceptionIsUnsuccessful() throws Exception {
        final ApiSyncResult apiSyncResult = ApiSyncResult.fromIOException(URI);
        expect(apiSyncResult.success).toBeFalse();
    }

    @Test
    public void fromIOExceptionIncreasesAuthExceptionCounter() throws Exception {
        final ApiSyncResult apiSyncResult = ApiSyncResult.fromIOException(URI);
        expect(apiSyncResult.syncResult.stats.numIoExceptions).toEqual(1L);
    }

    @Test
    public void fromUnexpectedResponseCodesAddsMaxDelayTimeFor5XX() throws Exception {
        final int inclusiveRange = ApiSyncResult.UNEXPECTED_RESPONSE_DELAY_RANGE + 1;
        when(random.nextInt(inclusiveRange)).thenReturn(ApiSyncResult.UNEXPECTED_RESPONSE_DELAY_RANGE);

        final ApiSyncResult apiSyncResult = ApiSyncResult.fromUnexpectedResponse(URI, 500, random);

        final int expectedDurationsInMinutes = ApiSyncResult.UNEXPECTED_RESPONSE_MINIMUM_DELAY + ApiSyncResult.UNEXPECTED_RESPONSE_DELAY_RANGE;
        expect(apiSyncResult.syncResult.delayUntil).toEqual(TimeUnit.MINUTES.toSeconds(expectedDurationsInMinutes));
    }

    @Test
    public void fromUnexpectedResponseCodesAddsMinDelayTimeFor5XX() throws Exception {
        final int inclusiveRange = ApiSyncResult.UNEXPECTED_RESPONSE_DELAY_RANGE + 1;
        when(random.nextInt(inclusiveRange)).thenReturn(0);

        final ApiSyncResult apiSyncResult = ApiSyncResult.fromUnexpectedResponse(URI, 500, random);

        final int expectedDurationsInMinutes = ApiSyncResult.UNEXPECTED_RESPONSE_MINIMUM_DELAY;
        expect(apiSyncResult.syncResult.delayUntil).toEqual(TimeUnit.MINUTES.toSeconds(expectedDurationsInMinutes));
    }

    @Test
    public void fromUnexpectedResponseCodesDoesNotAddDelayTimeFor4XX() throws Exception {
        final ApiSyncResult apiSyncResult = ApiSyncResult.fromUnexpectedResponse(URI, 404, random);
        verifyZeroInteractions(random);
        expect(apiSyncResult.syncResult.delayUntil).toEqual(0L);
    }

    @Test
    public void fromGeneralFailureIsUnsuccessful() throws Exception {
        final ApiSyncResult apiSyncResult = ApiSyncResult.fromGeneralFailure(URI);
        expect(apiSyncResult.success).toBeFalse();
    }

    @Test
    public void fromGeneralFailureAddsDelayTime() throws Exception {
        final ApiSyncResult apiSyncResult = ApiSyncResult.fromGeneralFailure(URI);
        expect(apiSyncResult.syncResult.delayUntil).toBeGreaterThan((long) (ApiSyncResult.GENERAL_ERROR_MINIMUM_DELAY * 60));
        expect(apiSyncResult.syncResult.delayUntil).toBeLessThan((long) ((ApiSyncResult.GENERAL_ERROR_MINIMUM_DELAY + ApiSyncResult.GENERAL_ERROR_DELAY_RANGE) * 60));
    }
}