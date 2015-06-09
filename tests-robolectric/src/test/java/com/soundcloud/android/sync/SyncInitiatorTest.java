package com.soundcloud.android.sync;

import static com.soundcloud.android.Expect.expect;
import static com.xtremelabs.robolectric.shadows.ShadowContentResolver.Status;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.collect.Lists;
import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.api.model.ApiPlaylist;
import com.soundcloud.android.model.ParcelableUrn;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playlists.PlaylistProperty;
import com.soundcloud.android.properties.FeatureFlags;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.storage.provider.Content;
import com.soundcloud.android.storage.provider.ScContentProvider;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import com.soundcloud.android.testsupport.fixtures.TestPropertySets;
import com.soundcloud.android.tracks.TrackProperty;
import com.soundcloud.propeller.PropertySet;
import com.xtremelabs.robolectric.Robolectric;
import com.xtremelabs.robolectric.shadows.ShadowContentResolver;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import rx.Subscriber;
import rx.observers.TestSubscriber;

import android.accounts.Account;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.ResultReceiver;

@RunWith(SoundCloudTestRunner.class)
public class SyncInitiatorTest {

    private SyncInitiator initiator;
    private Subscriber<Boolean> legacySyncSubscriber = new TestSubscriber<Boolean>();
    private Subscriber<SyncResult> syncSubscriber = new TestSubscriber<SyncResult>();

    @Mock private AccountOperations accountOperations;
    @Mock private ResultReceiver resultReceiver;
    @Mock private FeatureFlags featureFlags;
    @Mock private SyncStateManager syncStateManager;

    @Before
    public void setup() {
        initiator = new SyncInitiator(Robolectric.application, accountOperations, syncStateManager);
    }

    @Test
    public void shouldCreateSyncIntentForPushingFollowingsForValidAccount() throws Exception {
        Account account = new Account("soundcloud", "account");
        when(accountOperations.getSoundCloudAccount()).thenReturn(account);
        expect(initiator.pushFollowingsToApi()).toBeTrue();

        Status syncStatus = ShadowContentResolver.getStatus(account, ScContentProvider.AUTHORITY);
        expect(syncStatus.syncRequests).toBe(1);
        expect(syncStatus.syncExtras.getBoolean(SyncAdapterService.EXTRA_SYNC_PUSH)).toBeTrue();
        expect(syncStatus.syncExtras.getString(SyncAdapterService.EXTRA_SYNC_PUSH_URI)).toEqual(Content.ME_FOLLOWINGS.uri.toString());
    }

    @Test
    public void shouldReturnFalseWhenPushingFollowingsWithInvalidAccount() throws Exception {
        expect(initiator.pushFollowingsToApi()).toBeFalse();
    }

    @Test
    public void shouldCreateIntentForRefreshingTheSoundStream() {
        initiator.refreshSoundStream().subscribe(legacySyncSubscriber);

        Intent intent = Robolectric.getShadowApplication().getNextStartedService();
        expect(intent).not.toBeNull();
        expect(intent.getData()).toBe(Content.ME_SOUND_STREAM.uri);
        expect(intent.getAction()).toBe(ApiSyncService.ACTION_HARD_REFRESH);
        expect(intent.getBooleanExtra(ApiSyncService.EXTRA_IS_UI_REQUEST, false)).toBeTrue();
        expect(intent.getParcelableExtra(ApiSyncService.EXTRA_STATUS_RECEIVER)).toBeInstanceOf(ResultReceiver.class);
    }

    @Test
    public void shouldCreateIntentForInitialSoundStream() {
        initiator.initialSoundStream().subscribe(legacySyncSubscriber);

        Intent intent = Robolectric.getShadowApplication().getNextStartedService();
        expect(intent).not.toBeNull();
        expect(intent.getData()).toBe(Content.ME_SOUND_STREAM.uri);
        expect(intent.getBooleanExtra(ApiSyncService.EXTRA_IS_UI_REQUEST, false)).toBeTrue();
        expect(intent.getParcelableExtra(ApiSyncService.EXTRA_STATUS_RECEIVER)).toBeInstanceOf(ResultReceiver.class);
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

        Intent intent = Robolectric.getShadowApplication().getNextStartedService();
        expect(intent).not.toBeNull();
        expect(intent.getData()).toBe(Content.ME_SOUND_STREAM.uri);
        expect(intent.getAction()).toEqual(ApiSyncService.ACTION_APPEND);
        expect(intent.getBooleanExtra(ApiSyncService.EXTRA_IS_UI_REQUEST, false)).toBeTrue();
        expect(intent.getParcelableExtra(ApiSyncService.EXTRA_STATUS_RECEIVER)).toBeInstanceOf(ResultReceiver.class);
    }

    @Test
    public void shouldCreateIntentForSyncingLocalPlaylists() throws Exception {
        initiator.syncLocalPlaylists();

        Intent intent = Robolectric.getShadowApplication().getNextStartedService();
        expect(intent).not.toBeNull();
        expect(intent.getData()).toBe(Content.ME_PLAYLISTS.uri);
        expect(intent.getBooleanExtra(ApiSyncService.EXTRA_IS_UI_REQUEST, false)).toBeTrue();
        expect(intent.getParcelableExtra(ApiSyncService.EXTRA_STATUS_RECEIVER)).toBeNull();
    }

