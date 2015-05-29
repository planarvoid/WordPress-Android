package com.soundcloud.android.playlists;

import static com.soundcloud.android.Expect.expect;

import com.soundcloud.android.model.Urn;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import com.soundcloud.android.tracks.TrackItem;
import com.soundcloud.propeller.PropertySet;
import nl.jqno.equalsverifier.EqualsVerifier;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

@RunWith(SoundCloudTestRunner.class)
public class PlaylistWithTracksTest {

    @Test
    public void isLocalPlaylistIfUrnIsNegative() throws Exception {
        final PlaylistWithTracks playlistWithTracks = createPlaylistMetaData(Urn.forPlaylist(-123L), Collections.<TrackItem>emptyList());
        expect(playlistWithTracks.isLocalPlaylist()).toBeTrue();
    }

    @Test
    public void isNotLocalPlaylistTrueIfUrnIsMissing() throws Exception {
        final PlaylistWithTracks playlistWithTracks = createPlaylistMetaData(PropertySet.create(), Collections.<TrackItem>emptyList());
        expect(playlistWithTracks.isLocalPlaylist()).toBeFalse();
    }

    @Test
    public void isNotLocalPlaylistTrueIfUrnIsPositive() throws Exception {
        final PlaylistWithTracks playlistWithTracks = createPlaylistMetaData(Urn.forPlaylist(123L), Collections.<TrackItem>emptyList());
        expect(playlistWithTracks.isLocalPlaylist()).toBeFalse();
    }

    @Test
    public void isMissingMetadataIfPlaylistMetadataIsEmpty() throws Exception {
        final PlaylistWithTracks playlistWithTracks = createPlaylistMetaData(PropertySet.create(), Collections.<TrackItem>emptyList());
        expect(playlistWithTracks.isMissingMetaData()).toBeTrue();
    }

    @Test
    public void isNotMissingMetadataIfPlaylistMetadataIsEmpty() throws Exception {
        final PlaylistWithTracks playlistWithTracks = createPlaylistMetaData(Urn.forPlaylist(123L), Collections.<TrackItem>emptyList());
        expect(playlistWithTracks.isMissingMetaData()).toBeFalse();
    }

    @Test
    public void needsTracksIfTracklistEmpty() throws Exception {
        final PropertySet metadata = PropertySet.from(
                PlaylistProperty.URN.bind(Urn.forTrack(123L)),
                PlaylistProperty.TRACK_COUNT.bind(0) // the track count is usually wrong, as it does not count private tracks
        );

        final PlaylistWithTracks playlistWithTracks = createPlaylistMetaData(metadata, Collections.<TrackItem>emptyList());
        expect(playlistWithTracks.needsTracks()).toBeTrue();
    }

    @Test
    public void doesNotNeedTracksIfTrackListSizeGreaterThanZero() throws Exception {
        final PropertySet metadata = PropertySet.from(
                PlaylistProperty.URN.bind(Urn.forTrack(123L))
        );

        final PlaylistWithTracks playlistWithTracks = createPlaylistMetaData(metadata, ModelFixtures.trackItems(10));
        expect(playlistWithTracks.needsTracks()).toBeFalse();
    }

    @Test
    public void returnsTrackCountFromPlaylistMetadataIfTracksMissing() throws Exception {
        final PropertySet metadata = PropertySet.from(
                PlaylistProperty.URN.bind(Urn.forTrack(123L)),
                PlaylistProperty.TRACK_COUNT.bind(2)
        );

        final PlaylistWithTracks playlistWithTracks = createPlaylistMetaData(metadata, Collections.<TrackItem>emptyList());
        expect(playlistWithTracks.getTrackCount()).toEqual(2);
    }

    @Test
    public void returnsTrackCountFromTracklistIfTracksAreThere() throws Exception {
        final PropertySet metadata = PropertySet.from(
                PlaylistProperty.URN.bind(Urn.forTrack(123L)),
                PlaylistProperty.TRACK_COUNT.bind(1)
        );

        final PlaylistWithTracks playlistWithTracks = createPlaylistMetaData(metadata, ModelFixtures.trackItems(2));
        expect(playlistWithTracks.getTrackCount()).toEqual(2);
    }

    @Test
    public void returnsDurationFromPlaylistMetadataIfTracksMissing() throws Exception {
        final PropertySet metadata = PropertySet.from(
                PlaylistProperty.URN.bind(Urn.forTrack(123L)),
                PlaylistProperty.TRACK_COUNT.bind(1),
                PlaylistProperty.DURATION.bind(TimeUnit.SECONDS.toMillis(60))
        );

        final PlaylistWithTracks playlistWithTracks = createPlaylistMetaData(metadata, Collections.<TrackItem>emptyList());
        expect(playlistWithTracks.getDuration()).toEqual("1:00");
    }

    @Test
    public void returnsDurationFromTracklistIfTracksAreThere() throws Exception {
        final PropertySet metadata = PropertySet.from(
                PlaylistProperty.URN.bind(Urn.forTrack(123L)),
                PlaylistProperty.DURATION.bind(TimeUnit.SECONDS.toMillis(60))
        );

        final PlaylistWithTracks playlistWithTracks = createPlaylistMetaData(metadata, ModelFixtures.trackItems(2));
        expect(playlistWithTracks.getDuration()).toEqual("0:24");
    }

    @Test
    public void implementsEqualsAndHashCode() throws Exception {
        EqualsVerifier.forClass(PlaylistWithTracks.class).verify();
    }

    private PlaylistWithTracks createPlaylistMetaData(Urn playlistUrn, List<TrackItem> tracks) {
        return createPlaylistMetaData(PropertySet.from(PlaylistProperty.URN.bind(playlistUrn)), tracks);
    }

    private PlaylistWithTracks createPlaylistMetaData(PropertySet playlistMetadata, List<TrackItem> tracks) {
        return new PlaylistWithTracks(playlistMetadata, tracks);
    }
}
