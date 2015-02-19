package com.soundcloud.android.sync;

import static com.soundcloud.android.Expect.expect;
import static com.soundcloud.android.testsupport.InjectionSupport.lazyOf;
import static org.mockito.Mockito.verify;

import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.rx.eventbus.TestEventBus;
import com.soundcloud.android.sync.entities.EntitySyncRequestFactory;
import com.soundcloud.android.sync.likes.SyncPlaylistLikesJob;
import com.soundcloud.android.sync.likes.SyncTrackLikesJob;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import android.content.Intent;

@RunWith(SoundCloudTestRunner.class)
public class SyncRequestFactoryTest {

    private SyncRequestFactory syncRequestFactory;

    @Mock private LegacySyncRequest.Factory syncIntentFactory;
    @Mock private SyncTrackLikesJob syncTrackLikesJob;
    @Mock private SyncPlaylistLikesJob syncPlaylistLikesJob;
    @Mock private EntitySyncRequestFactory entitySyncRequestFactory;

    @Before
    public void setUp() throws Exception {
        syncRequestFactory = new SyncRequestFactory(syncIntentFactory, lazyOf(syncTrackLikesJob), lazyOf(syncPlaylistLikesJob), entitySyncRequestFactory, new TestEventBus());
    }

    @Test
    public void returnsSingleRequestJobWithTrackLikesJob() throws Exception {
        SyncRequest syncRequest = syncRequestFactory.create(new Intent(SyncActions.SYNC_TRACK_LIKES));
        expect(syncRequest.getPendingJobs().contains(syncTrackLikesJob)).toBeTrue();
        expect(syncRequest.getPendingJobs()).toNumber(1);
    }

    @Test
    public void returnsSingleRequestJobWithPlaylistLikesJob() throws Exception {
        SyncRequest syncRequest = syncRequestFactory.create(new Intent(SyncActions.SYNC_PLAYLIST_LIKES));
        expect(syncRequest.getPendingJobs().contains(syncPlaylistLikesJob)).toBeTrue();
        expect(syncRequest.getPendingJobs()).toNumber(1);
    }

    @Test
    public void createSyncResourcesRequestFromSyncTracksIntent() throws Exception {
        final Intent intent = new Intent(SyncActions.SYNC_TRACKS);
        syncRequestFactory.create(intent);
        verify(entitySyncRequestFactory).create(intent);
    }

    @Test
    public void createSyncResourcesRequestFromSyncPlaylistsIntent() throws Exception {
        final Intent intent = new Intent(SyncActions.SYNC_PLAYLISTS);
        syncRequestFactory.create(intent);
        verify(entitySyncRequestFactory).create(intent);
    }


}