package com.soundcloud.android.playlists;

import static org.assertj.core.api.Assertions.assertThat;

import com.soundcloud.android.model.Urn;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import com.soundcloud.android.tracks.TrackItem;
import com.soundcloud.java.collections.PropertySet;
import org.junit.Test;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class PlaylistWithTracksTest extends AndroidUnitTest {

    @Test
    public void isLocalPlaylistIfUrnIsNegative() {
        final PlaylistWithTracks playlistWithTracks = createPlaylistMetaData(Urn.forPlaylist(-123L),
                                                                             Collections.<TrackItem>emptyList());
        assertThat(playlistWithTracks.isLocalPlaylist()).isTrue();
    }

    @Test
    public void isNotLocalPlaylistTrueIfUrnIsPositive() {
        final PlaylistWithTracks playlistWithTracks = createPlaylistMetaData(Urn.forPlaylist(123L),
                                                                             Collections.<TrackItem>emptyList());
        assertThat(playlistWithTracks.isLocalPlaylist()).isFalse();
    }

    @Test
    public void needsTracksIfTracklistEmpty() {
        final PropertySet metadata = PropertySet.from(
                PlaylistProperty.URN.bind(Urn.forTrack(123L)),
                PlaylistProperty.TRACK_COUNT.bind(0)
                // the track count is usually wrong, as it does not count private tracks
        );

        final PlaylistWithTracks playlistWithTracks = createPlaylistMetaData(metadata,
                                                                             Collections.<TrackItem>emptyList());
        assertThat(playlistWithTracks.needsTracks()).isTrue();
    }

    @Test
    public void doesNotNeedTracksIfTrackListSizeGreaterThanZero() {
        final PropertySet metadata = PropertySet.from(
                PlaylistProperty.URN.bind(Urn.forTrack(123L))
        );

        final PlaylistWithTracks playlistWithTracks = createPlaylistMetaData(metadata, ModelFixtures.trackItems(10));
        assertThat(playlistWithTracks.needsTracks()).isFalse();
    }

    @Test
    public void returnsTrackCountFromPlaylistMetadataIfTracksMissing() {
        final PropertySet metadata = PropertySet.from(
                PlaylistProperty.URN.bind(Urn.forTrack(123L)),
                PlaylistProperty.TRACK_COUNT.bind(2)
        );

        final PlaylistWithTracks playlistWithTracks = createPlaylistMetaData(metadata,
                                                                             Collections.<TrackItem>emptyList());
        assertThat(playlistWithTracks.getTrackCount()).isEqualTo(2);
    }

    @Test
    public void returnsTrackCountFromTracklistIfTracksAreThere() {
        final PropertySet metadata = PropertySet.from(
                PlaylistProperty.URN.bind(Urn.forTrack(123L)),
                PlaylistProperty.TRACK_COUNT.bind(1)
        );

        final PlaylistWithTracks playlistWithTracks = createPlaylistMetaData(metadata, ModelFixtures.trackItems(2));
        assertThat(playlistWithTracks.getTrackCount()).isEqualTo(2);
    }

    @Test
    public void returnsDurationFromPlaylistMetadataIfTracksMissing() {
        final PropertySet metadata = PropertySet.from(
                PlaylistProperty.URN.bind(Urn.forTrack(123L)),
                PlaylistProperty.TRACK_COUNT.bind(1),
                PlaylistProperty.PLAYLIST_DURATION.bind(TimeUnit.SECONDS.toMillis(60))
        );

        final PlaylistWithTracks playlistWithTracks = createPlaylistMetaData(metadata,
                                                                             Collections.<TrackItem>emptyList());
        assertThat(playlistWithTracks.getDuration()).isEqualTo("1:00");
    }

    @Test
    public void returnsDurationFromTracklistIfTracksAreThere() {
        final PropertySet metadata = PropertySet.from(
                PlaylistProperty.URN.bind(Urn.forTrack(123L)),
                PlaylistProperty.PLAYLIST_DURATION.bind(TimeUnit.SECONDS.toMillis(60))
        );

        final PlaylistWithTracks playlistWithTracks = createPlaylistMetaData(metadata, ModelFixtures.trackItems(2));
        assertThat(playlistWithTracks.getDuration()).isEqualTo("22:37");
    }

    private PlaylistWithTracks createPlaylistMetaData(Urn playlistUrn, List<TrackItem> tracks) {
        return createPlaylistMetaData(PropertySet.from(PlaylistProperty.URN.bind(playlistUrn)), tracks);
    }

    private PlaylistWithTracks createPlaylistMetaData(PropertySet playlistMetadata, List<TrackItem> tracks) {
        return new PlaylistWithTracks(PlaylistItem.from(playlistMetadata), tracks);
    }
}
