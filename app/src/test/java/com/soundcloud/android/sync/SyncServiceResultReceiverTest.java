package com.soundcloud.android.sync;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.api.legacy.model.ContentStats;
import com.soundcloud.android.storage.provider.Content;
import com.soundcloud.android.sync.activities.ActivitiesNotifier;
import com.soundcloud.android.sync.stream.SoundStreamNotifier;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import android.content.SyncResult;
import android.net.Uri;
import android.os.Bundle;

public class SyncServiceResultReceiverTest extends AndroidUnitTest {

    private static final String LIKES_URI_STRING = Content.ME_LIKES.uri.toString();
    private static final Uri LIKES_URI = Content.ME_LIKES.uri;

    private SyncServiceResultReceiver syncServiceResultReceiver;

    @Mock private SoundStreamNotifier soundStreamNotifier;
    @Mock private ActivitiesNotifier activitiesNotifier;
    @Mock private SyncStateManager syncStateManager;
    @Mock private ContentStats contentStats;
    @Mock private SyncServiceResultReceiver.OnResultListener onResultListener;

    private SyncResult syncResult = new SyncResult();

    @Before
    public void setUp() throws Exception {
        syncServiceResultReceiver = new SyncServiceResultReceiver.Factory(context(), soundStreamNotifier,
                activitiesNotifier, syncStateManager, contentStats)
                .create(syncResult, onResultListener);
    }

    @Test
    public void syncErrorCopiesIOExceptionsToSyncResult() throws Exception {
        final Bundle resultData = new Bundle();
        final SyncResult syncResultArg = new SyncResult();
        syncResultArg.stats.numIoExceptions = 12;

        resultData.putParcelable(ApiSyncService.EXTRA_SYNC_RESULT, syncResultArg);
        syncServiceResultReceiver.onReceiveResult(ApiSyncService.STATUS_SYNC_ERROR, resultData);

        assertThat(syncResult.stats.numIoExceptions).isEqualTo(12L);
    }

    @Test
    public void syncErrorCopiesDelayUntilToSyncResult() throws Exception {
        final Bundle resultData = new Bundle();
        final SyncResult syncResultArg = new SyncResult();
        syncResultArg.delayUntil = 1000;

        resultData.putParcelable(ApiSyncService.EXTRA_SYNC_RESULT, syncResultArg);
        syncServiceResultReceiver.onReceiveResult(ApiSyncService.STATUS_SYNC_ERROR, resultData);

        assertThat(syncResult.delayUntil).isEqualTo(1000L);
    }

    @Test
    public void syncErrorCopiesAuthExceptionsToSyncResult() throws Exception {
        final Bundle resultData = new Bundle();
        final SyncResult syncResultArg = new SyncResult();
        syncResultArg.stats.numAuthExceptions = 12;

        resultData.putParcelable(ApiSyncService.EXTRA_SYNC_RESULT, syncResultArg);
        syncServiceResultReceiver.onReceiveResult(ApiSyncService.STATUS_SYNC_ERROR, resultData);

        assertThat(syncResult.stats.numAuthExceptions).isEqualTo(12L);
    }

    @Test
    public void syncSuccessIncrementsSyncMissesIfUriUnchanged() throws Exception {
        final Bundle resultData = new Bundle();
        resultData.putBoolean(LIKES_URI_STRING, false);

        syncServiceResultReceiver.onReceiveResult(ApiSyncService.STATUS_SYNC_FINISHED, resultData);
        verify(syncStateManager).incrementSyncMiss(eq(LIKES_URI));
    }

    @Test
    public void syncSuccessDoesNotIncrementsSyncMissesIfUriChanged() throws Exception {
        final Bundle resultData = new Bundle();
        resultData.putBoolean(LIKES_URI_STRING, true);

        syncServiceResultReceiver.onReceiveResult(ApiSyncService.STATUS_SYNC_FINISHED, resultData);
        verify(syncStateManager, never()).incrementSyncMiss(eq(LIKES_URI));
    }

    @Test
    public void syncSuccessOnStreamCreatesNotification() throws Exception {
        when(contentStats.getLastSeen(Content.ME_SOUND_STREAM)).thenReturn(1000L);

        final Bundle resultData = new Bundle();
        resultData.putBoolean(LIKES_URI_STRING, true);
        syncServiceResultReceiver.onReceiveResult(ApiSyncService.STATUS_SYNC_FINISHED, resultData);

        verify(soundStreamNotifier).notifyUnseenItems();
    }
}
