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
        final PlaylistDetailHeaderItem item = PlaylistDetailHeaderItem.from(
                ModelFixtures.playlistBuilder().trackCount(1).build(),
                ModelFixtures.trackItems(2),
                true,
                resources()
        );

        assertThat(item.trackCount()).isEqualTo(2);
    }

    @Test
    public void returnsTrackCountFromPlaylistMetadataIfTracksMissing() {
        final PlaylistDetailHeaderItem item = PlaylistDetailHeaderItem.from(
                ModelFixtures.playlistBuilder().trackCount(1).build(),
                Collections.emptyList(),
                true,
                resources()
        );

        assertThat(item.trackCount()).isEqualTo(1);
    }

    @Test
    public void returnsDurationFromPlaylistMetadataIfTracksMissing() {
        final PlaylistDetailHeaderItem item = PlaylistDetailHeaderItem.from(
                ModelFixtures.playlistBuilder().duration(TimeUnit.SECONDS.toMillis(60)).build(),
                Collections.emptyList(),
                true,
                resources()
        );

        assertThat(item.duration()).isEqualTo("1:00");
    }

    @Test
    public void returnsDurationFromTracklistIfTracksAreThere() {
        final PlaylistDetailHeaderItem item = PlaylistDetailHeaderItem.from(
                ModelFixtures.playlistBuilder().duration(TimeUnit.SECONDS.toMillis(60)).build(),
                ModelFixtures.trackItems(2), // TODO : set actual durations when we have builders
                true,
                resources()
        );

        assertThat(item.duration()).isEqualTo("22:37");
    }
}