    @Test
    public void shouldResetMyPlaylistSyncMissesOnChangedSync() {
        initiator.refreshPostedPlaylists().subscribe(legacySyncSubscriber);
        final Uri uri = Content.ME_PLAYLISTS.uri;
        sendSyncChangedLegacyToUri(uri);
        verify(syncStateManager).resetSyncMisses(uri);
    }

    @Test
    public void shouldCreateIntentForSyncingSinglePlaylist() throws Exception {
        final Urn playlistUrn = Urn.forPlaylist(1L);
        initiator.syncPlaylist(playlistUrn).subscribe(syncSubscriber);

        Intent intent = Robolectric.getShadowApplication().getNextStartedService();
        expect(intent).not.toBeNull();
        expect(intent.getAction()).toEqual(SyncActions.SYNC_PLAYLIST);
        expect(ParcelableUrn.unpack(SyncExtras.URN, intent.getExtras())).toEqual(playlistUrn);
        expect(intent.getParcelableExtra(ApiSyncService.EXTRA_STATUS_RECEIVER)).toBeInstanceOf(ResultReceiver.class);
    }

    @Test
    public void shouldCreateIntentForSyncingSingleTrack() throws Exception {
        initiator.syncTrack(Urn.forTrack(1L)).subscribe(legacySyncSubscriber);

        Intent intent = Robolectric.getShadowApplication().getNextStartedService();
        expect(intent).not.toBeNull();
        expect(intent.getData()).toEqual(Content.TRACKS.forQuery(String.valueOf(1L)));
        expect(intent.getBooleanExtra(ApiSyncService.EXTRA_IS_UI_REQUEST, false)).toBeTrue();
        expect(intent.getParcelableExtra(ApiSyncService.EXTRA_STATUS_RECEIVER)).toBeInstanceOf(ResultReceiver.class);
    }

    @Test
    public void syncTrackLikesShouldRequestTrackLikesSync() throws Exception {
        initiator.syncTrackLikes().subscribe(syncSubscriber);

        Intent intent = Robolectric.getShadowApplication().getNextStartedService();
        expect(intent).not.toBeNull();
        expect(intent.getAction()).toEqual(SyncActions.SYNC_TRACK_LIKES);
        expect(intent.getParcelableExtra(ApiSyncService.EXTRA_STATUS_RECEIVER)).toBeInstanceOf(ResultReceiverAdapter.class);
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

        Intent intent = Robolectric.getShadowApplication().getNextStartedService();
        expect(intent).not.toBeNull();
        expect(intent.getAction()).toEqual(SyncActions.SYNC_PLAYLIST_LIKES);
        expect(intent.getParcelableExtra(ApiSyncService.EXTRA_STATUS_RECEIVER)).toBeInstanceOf(ResultReceiverAdapter.class);
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
        initiator.requestTracksSync(Lists.<PropertySet>newArrayList(propertySet));

        Intent intent = Robolectric.getShadowApplication().getNextStartedService();
        expect(intent).not.toBeNull();
        expect(intent.getAction()).toEqual(SyncActions.SYNC_TRACKS);
        expect(ParcelableUrn.unpackList(SyncExtras.URNS, intent.getExtras()))
                .toContainExactly(propertySet.get(TrackProperty.URN));
    }

    @Test
    public void requestPlaylistsSyncShouldRequestPlaylistsSync() throws Exception {
        final PropertySet propertySet = ModelFixtures.create(ApiPlaylist.class).toPropertySet();
        initiator.requestPlaylistSync(Lists.<PropertySet>newArrayList(propertySet));

        Intent intent = Robolectric.getShadowApplication().getNextStartedService();
        expect(intent).not.toBeNull();
        expect(intent.getAction()).toEqual(SyncActions.SYNC_PLAYLISTS);
        expect(ParcelableUrn.unpackList(SyncExtras.URNS, intent.getExtras()))
                .toContainExactly(propertySet.get(PlaylistProperty.URN));
    }


    private void sendSyncChangedLegacyToUri(Uri uri) {
        Intent intent = Robolectric.getShadowApplication().getNextStartedService();
        final ResultReceiver resultReceiver = intent.getParcelableExtra(ApiSyncService.EXTRA_STATUS_RECEIVER);
        final Bundle resultData = new Bundle();
        resultData.putBoolean(uri.toString(), true);
        resultReceiver.send(ApiSyncService.STATUS_SYNC_FINISHED, resultData);
    }

    private void sendSyncChangedToUri(Uri uri) {
        Intent intent = Robolectric.getShadowApplication().getNextStartedService();
        final ResultReceiver resultReceiver = intent.getParcelableExtra(ApiSyncService.EXTRA_STATUS_RECEIVER);
        final Bundle resultData = new Bundle();
        resultData.putParcelable(ResultReceiverAdapter.SYNC_RESULT, SyncResult.success("action", true));
        resultReceiver.send(ApiSyncService.STATUS_SYNC_FINISHED, resultData);
    }
}
