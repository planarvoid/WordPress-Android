package com.soundcloud.android.sync;

import static org.assertj.core.api.Java6Assertions.assertThat;

import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import io.reactivex.Scheduler;
import io.reactivex.observers.TestObserver;
import io.reactivex.schedulers.Schedulers;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import android.content.Intent;
import android.os.Bundle;
import android.os.Parcelable;
import android.os.ResultReceiver;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class SyncInitiatorTest extends AndroidUnitTest {

    private final Scheduler scheduler = Schedulers.trampoline();
    private SyncInitiator syncInitiator;
    private TestObserver<SyncJobResult> syncObserver = new TestObserver<>();

    @Mock private AccountOperations accountOperations;

    @Before
    public void setUp() throws Exception {
        syncInitiator = new SyncInitiator(context(), accountOperations, scheduler);
    }

    @Test
    public void syncCreatesObservableForSyncable() {
        final TestObserver<SyncJobResult> syncSubscriber = syncInitiator.sync(Syncable.CHARTS).test();

        Intent intent = getNextStartedService();
        assertThat(intent).isNotNull();
        assertThat(SyncIntentHelper.getSyncable(intent)).isEqualTo(Syncable.CHARTS);
        assertThat(intent.<Parcelable>getParcelableExtra(ApiSyncService.EXTRA_STATUS_RECEIVER)).isInstanceOf(ResultReceiverAdapter.class);

        syncSubscriber.assertNoValues();
        final SyncJobResult result = sendSyncChangedToReceiver(intent);
        syncSubscriber.awaitCount(1).assertResult(result);
    }

    @Test
    public void syncCreatesObservableForSyncableWithAction() {
        final TestObserver<SyncJobResult> syncSubscriber = syncInitiator.sync(Syncable.CHARTS, "action").test();

        Intent intent = getNextStartedService();
        assertThat(intent).isNotNull();
        assertThat(intent.getAction()).isEqualTo("action");
        assertThat(SyncIntentHelper.getSyncable(intent)).isEqualTo(Syncable.CHARTS);
        assertThat(intent.<Parcelable>getParcelableExtra(ApiSyncService.EXTRA_STATUS_RECEIVER)).isInstanceOf(ResultReceiverAdapter.class);

        syncSubscriber.assertNoValues();
        final SyncJobResult result = sendSyncChangedToReceiver(intent);
        syncSubscriber.assertResult(result);
    }

    @Test
    public void synchroniseCreatesObservableForSyncable() {
        syncInitiator.sync(Syncable.CHARTS).subscribe(syncObserver);

        Intent intent = getNextStartedService();
        assertThat(intent).isNotNull();
        assertThat(SyncIntentHelper.getSyncable(intent)).isEqualTo(Syncable.CHARTS);
        assertThat(intent.<Parcelable>getParcelableExtra(ApiSyncService.EXTRA_STATUS_RECEIVER)).isInstanceOf(ResultReceiverAdapter.class);

        syncObserver.assertNoValues();
        final SyncJobResult result = sendSyncChangedToReceiver(intent);
        syncObserver.assertValues(result);
        syncObserver.assertComplete();
    }

    @Test
    public void batchSyncTrackCreatesObservableForTrackSync() {
        final List<Urn> entities = Collections.singletonList(Urn.forTrack(123));
        final TestObserver<SyncJobResult> syncSubscriber = syncInitiator.batchSyncTracks(entities).test();

        Intent intent = getNextStartedService();
        assertThat(SyncIntentHelper.getSyncable(intent)).isEqualTo(Syncable.TRACKS);
        assertThat(SyncIntentHelper.getSyncEntities(intent)).isEqualTo(entities);

        syncSubscriber.assertNoValues();
        final SyncJobResult result = sendSyncChangedToReceiver(intent);
        syncSubscriber.assertResult(result);
    }

    @Test
    public void batchSyncUsersCreatesObservableForUsersSync() {
        final List<Urn> entities = Collections.singletonList(Urn.forUser(123));
        final TestObserver<SyncJobResult> syncSubscriber = syncInitiator.batchSyncUsers(entities).test();

        Intent intent = getNextStartedService();
        assertThat(SyncIntentHelper.getSyncable(intent)).isEqualTo(Syncable.USERS);
        assertThat(SyncIntentHelper.getSyncEntities(intent)).isEqualTo(entities);

        syncSubscriber.assertNoValues();
        final SyncJobResult result = sendSyncChangedToReceiver(intent);
        syncSubscriber.assertResult(result);
    }

    @Test
    public void batchSyncPlaylistsCreatesObservableForPlaylistsSync() {
        final List<Urn> entities = Collections.singletonList(Urn.forPlaylist(123));
        final TestObserver<SyncJobResult> syncSubscriber = syncInitiator.batchSyncPlaylists(entities).test();

        Intent intent = getNextStartedService();
        assertThat(SyncIntentHelper.getSyncable(intent)).isEqualTo(Syncable.PLAYLISTS);
        assertThat(SyncIntentHelper.getSyncEntities(intent)).isEqualTo(entities);

        syncSubscriber.assertNoValues();
        final SyncJobResult result = sendSyncChangedToReceiver(intent);
        syncSubscriber.assertResult(result);
    }

    @Test
    public void syncPlaylistCreatesObservableForPlaylistSync() {
        final TestObserver<SyncJobResult> syncSubscriber = syncInitiator.syncPlaylist(Urn.forPlaylist(123)).test();

        Intent intent = getNextStartedService();
        assertThat(SyncIntentHelper.getSyncable(intent)).isEqualTo(Syncable.PLAYLIST);
        assertThat(SyncIntentHelper.getSyncEntities(intent)).isEqualTo(Collections.singletonList(Urn.forPlaylist(123)));

        syncSubscriber.assertNoValues();
        final SyncJobResult result = sendSyncChangedToReceiver(intent);
        syncSubscriber.assertResult(result);
    }

    @Test
    public void syncPlaylistsSyncsLocalAndRemotePlaylist() {
        final TestObserver<SyncJobResult> syncSubscriber = syncInitiator.syncPlaylists(Arrays.asList(Urn.forPlaylist(123), Urn.forPlaylist(-123))).test();

        Intent intent = getNextStartedService();
        assertThat(SyncIntentHelper.getSyncable(intent)).isEqualTo(Syncable.PLAYLIST);
        assertThat(SyncIntentHelper.getSyncEntities(intent)).isEqualTo(Collections.singletonList(Urn.forPlaylist(123)));

        syncSubscriber.assertNoValues();
        final SyncJobResult result = sendSyncChangedToReceiver(intent);
        syncSubscriber.assertValue(result);

        Intent syncMyPlaylists = getNextStartedService();
        assertThat(SyncIntentHelper.getSyncable(syncMyPlaylists)).isEqualTo(Syncable.MY_PLAYLISTS);
        final SyncJobResult result2 = sendSyncChangedToReceiver(syncMyPlaylists);

        syncSubscriber.assertValues(result, result2);
        syncSubscriber.assertComplete();
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
