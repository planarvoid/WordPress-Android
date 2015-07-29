package com.soundcloud.android.sync;

import static com.soundcloud.android.testsupport.InjectionSupport.lazyOf;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

import com.soundcloud.android.model.Urn;
import com.soundcloud.android.rx.eventbus.TestEventBus;
import com.soundcloud.android.sync.entities.EntitySyncRequestFactory;
import com.soundcloud.android.sync.likes.SyncPlaylistLikesJob;
import com.soundcloud.android.sync.likes.SyncTrackLikesJob;
import com.soundcloud.android.sync.playlists.SinglePlaylistSyncerFactory;
import com.soundcloud.android.sync.recommendations.RecommendationsSyncer;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import android.content.Intent;

public class SyncRequestFactoryTest extends AndroidUnitTest {

    private SyncRequestFactory syncRequestFactory;

    @Mock private LegacySyncRequest.Factory syncIntentFactory;
    @Mock private SyncTrackLikesJob syncTrackLikesJob;
    @Mock private SyncPlaylistLikesJob syncPlaylistLikesJob;
    @Mock private EntitySyncRequestFactory entitySyncRequestFactory;
    @Mock private SinglePlaylistSyncerFactory singlePlaylistSyncerFactory;
    @Mock private ResultReceiverAdapter resultReceiverAdapter;
    @Mock private RecommendationsSyncer recommendationsSyncer;

    @Before
    public void setUp() throws Exception {
        syncRequestFactory = new SyncRequestFactory(syncIntentFactory,
                lazyOf(syncTrackLikesJob), lazyOf(syncPlaylistLikesJob),
                entitySyncRequestFactory, singlePlaylistSyncerFactory,
                lazyOf(recommendationsSyncer), new TestEventBus());
    }

    @Test
    public void returnsSingleRequestJobWithTrackLikesJob() throws Exception {
        SyncRequest syncRequest = syncRequestFactory.create(new Intent(SyncActions.SYNC_TRACK_LIKES));
        assertThat(syncRequest.getPendingJobs()).hasSize(1);
        assertThat(syncRequest.getPendingJobs().contains(syncTrackLikesJob)).isTrue();
    }

    @Test
    public void returnsSingleRequestJobWithPlaylistLikesJob() throws Exception {
        SyncRequest syncRequest = syncRequestFactory.create(new Intent(SyncActions.SYNC_PLAYLIST_LIKES));
        assertThat(syncRequest.getPendingJobs()).hasSize(1);
        assertThat(syncRequest.getPendingJobs().contains(syncPlaylistLikesJob)).isTrue();
    }

    @Test
    public void createSyncResourcesRequestFromSyncTracksIntent() throws Exception {
        final Intent intent = new Intent(SyncActions.SYNC_TRACKS).putExtra(ApiSyncService.EXTRA_STATUS_RECEIVER, resultReceiverAdapter);
        syncRequestFactory.create(intent);
        verify(entitySyncRequestFactory).create(intent, resultReceiverAdapter);
    }

    @Test
    public void createSyncResourcesRequestFromSyncPlaylistsIntent() throws Exception {
        final Intent intent = new Intent(SyncActions.SYNC_PLAYLISTS).putExtra(ApiSyncService.EXTRA_STATUS_RECEIVER, resultReceiverAdapter);
        syncRequestFactory.create(intent);
        verify(entitySyncRequestFactory).create(intent, resultReceiverAdapter);
    }

    @Test
    public void createSyncResourcesRequestFromSyncUsersIntent() {
        final Intent intent = new Intent(SyncActions.SYNC_USERS).putExtra(ApiSyncService.EXTRA_STATUS_RECEIVER, resultReceiverAdapter);
        syncRequestFactory.create(intent);
        verify(entitySyncRequestFactory).create(intent, resultReceiverAdapter);
    }

    @Test
    public void createSyncSinglePlaylistRequestFromSyncPlaylistIntent() throws Exception {
        final Urn playlistUrn = Urn.forPlaylist(123L);
        final Intent intent = new Intent(SyncActions.SYNC_PLAYLIST).putExtra(SyncExtras.URN, playlistUrn);
        syncRequestFactory.create(intent);
        verify(singlePlaylistSyncerFactory).create(playlistUrn);
    }


}