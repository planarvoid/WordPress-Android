package com.soundcloud.android.sync;

import static org.assertj.core.api.Assertions.assertThat;
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

        verify(syncStateStorage).resetSyncMisses(Syncable.CHARTS);
    }

    @Test
    public void successfulSyncWithoutChangeIncrementsSyncMisses() {
        final Bundle resultData = createBundleForSuccess(false);

        receiver.onReceiveResult(0, resultData);

        verify(syncStateStorage).incrementSyncMisses(Syncable.CHARTS);
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
        resultData.putParcelable(Syncable.CHARTS.name(),
                                 SyncJobResult.failure(Syncable.CHARTS.name(),
                                                       ApiRequestException.serverError(null, null)));
        resultData.putParcelable(Syncable.LIKED_STATIONS.name(),
                                 SyncJobResult.failure(Syncable.LIKED_STATIONS.name(),
                                                       ApiRequestException.networkError(null, null)));
        resultData.putParcelable(Syncable.RECOMMENDED_STATIONS.name(),
                                 SyncJobResult.failure(Syncable.RECOMMENDED_STATIONS.name(),
                                                       ApiRequestException.authError(null, null)));
        resultData.putParcelable(Syncable.RECOMMENDED_TRACKS.name(),
                                 SyncJobResult.success(Syncable.RECOMMENDED_TRACKS.name(), true));

        receiver.onReceiveResult(0, resultData);

        verify(syncStateStorage).resetSyncMisses(Syncable.RECOMMENDED_TRACKS);
        assertThat(syncResult.stats.numIoExceptions).isEqualTo(1);
        assertThat(syncResult.stats.numAuthExceptions).isEqualTo(1);
        assertThat(syncResult.delayUntil).isGreaterThan(0);

    }

    private Bundle createBundleForSuccess(boolean wasChanged) {
        final Bundle resultData = new Bundle();
        resultData.putParcelable(Syncable.CHARTS.name(), SyncJobResult.success(Syncable.CHARTS.name(), wasChanged));
        return resultData;
    }

    private Bundle createBundleForFailure(ApiRequestException e) {
        final Bundle resultData = new Bundle();
        resultData.putParcelable(Syncable.CHARTS.name(), SyncJobResult.failure(Syncable.CHARTS.name(), e));
        return resultData;
    }
}
