package com.soundcloud.android.sync;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.api.model.ApiPlaylist;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playlists.PlaylistProperty;
import com.soundcloud.android.storage.provider.Content;
import com.soundcloud.android.storage.provider.ScContentProvider;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import com.soundcloud.android.testsupport.fixtures.TestPropertySets;
import com.soundcloud.android.tracks.TrackProperty;
import com.soundcloud.java.collections.PropertySet;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.robolectric.shadows.ShadowApplication;
import org.robolectric.shadows.ShadowContentResolver;
import rx.Subscriber;
import rx.observers.TestObserver;
import rx.observers.TestSubscriber;

import android.accounts.Account;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.ResultReceiver;

import java.util.Arrays;
import java.util.Collections;

public class SyncInitiatorTest extends AndroidUnitTest {

    private SyncInitiator initiator;
    private Subscriber<Boolean> legacySyncSubscriber = new TestSubscriber<>();
    private TestObserver<SyncResult> syncSubscriber = new TestObserver<>();

    @Mock private AccountOperations accountOperations;
    @Mock private ResultReceiver resultReceiver;
    @Mock private SyncStateManager syncStateManager;

    @Before
    public void setup() {
        initiator = new SyncInitiator(context(), accountOperations, syncStateManager);
    }

    @Test
    public void shouldCreateSyncIntentForPushingFollowingsForValidAccount() throws Exception {
        Account account = new Account("soundcloud", "account");
        when(accountOperations.getSoundCloudAccount()).thenReturn(account);
        assertThat(initiator.pushFollowingsToApi()).isTrue();

        ShadowContentResolver.Status syncStatus = ShadowContentResolver.getStatus(account, ScContentProvider.AUTHORITY);
        assertThat(syncStatus.syncRequests).isEqualTo(1);
        assertThat(syncStatus.syncExtras.getBoolean(SyncAdapterService.EXTRA_SYNC_PUSH)).isTrue();
        assertThat(syncStatus.syncExtras.getString(SyncAdapterService.EXTRA_SYNC_PUSH_URI)).isEqualTo(Content.ME_FOLLOWINGS.uri.toString());
    }

    @Test
    public void shouldReturnFalseWhenPushingFollowingsWithInvalidAccount() throws Exception {
        assertThat(initiator.pushFollowingsToApi()).isFalse();
    }

    @Test
    public void shouldCreateIntentForRefreshingTheSoundStream() {
        initiator.refreshSoundStream().subscribe(legacySyncSubscriber);

        Intent intent = ShadowApplication.getInstance().getNextStartedService();
        assertThat(intent).isNotNull();
        assertThat(intent.getData()).isEqualTo(Content.ME_SOUND_STREAM.uri);
        assertThat(intent.getAction()).isEqualTo(ApiSyncService.ACTION_HARD_REFRESH);
        assertThat(intent.getBooleanExtra(ApiSyncService.EXTRA_IS_UI_REQUEST, false)).isTrue();
        assertThat(intent.getParcelableExtra(ApiSyncService.EXTRA_STATUS_RECEIVER)).isInstanceOf(ResultReceiver.class);
    }

    @Test
    public void shouldCreateIntentForInitialSoundStream() {
        initiator.initialSoundStream().subscribe(legacySyncSubscriber);

        Intent intent = ShadowApplication.getInstance().getNextStartedService();
        assertThat(intent).isNotNull();
        assertThat(intent.getData()).isSameAs(Content.ME_SOUND_STREAM.uri);
        assertThat(intent.getBooleanExtra(ApiSyncService.EXTRA_IS_UI_REQUEST, false)).isTrue();
        assertThat(intent.getParcelableExtra(ApiSyncService.EXTRA_STATUS_RECEIVER)).isInstanceOf(ResultReceiver.class);
    }

    @Test
    public void shouldResetSoundStreamSyncMissesOnChangedSync() {
        initiator.refreshSoundStream().subscribe(legacySyncSubscriber);
        final Uri uri = Content.ME_SOUND_STREAM.uri;
        sendSyncChangedLegacyToUri(uri);
        verify(syncStateManager).resetSyncMisses(uri);
    }

    @Test
    public void shouldCreateIntentForSyncingOlderSoundStreamItems() {
        initiator.backfillSoundStream().subscribe(legacySyncSubscriber);

        Intent intent = ShadowApplication.getInstance().getNextStartedService();
        assertThat(intent).isNotNull();
        assertThat(intent.getData()).isSameAs(Content.ME_SOUND_STREAM.uri);
        assertThat(intent.getAction()).isEqualTo(ApiSyncService.ACTION_APPEND);
        assertThat(intent.getBooleanExtra(ApiSyncService.EXTRA_IS_UI_REQUEST, false)).isTrue();
        assertThat(intent.getParcelableExtra(ApiSyncService.EXTRA_STATUS_RECEIVER)).isInstanceOf(ResultReceiver.class);
    }

