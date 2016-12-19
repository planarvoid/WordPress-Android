package com.soundcloud.android.sync.entities;

import static java.util.Arrays.asList;
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
        factory = new EntitySyncRequestFactory(InjectionSupport.lazyOf(tracksSyncJob),
                                               InjectionSupport.lazyOf(playlistsSyncJob),
                                               InjectionSupport.lazyOf(usersSyncJob),
                                               new TestEventBus());
    }

    @Test
    public void tracksSyncableReturnsTrackSyncJob() {
        EntitySyncRequest entitySyncRequest = factory.create(Syncable.TRACKS, asList(Urn.forTrack(1L)), resultReceiver);

        assertThat(entitySyncRequest.getPendingJobs()).containsExactly(tracksSyncJob);
    }

    @Test
    public void playlistsSyncableReturnsPlaylistsSyncJob() {
        EntitySyncRequest entitySyncRequest = factory.create(Syncable.PLAYLISTS, asList(Urn.forPlaylist(1L)), resultReceiver);

        assertThat(entitySyncRequest.getPendingJobs()).containsExactly(playlistsSyncJob);
    }

    @Test
    public void usersSyncableReturnsUsersSyncJob() {
        EntitySyncRequest entitySyncRequest = factory.create(Syncable.USERS, asList(Urn.forUser(1L)), resultReceiver);

        assertThat(entitySyncRequest.getPendingJobs()).containsExactly(usersSyncJob);
    }
}
