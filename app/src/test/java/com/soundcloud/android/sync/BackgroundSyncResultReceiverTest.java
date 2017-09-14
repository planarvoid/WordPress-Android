package com.soundcloud.android.sync;

import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import com.soundcloud.android.api.ApiRequestException;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import android.content.SyncResult;
import android.os.Bundle;

import java.io.IOException;

public class BackgroundSyncResultReceiverTest extends AndroidUnitTest {
    private static final Syncable SYNCABLE = Syncable.DISCOVERY_CARDS;
    private static final Syncable SECOND_SYNCABLE = Syncable.LIKED_STATIONS;
    private static final Syncable THIRD_SYNCABLE = Syncable.ME;
    private static final Syncable FOURTH_SYNCABLE = Syncable.TRACKS;
    private final SyncResult syncResult = new SyncResult();
    @Mock private SyncStateStorage syncStateStorage;
    @Mock private Runnable syncCompleteRunnable;

    private BackgroundSyncResultReceiver receiver;

    @Before
    public void setUp() throws Exception {
        receiver = new BackgroundSyncResultReceiver(syncCompleteRunnable, syncResult, syncStateStorage);
    }

    @Test
    public void successfulSyncWithChangeResetsSyncMisses() {
        final Bundle resultData = createBundleForSuccess(true);

        receiver.onReceiveResult(0, resultData);

        verify(syncStateStorage).resetSyncMisses(SYNCABLE);
    }

    @Test
    public void successfulSyncWithoutChangeIncrementsSyncMisses() {
        final Bundle resultData = createBundleForSuccess(false);

        receiver.onReceiveResult(0, resultData);

        verify(syncStateStorage).incrementSyncMisses(SYNCABLE);
    }

    @Test
    public void failedSyncForNetWorkErrorShouldIncrementIoStats() {
        final Bundle resultData = createBundleForFailure(ApiRequestException.networkError(null, new IOException()));

        receiver.onReceiveResult(0, resultData);

        verifyNoMoreInteractions(syncStateStorage);
        assertThat(syncResult.stats.numIoExceptions).isEqualTo(1);
        assertThat(syncResult.stats.numAuthExceptions).isEqualTo(0);
        assertThat(syncResult.delayUntil).isEqualTo(0);
    }

    @Test
    public void failedSyncForAuthExceptionShouldIncrementAuthStats() {
        final Bundle resultData = createBundleForFailure(ApiRequestException.authError(null, null));

        receiver.onReceiveResult(0, resultData);

        verifyNoMoreInteractions(syncStateStorage);
        assertThat(syncResult.stats.numIoExceptions).isEqualTo(0);
        assertThat(syncResult.stats.numAuthExceptions).isEqualTo(1);
        assertThat(syncResult.delayUntil).isEqualTo(0);
    }

    @Test
    public void failedSyncForErrorExceptionShouldDelayNextSync() {
        final Bundle resultData = createBundleForFailure(ApiRequestException.serverError(null, null));

        receiver.onReceiveResult(0, resultData);

        verifyNoMoreInteractions(syncStateStorage);
        assertThat(syncResult.stats.numIoExceptions).isEqualTo(0);
        assertThat(syncResult.stats.numAuthExceptions).isEqualTo(0);
        assertThat(syncResult.delayUntil).isGreaterThan(0);
    }

    @Test
    public void handlesMixedSyncResults() {
        final Bundle resultData = new Bundle();
        resultData.putParcelable(SYNCABLE.name(),
                                 SyncJobResult.failure(SYNCABLE.name(),
                                                       ApiRequestException.serverError(null, null)));
        resultData.putParcelable(SECOND_SYNCABLE.name(),
                                 SyncJobResult.failure(SECOND_SYNCABLE.name(),
                                                       ApiRequestException.networkError(null, null)));
        resultData.putParcelable(THIRD_SYNCABLE.name(),
                                 SyncJobResult.failure(THIRD_SYNCABLE.name(),
                                                       ApiRequestException.authError(null, null)));
        resultData.putParcelable(FOURTH_SYNCABLE.name(),
                                 SyncJobResult.success(FOURTH_SYNCABLE.name(), true));

        receiver.onReceiveResult(0, resultData);

        verify(syncStateStorage).resetSyncMisses(FOURTH_SYNCABLE);
        assertThat(syncResult.stats.numIoExceptions).isEqualTo(1);
        assertThat(syncResult.stats.numAuthExceptions).isEqualTo(1);
        assertThat(syncResult.delayUntil).isGreaterThan(0);

    }

    private Bundle createBundleForSuccess(boolean wasChanged) {
        final Bundle resultData = new Bundle();
        resultData.putParcelable(SYNCABLE.name(), SyncJobResult.success(SYNCABLE.name(), wasChanged));
        return resultData;
    }

    private Bundle createBundleForFailure(ApiRequestException e) {
        final Bundle resultData = new Bundle();
        resultData.putParcelable(SYNCABLE.name(), SyncJobResult.failure(SYNCABLE.name(), e));
        return resultData;
    }
}
