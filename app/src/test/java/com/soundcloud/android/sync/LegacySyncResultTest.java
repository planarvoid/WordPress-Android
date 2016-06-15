package com.soundcloud.android.sync;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.soundcloud.android.testsupport.AndroidUnitTest;
import org.junit.Test;
import org.mockito.Mock;

import android.net.Uri;

import java.util.Random;
import java.util.concurrent.TimeUnit;

public class LegacySyncResultTest extends AndroidUnitTest {

    private static final Uri URI = Uri.parse("some/uri");
    @Mock private Random random;

    @Test
    public void fromSuccessfulChangeIsSuccessful() throws Exception {
        final LegacySyncResult legacySyncResult = LegacySyncResult.fromSuccessfulChange(URI);
        assertThat(legacySyncResult.success).isTrue();
    }

    @Test
    public void fromSuccessfulChangeIsChanged() throws Exception {
        final LegacySyncResult legacySyncResult = LegacySyncResult.fromSuccessfulChange(URI);
        assertThat(legacySyncResult.change).isEqualTo(LegacySyncResult.CHANGED);
    }

    @Test
    public void fromSuccessfulChangeHasTimestamp() throws Exception {
        final LegacySyncResult legacySyncResult = LegacySyncResult.fromSuccessfulChange(URI);
        assertThat(legacySyncResult.synced_at).isGreaterThan(0L);
    }

    @Test
    public void fromSuccessWithoutChangeIsSuccessful() throws Exception {
        final LegacySyncResult legacySyncResult = LegacySyncResult.fromSuccessWithoutChange(URI);
        assertThat(legacySyncResult.success).isTrue();
    }

    @Test
    public void fromSuccessWithoutChangeIsUnchanged() throws Exception {
        final LegacySyncResult legacySyncResult = LegacySyncResult.fromSuccessWithoutChange(URI);
        assertThat(legacySyncResult.change).isEqualTo(LegacySyncResult.UNCHANGED);
    }

    @Test
    public void fromSuccessWithoutHasTimestamp() throws Exception {
        final LegacySyncResult legacySyncResult = LegacySyncResult.fromSuccessWithoutChange(URI);
        assertThat(legacySyncResult.synced_at).isGreaterThan(0L);
    }

    @Test
    public void fromAuthExceptionIsUnsuccessful() throws Exception {
        final LegacySyncResult legacySyncResult = LegacySyncResult.fromAuthException(URI);
        assertThat(legacySyncResult.success).isFalse();
    }

    @Test
    public void fromAuthExceptionIncreasesAuthExceptionCounter() throws Exception {
        final LegacySyncResult legacySyncResult = LegacySyncResult.fromAuthException(URI);
        assertThat(legacySyncResult.syncResult.stats.numAuthExceptions).isEqualTo(1L);
    }

    @Test
    public void fromIOExceptionIsUnsuccessful() throws Exception {
        final LegacySyncResult legacySyncResult = LegacySyncResult.fromIOException(URI);
        assertThat(legacySyncResult.success).isFalse();
    }

    @Test
    public void fromIOExceptionIncreasesAuthExceptionCounter() throws Exception {
        final LegacySyncResult legacySyncResult = LegacySyncResult.fromIOException(URI);
        assertThat(legacySyncResult.syncResult.stats.numIoExceptions).isEqualTo(1L);
    }

    @Test
    public void fromUnexpectedResponseCodesAddsOneSyncIntervalDelayFor5XX() throws Exception {
        final LegacySyncResult legacySyncResult = LegacySyncResult.fromUnexpectedResponse(URI, 500);

        assertThat(legacySyncResult.syncResult.delayUntil).isEqualTo(SyncConfig.DEFAULT_SYNC_DELAY);
    }

    @Test
    public void fromUnexpectedResponseCodesDoesNotAddDelayTimeFor4XX() throws Exception {
        final LegacySyncResult legacySyncResult = LegacySyncResult.fromUnexpectedResponse(URI, 404);

        assertThat(legacySyncResult.syncResult.delayUntil).isEqualTo(0L);
    }

    @Test
    public void fromGeneralFailureIsUnsuccessful() throws Exception {
        final LegacySyncResult legacySyncResult = LegacySyncResult.fromGeneralFailure(URI, random);
        assertThat(legacySyncResult.success).isFalse();
    }

    @Test
    public void fromGeneralFailureAddsMinDelayTime() throws Exception {
        final int inclusiveRange = LegacySyncResult.GENERAL_ERROR_DELAY_RANGE + 1;
        when(random.nextInt(inclusiveRange)).thenReturn(0);

        final LegacySyncResult legacySyncResult = LegacySyncResult.fromGeneralFailure(URI, random);

        assertThat(legacySyncResult.syncResult.delayUntil).isEqualTo(TimeUnit.MINUTES.toSeconds(LegacySyncResult.GENERAL_ERROR_MINIMUM_DELAY));
    }

    @Test
    public void fromGeneralFailureAddsMaxDelayTime() throws Exception {
        final int inclusiveRange = LegacySyncResult.GENERAL_ERROR_DELAY_RANGE + 1;
        when(random.nextInt(inclusiveRange)).thenReturn(LegacySyncResult.GENERAL_ERROR_DELAY_RANGE);

        final LegacySyncResult legacySyncResult = LegacySyncResult.fromGeneralFailure(URI, random);

        long expectedTimeInSeconds = LegacySyncResult.GENERAL_ERROR_MINIMUM_DELAY + LegacySyncResult.GENERAL_ERROR_DELAY_RANGE;
        assertThat(legacySyncResult.syncResult.delayUntil).isEqualTo(TimeUnit.MINUTES.toSeconds(expectedTimeInSeconds));
    }
}
