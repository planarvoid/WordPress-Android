package com.soundcloud.android.sync.playlists;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.soundcloud.android.model.Urn;
import org.junit.Test;

public class SinglePlaylistSyncJobTest {

    @Test
    public void singlePlaylistSyncerEqualityIsBasedOnUrn() throws Exception {

        SinglePlaylistSyncer syncer = mock(SinglePlaylistSyncer.class);

        assertThat(new SinglePlaylistSyncJob(syncer, Urn.forPlaylist(1)))
                .isEqualTo(new SinglePlaylistSyncJob(syncer, Urn.forPlaylist(1)));

        assertThat(new SinglePlaylistSyncJob(syncer, Urn.forPlaylist(1)))
                .isNotEqualTo(new SinglePlaylistSyncJob(syncer, Urn.forPlaylist(2)));

    }
}