    @Test
    public void shouldCreateIntentForSyncingLocalPlaylists() throws Exception {
        initiator.syncLocalPlaylists();

        Intent intent = ShadowApplication.getInstance().getNextStartedService();
        assertThat(intent).isNotNull();
        assertThat(intent.getData()).isSameAs(Content.ME_PLAYLISTS.uri);
        assertThat(intent.getBooleanExtra(ApiSyncService.EXTRA_IS_UI_REQUEST, false)).isTrue();
        assertThat(intent.getParcelableExtra(ApiSyncService.EXTRA_STATUS_RECEIVER)).isNull();
    }

    @Test
    public void shouldResetMyPlaylistSyncMissesOnChangedSync() {
        initiator.refreshMyPlaylists().subscribe(legacySyncSubscriber);
        final Uri uri = Content.ME_PLAYLISTS.uri;
        sendSyncChangedLegacyToUri(uri);
        verify(syncStateManager).resetSyncMisses(uri);
    }

    @Test
    public void shouldCreateIntentForSyncingSinglePlaylist() throws Exception {
        final Urn playlistUrn = Urn.forPlaylist(1L);
        initiator.syncPlaylist(playlistUrn).subscribe(syncSubscriber);

        Intent intent = ShadowApplication.getInstance().getNextStartedService();
        assertThat(intent).isNotNull();
        assertThat(intent.getAction()).isEqualTo(SyncActions.SYNC_PLAYLIST);
        assertThat(intent.getParcelableExtra(SyncExtras.URN)).isEqualTo(playlistUrn);
        assertThat(intent.getParcelableExtra(ApiSyncService.EXTRA_STATUS_RECEIVER)).isInstanceOf(ResultReceiver.class);
    }

    @Test
    public void syncPlaylistSyncsMyPlaylistsIfPlaylistIsLocal() throws Exception {
        final Urn playlistUrn = Urn.forPlaylist(-1L);
        initiator.syncPlaylist(playlistUrn).subscribe(syncSubscriber);

        Intent intent = ShadowApplication.getInstance().getNextStartedService();
        assertThat(intent).isNotNull();
        assertThat(intent.getData()).isSameAs(Content.ME_PLAYLISTS.uri);
        assertThat(intent.getBooleanExtra(ApiSyncService.EXTRA_IS_UI_REQUEST, false)).isTrue();

        final LegacyResultReceiverAdapter resultReceiver = intent.getParcelableExtra(ApiSyncService.EXTRA_STATUS_RECEIVER);
        final Bundle resultData = new Bundle();
        resultData.putBoolean(Content.ME_PLAYLISTS.uri.toString(), true);
        resultReceiver.onReceiveResult(ApiSyncService.STATUS_SYNC_FINISHED, resultData);

        assertThat(syncSubscriber.getOnNextEvents()).containsExactly(SyncResult.success(SyncActions.SYNC_PLAYLIST, true));
    }

    @Test
    public void shouldCreateIntentForSyncingSingleTrack() throws Exception {
        initiator.syncTrack(Urn.forTrack(1L)).subscribe(legacySyncSubscriber);

        Intent intent = ShadowApplication.getInstance().getNextStartedService();
        assertThat(intent).isNotNull();
        assertThat(intent.getData()).isEqualTo(Content.TRACKS.forQuery(String.valueOf(1L)));
        assertThat(intent.getBooleanExtra(ApiSyncService.EXTRA_IS_UI_REQUEST, false)).isTrue();
        assertThat(intent.getParcelableExtra(ApiSyncService.EXTRA_STATUS_RECEIVER)).isInstanceOf(ResultReceiver.class);
    }

    @Test
    public void syncTrackLikesShouldRequestTrackLikesSync() throws Exception {
        initiator.syncTrackLikes().subscribe(syncSubscriber);

        Intent intent = ShadowApplication.getInstance().getNextStartedService();
        assertThat(intent).isNotNull();
        assertThat(intent.getAction()).isEqualTo(SyncActions.SYNC_TRACK_LIKES);
        assertThat(intent.getParcelableExtra(ApiSyncService.EXTRA_STATUS_RECEIVER)).isInstanceOf(ResultReceiverAdapter.class);
    }

    @Test
    public void syncRecommendationsShouldRequestRecommendationsSync() throws Exception {
        initiator.syncRecommendations().subscribe(syncSubscriber);

        Intent intent = ShadowApplication.getInstance().getNextStartedService();
        assertThat(intent).isNotNull();
        assertThat(intent.getAction()).isEqualTo(SyncActions.SYNC_RECOMMENDATIONS);
        assertThat(intent.getParcelableExtra(ApiSyncService.EXTRA_STATUS_RECEIVER)).isInstanceOf(ResultReceiverAdapter.class);
    }

