package com.soundcloud.android.playlists;

import static org.assertj.core.api.Assertions.assertThat;

import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import org.junit.Test;

import java.util.Collections;
import java.util.concurrent.TimeUnit;

public class PlaylistHeaderDetailItemTest extends AndroidUnitTest {

    @Test
    public void returnsTrackCountFromTracklistIfTracksAreThere() {
        final PlaylistDetailsMetadata item = PlaylistDetailsMetadata.from(ModelFixtures.playlistBuilder().trackCount(1).build(), ModelFixtures.trackItems(2), true, false, resources());

        assertThat(item.trackCount()).isEqualTo(2);
    }

    @Test
    public void returnsTrackCountFromPlaylistMetadataIfTracksMissing() {
        final PlaylistDetailsMetadata item = PlaylistDetailsMetadata.from(ModelFixtures.playlistBuilder().trackCount(1).build(), Collections.emptyList(), true, false, resources());

        assertThat(item.trackCount()).isEqualTo(1);
    }

    @Test
    public void returnsDurationFromPlaylistMetadataIfTracksMissing() {
        final PlaylistDetailsMetadata item = PlaylistDetailsMetadata.from(ModelFixtures.playlistBuilder().duration(TimeUnit.SECONDS.toMillis(60)).build(),
                                                                          Collections.emptyList(),
                                                                          true,
                                                                          false,
                                                                          resources());

        assertThat(item.duration()).isEqualTo("1:00");
    }

    @Test
    public void returnsDurationFromTracklistIfTracksAreThere() {
        final PlaylistDetailsMetadata item = PlaylistDetailsMetadata.from(ModelFixtures.playlistBuilder().duration(TimeUnit.SECONDS.toMillis(60)).build(),
                                                                          ModelFixtures.trackItems(2),
                                                                          true,
                                                                          false,
                                                                          resources());

        assertThat(item.duration()).isEqualTo("22:37");
    }
}
