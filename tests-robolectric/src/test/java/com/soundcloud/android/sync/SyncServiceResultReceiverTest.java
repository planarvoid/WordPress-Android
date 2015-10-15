package com.soundcloud.android.sync;

import static com.soundcloud.android.Expect.expect;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.soundcloud.android.api.legacy.model.ContentStats;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.storage.provider.Content;
import com.soundcloud.android.stream.SoundStreamSyncOperations;
import com.xtremelabs.robolectric.Robolectric;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import android.content.Context;
import android.content.SyncResult;
import android.net.Uri;
import android.os.Bundle;

@RunWith(SoundCloudTestRunner.class)
public class SyncServiceResultReceiverTest {

    private static final String LIKES_URI_STRING = Content.ME_LIKES.uri.toString();
    private static final Uri LIKES_URI = Content.ME_LIKES.uri;

    SyncServiceResultReceiver syncServiceResultReceiver;

    private Context context = Robolectric.application;
    @Mock private SoundStreamSyncOperations soundStreamSyncOperations;
    @Mock private SyncStateManager syncStateManager;
    @Mock private SyncServiceResultReceiver.OnResultListener onResultListener;

    private SyncResult syncResult = new SyncResult();

    @Before
    public void setUp() throws Exception {
        syncServiceResultReceiver = new SyncServiceResultReceiver.Factory(context, soundStreamSyncOperations, syncStateManager)
                .create(syncResult, onResultListener);
    }

    @Test
    public void syncErrorCopiesIOExceptionsToSyncResult() throws Exception {
        final Bundle resultData = new Bundle();
        final SyncResult syncResultArg = new SyncResult();
        syncResultArg.stats.numIoExceptions = 12;

        resultData.putParcelable(ApiSyncService.EXTRA_SYNC_RESULT, syncResultArg);
        syncServiceResultReceiver.onReceiveResult(ApiSyncService.STATUS_SYNC_ERROR, resultData);

        expect(syncResult.stats.numIoExceptions).toEqual(12L);
    }

    @Test
    public void syncErrorCopiesDelayUntilToSyncResult() throws Exception {
        final Bundle resultData = new Bundle();
        final SyncResult syncResultArg = new SyncResult();
        syncResultArg.delayUntil = 1000;

        resultData.putParcelable(ApiSyncService.EXTRA_SYNC_RESULT, syncResultArg);
        syncServiceResultReceiver.onReceiveResult(ApiSyncService.STATUS_SYNC_ERROR, resultData);

        expect(syncResult.delayUntil).toEqual(1000L);
    }

    @Test
    public void syncErrorCopiesAuthExceptionsToSyncResult() throws Exception {
        final Bundle resultData = new Bundle();
        final SyncResult syncResultArg = new SyncResult();
        syncResultArg.stats.numAuthExceptions = 12;

        resultData.putParcelable(ApiSyncService.EXTRA_SYNC_RESULT, syncResultArg);
        syncServiceResultReceiver.onReceiveResult(ApiSyncService.STATUS_SYNC_ERROR, resultData);

        expect(syncResult.stats.numAuthExceptions).toEqual(12L);
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
        ContentStats.setLastSeen(Robolectric.application, Content.ME_SOUND_STREAM, 1000L);

        final Bundle resultData = new Bundle();
        resultData.putBoolean(LIKES_URI_STRING, true);
        syncServiceResultReceiver.onReceiveResult(ApiSyncService.STATUS_SYNC_FINISHED, resultData);

        verify(soundStreamSyncOperations).createNotificationForUnseenItems();

    }
}