    @Test
    public void shouldResetMyLikesSyncMissesOnChangedTrackLikesSync() {
        initiator.syncTrackLikes().subscribe(syncSubscriber);
        final Uri uri = Content.ME_LIKES.uri;
        sendSyncChangedToUri(uri);
        verify(syncStateManager).resetSyncMisses(uri);
    }

    @Test
    public void syncPlaylistLikesShouldRequestPlaylistLikesSync() throws Exception {
        initiator.syncPlaylistLikes().subscribe(syncSubscriber);

        Intent intent = ShadowApplication.getInstance().getNextStartedService();
        assertThat(intent).isNotNull();
        assertThat(intent.getAction()).isEqualTo(SyncActions.SYNC_PLAYLIST_LIKES);
        assertThat(intent.getParcelableExtra(ApiSyncService.EXTRA_STATUS_RECEIVER)).isInstanceOf(ResultReceiverAdapter.class);
    }

    @Test
    public void shouldResetMyLikesSyncMissesOnChangedPlaylistLikesSync() {
        initiator.syncPlaylistLikes().subscribe(syncSubscriber);
        final Uri uri = Content.ME_LIKES.uri;
        sendSyncChangedToUri(uri);
        verify(syncStateManager).resetSyncMisses(uri);
    }

    @Test
    public void requestTracksSyncShouldRequestTracksSync() throws Exception {
        final PropertySet propertySet = TestPropertySets.fromApiTrack();
        initiator.requestTracksSync(Collections.singletonList(propertySet));

        Intent intent = ShadowApplication.getInstance().getNextStartedService();
        assertThat(intent).isNotNull();
        assertThat(intent.getAction()).isEqualTo(SyncActions.SYNC_TRACKS);
        assertThat(intent.getParcelableArrayListExtra(SyncExtras.URNS)).containsExactly(propertySet.get(TrackProperty.URN));
    }

    @Test
    public void requestPlaylistsSyncShouldRequestPlaylistsSync() throws Exception {
        final PropertySet propertySet = ModelFixtures.create(ApiPlaylist.class).toPropertySet();
        initiator.requestPlaylistSync(Collections.singletonList(propertySet));

        Intent intent = ShadowApplication.getInstance().getNextStartedService();
        assertThat(intent).isNotNull();
        assertThat(intent.getAction()).isEqualTo(SyncActions.SYNC_PLAYLISTS);
        assertThat(intent.getParcelableArrayListExtra(SyncExtras.URNS)).containsExactly(propertySet.get(PlaylistProperty.URN));
    }


    @Test
    public void refreshCollectionsSendsCollectionSyncIntent() {
        initiator.refreshCollections().subscribe(legacySyncSubscriber);

        Intent intent = ShadowApplication.getInstance().getNextStartedService();
        assertThat(intent).isNotNull();
        assertThat(intent.getAction()).isEqualTo(ApiSyncService.ACTION_HARD_REFRESH);
        assertThat(intent.getParcelableArrayListExtra(ApiSyncService.EXTRA_SYNC_URIS)).isEqualTo(Arrays.asList(SyncContent.MyLikes.content.uri,
                SyncContent.MyPlaylists.content.uri));
    }

    @Test
    public void refreshCollectionsResetsSyncMissesOnAllCollections() {
        initiator.refreshCollections().subscribe(legacySyncSubscriber);
        final Uri[] uris = new Uri[] {Content.ME_PLAYLISTS.uri, Content.ME_LIKES.uri};

        sendSyncChangedLegacyToUri(uris);

        for (Uri uri : uris) {
            verify(syncStateManager).resetSyncMisses(uri);
        }
    }


    private void sendSyncChangedLegacyToUri(Uri... uris) {
        Intent intent = ShadowApplication.getInstance().getNextStartedService();
        final ResultReceiver resultReceiver = intent.getParcelableExtra(ApiSyncService.EXTRA_STATUS_RECEIVER);
        final Bundle resultData = new Bundle();
        for (Uri uri : uris) {
            resultData.putBoolean(uri.toString(), true);
        }
        resultReceiver.send(ApiSyncService.STATUS_SYNC_FINISHED, resultData);
    }

    private void sendSyncChangedToUri(Uri uri) {
        Intent intent = ShadowApplication.getInstance().getNextStartedService();
        final ResultReceiver resultReceiver = intent.getParcelableExtra(ApiSyncService.EXTRA_STATUS_RECEIVER);
        final Bundle resultData = new Bundle();
        resultData.putParcelable(ResultReceiverAdapter.SYNC_RESULT, SyncResult.success("action", true));
        resultReceiver.send(ApiSyncService.STATUS_SYNC_FINISHED, resultData);
    }
}
