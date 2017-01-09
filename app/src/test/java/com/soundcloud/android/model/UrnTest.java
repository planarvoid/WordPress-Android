package com.soundcloud.android.model;

import static org.assertj.core.api.Assertions.assertThat;

import com.soundcloud.android.testsupport.AndroidUnitTest;
import nl.jqno.equalsverifier.EqualsVerifier;
import org.junit.Test;

import android.os.Parcel;

public class UrnTest extends AndroidUnitTest {

    @Test
    public void shouldValidateLocalPlaylistUrnAsLocalUrn() {
        assertThat(new Urn("local:playlists:1").isLocal()).isTrue();
    }

    @Test
    public void shouldNotValidateUnknownUrnAsLocalUrn() {
        assertThat(new Urn("unknown:playlists:1").isLocal()).isFalse();
    }

    @Test
    public void shouldParseUserUrns() throws Exception {
        Urn urn = new Urn("soundcloud:users:123");
        assertThat(urn.isUser()).isTrue();
        assertThat(urn.getNumericId()).isEqualTo(123L);
    }

    @Test
    public void shouldParseLegacyTrackUrns() throws Exception {
        Urn urn = new Urn("soundcloud:sounds:123");
        assertThat(urn.isTrack()).isTrue();
        assertThat(urn.isPlayable()).isTrue();
        assertThat(urn.getNumericId()).isEqualTo(123L);
        assertThat(urn.toString()).isEqualTo("soundcloud:tracks:123");
    }

    @Test
    public void shouldParseTrackUrns() throws Exception {
        Urn urn = new Urn("soundcloud:tracks:123");
        assertThat(urn.isTrack()).isTrue();
        assertThat(urn.isPlayable()).isTrue();
        assertThat(urn.getNumericId()).isEqualTo(123L);
    }

    @Test
    public void shouldParsePlaylistUrns() throws Exception {
        Urn urn = new Urn("soundcloud:playlists:123");
        assertThat(urn.isPlaylist()).isTrue();
        assertThat(urn.isPlayable()).isTrue();
        assertThat(urn.getNumericId()).isEqualTo(123L);
    }

    @Test
    public void shouldParseLocalPlaylistUrns() throws Exception {
        Urn urn = new Urn("local:playlists:123");
        assertThat(urn.isPlaylist()).isTrue();
        assertThat(urn.isPlayable()).isTrue();
        assertThat(urn.getNumericId()).isEqualTo(123L);
    }

    // Eventually we shouldn't have to do it anymore, but this is how we currently represent local playlists
    @Test
    public void shouldParseNegativePlaylistUrns() throws Exception {
        Urn urn = new Urn("soundcloud:playlists:-123");
        assertThat(urn.isPlaylist()).isTrue();
        assertThat(urn.getNumericId()).isEqualTo(-123L);
    }

    @Test
    public void isLocalPlaylist() {
        Urn urn = Urn.newLocalPlaylist();
        assertThat(urn.isLocal()).isTrue();
    }

    @Test
    public void isLocalPlaylistShouldBeFalseWhenNotLocal() {
        Urn urn = Urn.forPlaylist(123L);
        assertThat(urn.isLocal()).isFalse();
    }

    @Test
    public void shouldParseAdUrns() throws Exception {
        final Urn urn = new Urn("dfp:ads:123-321");
        assertThat(urn.isAd()).isTrue();
        assertThat(urn.getNumericId()).isEqualTo(-1L);
    }

    // This is still up for debate, but right now we represent NOT_SET Urns with numeric part -1
    @Test
    public void shouldAllowUrnsWithNegativeOneIds() {
        final Urn urn = new Urn("soundcloud:tracks:-1");
        assertThat(urn.getNumericId()).isEqualTo(-1L);
    }

    @Test
    public void shouldParseUnknownUrnsForForwardsCompatibility() {
        Urn urn = new Urn("adswizz:ad:ABCDEF");
        assertThat(urn.getNumericId()).isEqualTo(-1L);
    }

