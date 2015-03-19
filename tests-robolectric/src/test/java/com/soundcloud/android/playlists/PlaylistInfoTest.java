package com.soundcloud.android.playlists;

import static com.soundcloud.android.Expect.expect;

import com.soundcloud.android.model.Urn;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.testsupport.fixtures.TestPropertySets;
import com.soundcloud.propeller.PropertySet;
import nl.jqno.equalsverifier.EqualsVerifier;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

@RunWith(SoundCloudTestRunner.class)
public class PlaylistInfoTest {

    @Test
    public void isLocalPlaylistIfUrnIsNegative() throws Exception {
        final PlaylistInfo playlistInfo = createPlaylistMetaData(Urn.forPlaylist(-123L), Collections.<PropertySet>emptyList());
        expect(playlistInfo.isLocalPlaylist()).toBeTrue();
    }

    @Test
    public void isNotLocalPlaylistTrueIfUrnIsMissing() throws Exception {
        final PlaylistInfo playlistInfo = createPlaylistMetaData(PropertySet.create(), Collections.<PropertySet>emptyList());
        expect(playlistInfo.isLocalPlaylist()).toBeFalse();
    }

    @Test
    public void isNotLocalPlaylistTrueIfUrnIsPositive() throws Exception {
        final PlaylistInfo playlistInfo = createPlaylistMetaData(Urn.forPlaylist(123L), Collections.<PropertySet>emptyList());
        expect(playlistInfo.isLocalPlaylist()).toBeFalse();
    }

    @Test
    public void isMissingMetadataIfPlaylistMetadataIsEmpty() throws Exception {
        final PlaylistInfo playlistInfo = createPlaylistMetaData(PropertySet.create(), Collections.<PropertySet>emptyList());
        expect(playlistInfo.isMissingMetaData()).toBeTrue();
    }

    @Test
    public void isNotMissingMetadataIfPlaylistMetadataIsEmpty() throws Exception {
        final PlaylistInfo playlistInfo = createPlaylistMetaData(Urn.forPlaylist(123L), Collections.<PropertySet>emptyList());
        expect(playlistInfo.isMissingMetaData()).toBeFalse();
    }

    @Test
    public void needsTracksIfTracklistEmptyAndTrackCountGreaterThanZero() throws Exception {
        final PropertySet metadata = PropertySet.from(
                PlaylistProperty.URN.bind(Urn.forTrack(123L)),
                PlaylistProperty.TRACK_COUNT.bind(1)
        );

        final PlaylistInfo playlistInfo = createPlaylistMetaData(metadata, Collections.<PropertySet>emptyList());
        expect(playlistInfo.needsTracks()).toBeTrue();
    }

    @Test
    public void doesNotNeedTracksIfTracklistEmptyAndTrackCountIsZero() throws Exception {
        final PropertySet metadata = PropertySet.from(
                PlaylistProperty.URN.bind(Urn.forTrack(123L)),
                PlaylistProperty.TRACK_COUNT.bind(0)
        );

        final PlaylistInfo playlistInfo = createPlaylistMetaData(metadata, Collections.<PropertySet>emptyList());
        expect(playlistInfo.needsTracks()).toBeFalse();
    }

    @Test
    public void doesNotNeedTracksIfTrackListSizeGreaterThanZero() throws Exception {
        final PropertySet metadata = PropertySet.from(
                PlaylistProperty.URN.bind(Urn.forTrack(123L))
        );

        final PlaylistInfo playlistInfo = createPlaylistMetaData(metadata, Arrays.asList(TestPropertySets.fromApiTrack()));
        expect(playlistInfo.needsTracks()).toBeFalse();
    }

    @Test
    public void returnsTrackCountFromPlaylistMetadataIfTracksMissing() throws Exception {
        final PropertySet metadata = PropertySet.from(
                PlaylistProperty.URN.bind(Urn.forTrack(123L)),
                PlaylistProperty.TRACK_COUNT.bind(2)
        );

        final PlaylistInfo playlistInfo = createPlaylistMetaData(metadata, Collections.<PropertySet>emptyList());
        expect(playlistInfo.getTrackCount()).toEqual(2);
    }

    @Test
    public void returnsTrackCountFromTracklistIfTracksAreThere() throws Exception {
        final PropertySet metadata = PropertySet.from(
                PlaylistProperty.URN.bind(Urn.forTrack(123L)),
                PlaylistProperty.TRACK_COUNT.bind(1)
        );

        final PlaylistInfo playlistInfo = createPlaylistMetaData(metadata, Arrays.asList(TestPropertySets.fromApiTrack(), TestPropertySets.fromApiTrack()));
        expect(playlistInfo.getTrackCount()).toEqual(2);
    }

    @Test
    public void returnsDurationFromPlaylistMetadataIfTracksMissing() throws Exception {
        final PropertySet metadata = PropertySet.from(
                PlaylistProperty.URN.bind(Urn.forTrack(123L)),
                PlaylistProperty.TRACK_COUNT.bind(1),
                PlaylistProperty.DURATION.bind((int) TimeUnit.SECONDS.toMillis(60))
        );

        final PlaylistInfo playlistInfo = createPlaylistMetaData(metadata, Collections.<PropertySet>emptyList());
        expect(playlistInfo.getDuration()).toEqual("1:00");
    }

    @Test
    public void returnsDurationFromTracklistIfTracksAreThere() throws Exception {
        final PropertySet metadata = PropertySet.from(
                PlaylistProperty.URN.bind(Urn.forTrack(123L)),
                PlaylistProperty.DURATION.bind((int) TimeUnit.SECONDS.toMillis(60))
        );

        final PlaylistInfo playlistInfo = createPlaylistMetaData(metadata, Arrays.asList(TestPropertySets.fromApiTrack(), TestPropertySets.fromApiTrack()));
        expect(playlistInfo.getDuration()).toEqual("0:24");
    }

    @Test
    public void implementsEqualsAndHashCode() throws Exception {
        EqualsVerifier.forClass(PlaylistInfo.class).verify();
    }

    private PlaylistInfo createPlaylistMetaData(Urn playlistUrn, List<PropertySet> tracks) {
        return createPlaylistMetaData(PropertySet.from(PlaylistProperty.URN.bind(playlistUrn)), tracks);
    }

    private PlaylistInfo createPlaylistMetaData(PropertySet playlistMetadata, List<PropertySet> tracks) {
        return new PlaylistInfo(playlistMetadata, tracks);
    }
}
