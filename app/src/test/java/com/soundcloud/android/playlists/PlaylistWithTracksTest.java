package com.soundcloud.android.playlists;

import static org.assertj.core.api.Assertions.assertThat;

import com.soundcloud.android.model.Urn;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import com.soundcloud.android.tracks.TrackItem;
import org.junit.Test;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class PlaylistWithTracksTest extends AndroidUnitTest {
    private Playlist.Builder builder = ModelFixtures.playlistBuilder().urn(Urn.forTrack(123L));

    @Test
    public void returnsTrackCountFromPlaylistMetadataIfTracksMissing() {
        final PlaylistWithTracks playlistWithTracks = createPlaylistMetaData(builder.trackCount(2).build(),
                                                                             Collections.<TrackItem>emptyList());
        assertThat(playlistWithTracks.getTrackCount()).isEqualTo(2);
    }

    @Test
    public void returnsTrackCountFromTracklistIfTracksAreThere() {
        final PlaylistWithTracks playlistWithTracks = createPlaylistMetaData(builder.trackCount(1).build(), ModelFixtures.trackItems(2));
        assertThat(playlistWithTracks.getTrackCount()).isEqualTo(2);
    }

    @Test
    public void returnsDurationFromPlaylistMetadataIfTracksMissing() {
        final Playlist playlist = builder.trackCount(1).duration(TimeUnit.SECONDS.toMillis(60)).build();

        final PlaylistWithTracks playlistWithTracks = createPlaylistMetaData(playlist,
                                                                             Collections.<TrackItem>emptyList());
        assertThat(playlistWithTracks.getDuration()).isEqualTo("1:00");
    }

    @Test
    public void returnsDurationFromTracklistIfTracksAreThere() {
        final Playlist playlist = builder.trackCount(1).duration(TimeUnit.SECONDS.toMillis(60)).build();

        final PlaylistWithTracks playlistWithTracks = createPlaylistMetaData(playlist, ModelFixtures.trackItems(2));
        assertThat(playlistWithTracks.getDuration()).isEqualTo("22:37");
    }

    private PlaylistWithTracks createPlaylistMetaData(Playlist playlist, List<TrackItem> tracks) {
        return new PlaylistWithTracks(playlist, tracks);
    }
}
