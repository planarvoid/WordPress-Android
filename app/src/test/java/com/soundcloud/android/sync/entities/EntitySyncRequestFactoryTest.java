package com.soundcloud.android.sync.entities;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;

import com.soundcloud.android.model.Urn;
import com.soundcloud.android.sync.Syncable;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.InjectionSupport;
import com.soundcloud.rx.eventbus.TestEventBus;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import android.os.ResultReceiver;

public class EntitySyncRequestFactoryTest extends AndroidUnitTest {

    private EntitySyncRequestFactory factory;

    @Mock private EntitySyncJob tracksSyncJob;
    @Mock private EntitySyncJob playlistsSyncJob;
    @Mock private EntitySyncJob usersSyncJob;
    @Mock private ResultReceiver resultReceiver;

    @Before
    public void setUp() throws Exception {
        factory = new EntitySyncRequestFactory(InjectionSupport.providerOf(tracksSyncJob),
                                               InjectionSupport.providerOf(playlistsSyncJob),
                                               InjectionSupport.providerOf(usersSyncJob),
                                               new TestEventBus());
    }

    @Test
    public void tracksSyncableReturnsTrackSyncJob() {
        EntitySyncRequest entitySyncRequest = factory.create(Syncable.TRACKS, singletonList(Urn.forTrack(1L)), resultReceiver);

        assertThat(entitySyncRequest.getPendingJobs()).containsExactly(tracksSyncJob);
    }

    @Test
    public void playlistsSyncableReturnsPlaylistsSyncJob() {
        EntitySyncRequest entitySyncRequest = factory.create(Syncable.PLAYLISTS, singletonList(Urn.forPlaylist(1L)), resultReceiver);

        assertThat(entitySyncRequest.getPendingJobs()).containsExactly(playlistsSyncJob);
    }

    @Test
    public void usersSyncableReturnsUsersSyncJob() {
        EntitySyncRequest entitySyncRequest = factory.create(Syncable.USERS, singletonList(Urn.forUser(1L)), resultReceiver);

        assertThat(entitySyncRequest.getPendingJobs()).containsExactly(usersSyncJob);
    }
}