    @Test
    public void shouldParseQueryUrnCorrectly() {
        Urn urn = new Urn("soundcloud:search-suggest:57322698561340628722353c71e20d86");

        assertThat(urn.getNumericId()).isEqualTo(-1L);
        assertThat(urn.toString()).isEqualTo("soundcloud:search-suggest:57322698561340628722353c71e20d86");
    }

    @Test
    public void shouldBuildTrackUrns() {
        final Urn urn = Urn.forTrack(1);
        assertThat(urn).isEqualTo(new Urn("soundcloud:tracks:1"));
    }

    @Test
    public void shouldBuildPlaylistUrns() {
        final Urn urn = Urn.forPlaylist(1);
        assertThat(urn).isEqualTo(new Urn("soundcloud:playlists:1"));
    }

    @Test
    public void shouldBuildUserUrns() {
        final Urn urn = Urn.forUser(1);
        assertThat(urn).isEqualTo(new Urn("soundcloud:users:1"));
    }

    @Test
    public void shouldBuildCommentUrns() {
        final Urn urn = Urn.forComment(1);
        assertThat(urn).isEqualTo(new Urn("soundcloud:comments:1"));
    }

    @Test
    public void shouldBuildStationUrns() {
        final Urn urn = Urn.forTrackStation(1);
        assertThat(urn).isEqualTo(new Urn("soundcloud:track-stations:1"));
    }

    @Test
    public void shouldBuildAdUrns() {
        final Urn urn = Urn.forAd("dfp", "123");
        assertThat(urn).isEqualTo(new Urn("dfp:ads:123"));
    }

    @Test
    public void isStationShouldMatchStationUrns() {
        Urn trackStationUrn = new Urn("soundcloud:track-stations:1");
        Urn curatorStationUrn = new Urn("soundcloud:curator-stations:1");
        Urn moodStationUrn = new Urn("soundcloud:mood-stations:happy");
        Urn artistStationUrn = new Urn("soundcloud:artist-stations:1");

        assertThat(trackStationUrn.isStation()).isTrue();
        assertThat(curatorStationUrn.isStation()).isTrue();
        assertThat(moodStationUrn.isStation()).isTrue();
        assertThat(artistStationUrn.isStation()).isTrue();
    }

    @Test
    public void isStationShouldNotMatchOtherUrns() {
        final Urn playlistUrn = Urn.forPlaylist(1);
        final Urn trackUrn = Urn.forTrack(1);

        assertThat(playlistUrn.isStation()).isFalse();
        assertThat(trackUrn.isStation()).isFalse();
    }

    @Test
    public void isAdShouldNotMatchOtherUrns() {
        final Urn playlistUrn = Urn.forPlaylist(1);
        final Urn trackUrn = Urn.forTrack(1);
        final Urn trackStationUrn = new Urn("soundcloud:track-stations:1");

        assertThat(playlistUrn.isAd()).isFalse();
        assertThat(trackUrn.isAd()).isFalse();
        assertThat(trackStationUrn.isAd()).isFalse();
        assertThat(Urn.NOT_SET.isAd()).isFalse();
    }

    @Test
    public void shouldBeParcelable() {
        Parcel parcel = Parcel.obtain();
        Urn urn = new Urn("soundcloud:tracks:1");
        urn.writeToParcel(parcel, 0);
        parcel.setDataPosition(0);

        Urn unparceled = Urn.CREATOR.createFromParcel(parcel);
        assertThat(unparceled).isEqualTo(urn);
    }

    @Test
    public void shouldBeComparableBasedOnAlphaNumericOrdering() {
        Urn a = new Urn("soundcloud:tracks:1");
        Urn b = new Urn("soundcloud:tracks:2");

        assertThat(a.compareTo(a)).isEqualTo(0);
        assertThat(a.compareTo(b)).isLessThan(0);
        assertThat(b.compareTo(a)).isGreaterThan(0);
    }

    @Test
    public void shouldImplementEqualsAndHashCode() throws Exception {
        EqualsVerifier.forClass(Urn.class)
                      .withOnlyTheseFields("content")
                      .verify();
    }
}
