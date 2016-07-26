package com.soundcloud.android.sync;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.robolectric.shadows.ShadowApplication;
import rx.observers.TestSubscriber;
import rx.subjects.PublishSubject;

import android.content.Intent;
import android.os.Bundle;
import android.os.ResultReceiver;

import java.util.Arrays;
import java.util.List;

public class SyncInitiatorTest extends AndroidUnitTest {

    private SyncInitiator syncInitiator;
    private TestSubscriber<SyncJobResult> syncSubscriber = new TestSubscriber<>();

    @Mock private AccountOperations accountOperations;
    @Mock private LegacySyncInitiator legacySyncInitiator;

    @Before
    public void setUp() throws Exception {
        syncInitiator = new SyncInitiator(context(),
                                          accountOperations,
                                          legacySyncInitiator);
    }

    @Test
    public void syncCreatesObservableForSyncable() {
        syncInitiator.sync(Syncable.CHARTS).subscribe(syncSubscriber);

        Intent intent = ShadowApplication.getInstance().getNextStartedService();
        assertThat(intent).isNotNull();
        assertThat(SyncIntentHelper.getSyncable(intent)).isEqualTo(Syncable.CHARTS);
        assertThat(intent.getParcelableExtra(ApiSyncService.EXTRA_STATUS_RECEIVER)).isInstanceOf(ResultReceiverAdapter.class);

        syncSubscriber.assertNoValues();
        final SyncJobResult result = sendSyncChangedToReceiver(intent);
        syncSubscriber.assertReceivedOnNext(Arrays.asList(result));
        syncSubscriber.assertCompleted();
    }

    @Test
    public void syncCreatesObservableForSyncableWithAction() {
        syncInitiator.sync(Syncable.CHARTS, "action").subscribe(syncSubscriber);

        Intent intent = ShadowApplication.getInstance().getNextStartedService();
        assertThat(intent).isNotNull();
        assertThat(intent.getAction()).isEqualTo("action");
        assertThat(SyncIntentHelper.getSyncable(intent)).isEqualTo(Syncable.CHARTS);
        assertThat(intent.getParcelableExtra(ApiSyncService.EXTRA_STATUS_RECEIVER)).isInstanceOf(ResultReceiverAdapter.class);

        syncSubscriber.assertNoValues();
        final SyncJobResult result = sendSyncChangedToReceiver(intent);
        syncSubscriber.assertReceivedOnNext(Arrays.asList(result));
        syncSubscriber.assertCompleted();
    }

    @Test
    public void batchSyncTrackCreatesObservableForTrackSync() {
        final List<Urn> entities = Arrays.asList(Urn.forTrack(123));
        syncInitiator.batchSyncTracks(entities).subscribe(syncSubscriber);

        Intent intent = ShadowApplication.getInstance().getNextStartedService();
        assertThat(SyncIntentHelper.getSyncable(intent)).isEqualTo(Syncable.TRACKS);
        assertThat(SyncIntentHelper.getSyncEntities(intent)).isEqualTo(entities);

        syncSubscriber.assertNoValues();
        final SyncJobResult result = sendSyncChangedToReceiver(intent);
        syncSubscriber.assertReceivedOnNext(Arrays.asList(result));
        syncSubscriber.assertCompleted();
    }

    @Test
    public void batchSyncUsersCreatesObservableForUsersSync() {
        final List<Urn> entities = Arrays.asList(Urn.forUser(123));
        syncInitiator.batchSyncUsers(entities).subscribe(syncSubscriber);

        Intent intent = ShadowApplication.getInstance().getNextStartedService();
        assertThat(SyncIntentHelper.getSyncable(intent)).isEqualTo(Syncable.USERS);
        assertThat(SyncIntentHelper.getSyncEntities(intent)).isEqualTo(entities);

        syncSubscriber.assertNoValues();
        final SyncJobResult result = sendSyncChangedToReceiver(intent);
        syncSubscriber.assertReceivedOnNext(Arrays.asList(result));
        syncSubscriber.assertCompleted();
    }

    @Test
    public void batchSyncPlaylistsCreatesObservableForPlaylistsSync() {
        final List<Urn> entities = Arrays.asList(Urn.forPlaylist(123));
        syncInitiator.batchSyncPlaylists(entities).subscribe(syncSubscriber);

        Intent intent = ShadowApplication.getInstance().getNextStartedService();
        assertThat(SyncIntentHelper.getSyncable(intent)).isEqualTo(Syncable.PLAYLISTS);
        assertThat(SyncIntentHelper.getSyncEntities(intent)).isEqualTo(entities);

        syncSubscriber.assertNoValues();
        final SyncJobResult result = sendSyncChangedToReceiver(intent);
        syncSubscriber.assertReceivedOnNext(Arrays.asList(result));
        syncSubscriber.assertCompleted();
    }

    @Test
    public void syncPlaylistCreatesObservableForPlaylistSync() {
        syncInitiator.syncPlaylist(Urn.forPlaylist(123)).subscribe(syncSubscriber);

        Intent intent = ShadowApplication.getInstance().getNextStartedService();
        assertThat(SyncIntentHelper.getSyncable(intent)).isEqualTo(Syncable.PLAYLIST);
        assertThat(SyncIntentHelper.getSyncEntities(intent)).isEqualTo(Arrays.asList(Urn.forPlaylist(123)));

        syncSubscriber.assertNoValues();
        final SyncJobResult result = sendSyncChangedToReceiver(intent);
        syncSubscriber.assertReceivedOnNext(Arrays.asList(result));
        syncSubscriber.assertCompleted();
    }

    @Test
    public void syncPlaylistsSyncsLocalAndRemotePlaylist() {
        final PublishSubject<Boolean> refreshPlaylistsSubject = PublishSubject.create();
        when(legacySyncInitiator.refreshMyPlaylists()).thenReturn(refreshPlaylistsSubject);
        syncInitiator.syncPlaylists(Arrays.asList(Urn.forPlaylist(123), Urn.forPlaylist(-123))).subscribe(syncSubscriber);

        Intent intent = ShadowApplication.getInstance().getNextStartedService();
        assertThat(SyncIntentHelper.getSyncable(intent)).isEqualTo(Syncable.PLAYLIST);
        assertThat(SyncIntentHelper.getSyncEntities(intent)).isEqualTo(Arrays.asList(Urn.forPlaylist(123)));

        syncSubscriber.assertNoValues();
        final SyncJobResult result = sendSyncChangedToReceiver(intent);
        syncSubscriber.assertReceivedOnNext(Arrays.asList(result));

        refreshPlaylistsSubject.onNext(true);
        refreshPlaylistsSubject.onCompleted();

        syncSubscriber.assertReceivedOnNext(Arrays.asList(result, SyncJobResult.success(LegacySyncActions.SYNC_PLAYLIST, true)));
        syncSubscriber.assertCompleted();
    }



    private SyncJobResult sendSyncChangedToReceiver(Intent intent) {
        final ResultReceiver resultReceiver = intent.getParcelableExtra(ApiSyncService.EXTRA_STATUS_RECEIVER);
        final Bundle resultData = new Bundle();
        final SyncJobResult result = SyncJobResult.success("action", true);
        resultData.putParcelable(ResultReceiverAdapter.SYNC_RESULT, result);
        resultReceiver.send(ApiSyncService.STATUS_SYNC_FINISHED, resultData);
        return result;
    }
}
