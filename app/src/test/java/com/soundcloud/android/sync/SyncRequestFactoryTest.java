package com.soundcloud.android.sync;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.model.Urn;
import com.soundcloud.android.sync.entities.EntitySyncRequestFactory;
import com.soundcloud.android.sync.likes.DefaultSyncJob;
import com.soundcloud.android.sync.playlists.SinglePlaylistSyncerFactory;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.TestSyncer;
import com.soundcloud.rx.eventbus.TestEventBus;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Matchers;
import org.mockito.Mock;

import android.content.Intent;

import java.util.ArrayList;
import java.util.Arrays;

public class SyncRequestFactoryTest extends AndroidUnitTest {

    private SyncRequestFactory syncRequestFactory;

    @Mock private SyncerRegistry syncerRegistry;
    @Mock private SingleJobRequestFactory singleJobRequestFactory;
    @Mock private MultiJobRequestFactory multiJobRequestFactory;
    @Mock private EntitySyncRequestFactory entitySyncRequestFactory;
    @Mock private SinglePlaylistSyncerFactory singlePlaylistSyncerFactory;
    @Mock private ResultReceiverAdapter resultReceiverAdapter;

    @Before
    public void setUp() throws Exception {
        syncRequestFactory = new SyncRequestFactory(
                syncerRegistry,
                singleJobRequestFactory,
                multiJobRequestFactory,
                entitySyncRequestFactory,
                singlePlaylistSyncerFactory,
                new TestEventBus()
        );
    }

    @Test
    public void createSyncSinglePlaylistRequestFromSyncPlaylistIntent() throws Exception {
        final Urn playlistUrn = Urn.forPlaylist(123L);
        final ArrayList<Urn> entities = new ArrayList<>(Arrays.asList(playlistUrn));
        final Intent intent = new Intent().putExtra(ApiSyncService.EXTRA_SYNCABLE, Syncable.PLAYLIST)
                                          .putExtra(ApiSyncService.EXTRA_SYNCABLE_ENTITIES, entities)
                                          .putExtra(ApiSyncService.EXTRA_STATUS_RECEIVER, resultReceiverAdapter);
        syncRequestFactory.create(intent);
        verify(singlePlaylistSyncerFactory).create(playlistUrn);
    }

    @Test
    public void createSyncTrackEntitesRequestFromSyncableTracksIntent() throws Exception {
        final ArrayList<Urn> entities = new ArrayList<>(Arrays.asList(Urn.forTrack(123L)));
        final Intent intent = new Intent().putExtra(ApiSyncService.EXTRA_SYNCABLE, Syncable.TRACKS)
                                          .putParcelableArrayListExtra(ApiSyncService.EXTRA_SYNCABLE_ENTITIES, entities)
                                          .putExtra(ApiSyncService.EXTRA_STATUS_RECEIVER, resultReceiverAdapter);
        syncRequestFactory.create(intent);
        verify(entitySyncRequestFactory).create(Syncable.TRACKS, entities, resultReceiverAdapter);
    }

    @Test
    public void createSyncPlaylistEntitesRequestFromSyncablePlaylistsIntent() throws Exception {
        final ArrayList<Urn> entities = new ArrayList<>(Arrays.asList(Urn.forPlaylist(123L)));
        final Intent intent = new Intent().putExtra(ApiSyncService.EXTRA_SYNCABLE, Syncable.PLAYLISTS)
                                          .putParcelableArrayListExtra(ApiSyncService.EXTRA_SYNCABLE_ENTITIES, entities)
                                          .putExtra(ApiSyncService.EXTRA_STATUS_RECEIVER, resultReceiverAdapter);
        syncRequestFactory.create(intent);
        verify(entitySyncRequestFactory).create(Syncable.PLAYLISTS, entities, resultReceiverAdapter);
    }

    @Test
    public void createSyncUserEntitesRequestFromSyncableUsersIntent() throws Exception {
        final ArrayList<Urn> entities = new ArrayList<>(Arrays.asList(Urn.forUser(123L)));
        final Intent intent = new Intent().putExtra(ApiSyncService.EXTRA_SYNCABLE, Syncable.USERS)
                                          .putParcelableArrayListExtra(ApiSyncService.EXTRA_SYNCABLE_ENTITIES, entities)
                                          .putExtra(ApiSyncService.EXTRA_STATUS_RECEIVER, resultReceiverAdapter);
        syncRequestFactory.create(intent);
        verify(entitySyncRequestFactory).create(Syncable.USERS, entities, resultReceiverAdapter);
    }

    @Test
    public void createsSingleRequestJobForSyncable() throws Exception {
        final Intent intent = new Intent().putExtra(ApiSyncService.EXTRA_SYNCABLE, Syncable.CHARTS)
                                          .putExtra(ApiSyncService.EXTRA_STATUS_RECEIVER, resultReceiverAdapter)
                                          .putExtra(ApiSyncService.EXTRA_IS_UI_REQUEST, true).setAction("action");

        final SingleJobRequest request = mock(SingleJobRequest.class);
        final SyncerRegistry.SyncProvider syncProvider = TestSyncData.get(Syncable.CHARTS);
        final ArgumentCaptor<DefaultSyncJob> defaultSyncJobArgumentCaptor = ArgumentCaptor.forClass(DefaultSyncJob.class);

        when(syncerRegistry.get(Syncable.CHARTS)).thenReturn(syncProvider);
        when(singleJobRequestFactory.create(defaultSyncJobArgumentCaptor.capture(),
                                            Matchers.eq(Syncable.CHARTS.name()), Matchers.eq(true), Matchers.same(resultReceiverAdapter))).thenReturn(request);

        assertThat(syncRequestFactory.create(intent)).isSameAs(request);

        final DefaultSyncJob syncJob = defaultSyncJobArgumentCaptor.getValue();
        syncJob.run();
        assertThat(((TestSyncer) syncProvider.syncer("action", false)).hasRun()).isTrue();
    }
}
