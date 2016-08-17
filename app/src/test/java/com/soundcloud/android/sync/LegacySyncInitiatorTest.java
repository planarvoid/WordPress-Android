package com.soundcloud.android.sync;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.storage.provider.Content;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.robolectric.shadows.ShadowApplication;
import rx.Observable;
import rx.Subscriber;
import rx.observers.TestObserver;
import rx.observers.TestSubscriber;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.ResultReceiver;

import java.util.Arrays;

public class LegacySyncInitiatorTest extends AndroidUnitTest {

    private LegacySyncInitiator initiator;
    private Subscriber<Boolean> legacySyncSubscriber = new TestSubscriber<>();
    private TestObserver<SyncJobResult> syncSubscriber = new TestObserver<>();

    @Mock private ResultReceiver resultReceiver;
    @Mock private SyncStateManager syncStateManager;

    @Before
    public void setup() {
        initiator = new LegacySyncInitiator(context(), syncStateManager);
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
        verify(syncStateManager).resetSyncMissesAsync(uri);
    }

    @Test
    public void syncTrackLikesShouldRequestTrackLikesSync() throws Exception {
        initiator.syncTrackLikes().subscribe(syncSubscriber);

        Intent intent = ShadowApplication.getInstance().getNextStartedService();
        assertThat(intent).isNotNull();
        assertThat(intent.getAction()).isEqualTo(LegacySyncActions.SYNC_TRACK_LIKES);
        assertThat(intent.getParcelableExtra(ApiSyncService.EXTRA_STATUS_RECEIVER)).isInstanceOf(ResultReceiverAdapter.class);
    }

    @Test
    public void shouldResetMyLikesSyncMissesOnChangedTrackLikesSync() {
        initiator.syncTrackLikes().subscribe(syncSubscriber);
        final Uri uri = Content.ME_LIKES.uri;
        sendSyncChangedToUri();
        verify(syncStateManager).resetSyncMissesAsync(uri);
    }

    @Test
    public void syncPlaylistLikesShouldRequestPlaylistLikesSync() throws Exception {
        initiator.syncPlaylistLikes().subscribe(syncSubscriber);

        Intent intent = ShadowApplication.getInstance().getNextStartedService();
        assertThat(intent).isNotNull();
        assertThat(intent.getAction()).isEqualTo(LegacySyncActions.SYNC_PLAYLIST_LIKES);
        assertThat(intent.getParcelableExtra(ApiSyncService.EXTRA_STATUS_RECEIVER)).isInstanceOf(ResultReceiverAdapter.class);
    }

    @Test
    public void shouldResetMyLikesSyncMissesOnChangedPlaylistLikesSync() {
        initiator.syncPlaylistLikes().subscribe(syncSubscriber);
        final Uri uri = Content.ME_LIKES.uri;
        sendSyncChangedToUri();
        verify(syncStateManager).resetSyncMissesAsync(uri);
    }

    @Test
    public void refreshCollectionsSendsCollectionSyncIntent() {
        initiator.refreshCollections().subscribe(legacySyncSubscriber);

        Intent intent = ShadowApplication.getInstance().getNextStartedService();
        assertThat(intent).isNotNull();
        assertThat(intent.getAction()).isEqualTo(ApiSyncService.ACTION_HARD_REFRESH);
        assertThat(intent.getParcelableArrayListExtra(ApiSyncService.EXTRA_SYNC_URIS)).isEqualTo(Arrays.asList(
                LegacySyncContent.MyLikes.content.uri,
                LegacySyncContent.MyPlaylists.content.uri));
    }

    @Test
    public void refreshCollectionsResetsSyncMissesOnAllCollections() {
        when(syncStateManager.resetSyncMissesAsync(any(Uri.class))).thenReturn(Observable.just(true));
        initiator.refreshCollections().subscribe(legacySyncSubscriber);
        final Uri[] uris = new Uri[]{Content.ME_PLAYLISTS.uri, Content.ME_LIKES.uri};

        sendSyncChangedLegacyToUri(uris);

        for (Uri uri : uris) {
            verify(syncStateManager).resetSyncMissesAsync(uri);
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

    private void sendSyncChangedToUri() {
        Intent intent = ShadowApplication.getInstance().getNextStartedService();
        final ResultReceiver resultReceiver = intent.getParcelableExtra(ApiSyncService.EXTRA_STATUS_RECEIVER);
        final Bundle resultData = new Bundle();
        resultData.putParcelable(ResultReceiverAdapter.SYNC_RESULT, SyncJobResult.success("action", true));
        resultReceiver.send(ApiSyncService.STATUS_SYNC_FINISHED, resultData);
    }
}
