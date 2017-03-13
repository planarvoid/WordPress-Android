package com.soundcloud.android.playback;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;

import com.soundcloud.android.Consts;
import com.soundcloud.android.analytics.PromotedSourceInfo;
import com.soundcloud.android.analytics.SearchQuerySourceInfo;
import com.soundcloud.android.main.Screen;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.java.optional.Optional;
import com.soundcloud.java.strings.Strings;
import org.junit.Test;
import org.mockito.Mock;

import android.content.SharedPreferences;
import android.os.Parcel;

import java.util.Arrays;

public class PlaySessionSourceTest extends AndroidUnitTest {
    private static final String ORIGIN_PAGE = "origin:page";
    private static final String EXPLORE_VERSION = "1.0";
    private static final Urn PLAYLIST_URN = Urn.forPlaylist(123);
    private static final Urn USER_URN = Urn.forUser(2);
    private static final int TRACK_COUNT = 5;

    @Mock SharedPreferences sharedPreferences;

    @Test
    public void shouldCreateEmptyPlaySessionSource() throws Exception {
        PlaySessionSource playSessionSource = PlaySessionSource.EMPTY;
        assertThat(playSessionSource.getOriginScreen()).isEqualTo(Strings.EMPTY);
        assertThat(playSessionSource.getCollectionUrn()).isEqualTo(Urn.NOT_SET);
        assertThat(playSessionSource.getCollectionOwnerUrn()).isEqualTo(Urn.NOT_SET);
        assertThat(playSessionSource.getCollectionSize()).isEqualTo(Consts.NOT_SET);
        assertThat(playSessionSource.getInitialSource()).isEqualTo(Strings.EMPTY);
        assertThat(playSessionSource.getSearchQuerySourceInfo()).isNull();
    }

    @Test
    public void shouldCreatePlaySessionSourceFromOriginPage() throws Exception {
        PlaySessionSource playSessionSource = new PlaySessionSource(ORIGIN_PAGE);
        assertThat(playSessionSource.getOriginScreen()).isEqualTo(ORIGIN_PAGE);
        assertThat(playSessionSource.getCollectionUrn()).isEqualTo(Urn.NOT_SET);
        assertThat(playSessionSource.getCollectionOwnerUrn()).isEqualTo(Urn.NOT_SET);
        assertThat(playSessionSource.getCollectionSize()).isEqualTo(Consts.NOT_SET);
        assertThat(playSessionSource.getInitialSource()).isEqualTo(Strings.EMPTY);
    }

    @Test
    public void shouldCreatePlaySessionSourceFromOriginPageAndSetId() throws Exception {
        PlaySessionSource playSessionSource = PlaySessionSource.forPlaylist(ORIGIN_PAGE,
                                                                            PLAYLIST_URN,
                                                                            USER_URN,
                                                                            TRACK_COUNT);

        assertThat(playSessionSource.getOriginScreen()).isEqualTo(ORIGIN_PAGE);
        assertThat(playSessionSource.getCollectionUrn()).isEqualTo(PLAYLIST_URN);
        assertThat(playSessionSource.getCollectionOwnerUrn()).isEqualTo(USER_URN);
        assertThat(playSessionSource.getCollectionSize()).isEqualTo(TRACK_COUNT);
        assertThat(playSessionSource.getInitialSource()).isEqualTo(Strings.EMPTY);
    }

    @Test
    public void shouldCreatePlaySessionSourceFromOriginPageTrackSourceInfoAndSetId() throws Exception {
        PlaySessionSource playSessionSource = PlaySessionSource.forPlaylist(ORIGIN_PAGE,
                                                                            PLAYLIST_URN,
                                                                            USER_URN,
                                                                            TRACK_COUNT);

        assertThat(playSessionSource.getOriginScreen()).isEqualTo(ORIGIN_PAGE);
        assertThat(playSessionSource.getCollectionUrn()).isEqualTo(PLAYLIST_URN);
        assertThat(playSessionSource.getCollectionOwnerUrn()).isEqualTo(USER_URN);
        assertThat(playSessionSource.getCollectionSize()).isEqualTo(TRACK_COUNT);
        assertThat(playSessionSource.getInitialSource()).isEmpty();
    }

    @Test
    public void shouldCreateQuerySourceInfoFromTrackSourceInfo() throws Exception {
        SearchQuerySourceInfo searchQuerySourceInfo = new SearchQuerySourceInfo(new Urn("soundcloud:search:urn"),
                                                                                "query");
        PlaySessionSource playSessionSource = new PlaySessionSource(Screen.SEARCH_EVERYTHING);
        playSessionSource.setSearchQuerySourceInfo(searchQuerySourceInfo);

        assertThat(playSessionSource.getSearchQuerySourceInfo()).isEqualTo(searchQuerySourceInfo);
        assertThat(playSessionSource.isFromSearchQuery()).isEqualTo(true);
    }

    @Test
    public void isFromStationsShouldReturnsFalseWhenTheCollectionUrnIsNotAStation() {
        assertThat(new PlaySessionSource(Screen.ACTIVITIES).isFromStations()).isFalse();
    }

    @Test
    public void isFromStationsShouldReturnsTrueWhenTheCollectionUrnIsAStation() {
        assertThat(PlaySessionSource.forStation(Screen.SEARCH_MAIN, Urn.forTrackStation(123L))
                                    .isFromStations()).isTrue();
    }

    @Test
    public void playlistSessionSourceShouldBeParcelable() {
        SearchQuerySourceInfo searchQuerySourceInfo = new SearchQuerySourceInfo(new Urn("soundcloud:search:urn"),
                                                                                "query");
        PromotedSourceInfo promotedSourceInfo = new PromotedSourceInfo("ad:urn:123",
                                                                       Urn.forTrack(123L),
                                                                       Optional.absent(),
                                                                       Arrays.asList("url"));
        PlaySessionSource original = PlaySessionSource.forPlaylist(ORIGIN_PAGE, PLAYLIST_URN, USER_URN, TRACK_COUNT);
        original.setSearchQuerySourceInfo(searchQuerySourceInfo);
        original.setPromotedSourceInfo(promotedSourceInfo);

        Parcel parcel = Parcel.obtain();
        original.writeToParcel(parcel, 0);
        parcel.setDataPosition(0);

        PlaySessionSource copy = new PlaySessionSource(parcel);
        assertThat(copy.getOriginScreen()).isEqualTo(ORIGIN_PAGE);
        assertThat(copy.getCollectionUrn()).isEqualTo(PLAYLIST_URN);
        assertThat(copy.getCollectionOwnerUrn()).isEqualTo(USER_URN);
        assertThat(copy.getCollectionSize()).isEqualTo(TRACK_COUNT);
        assertThat(copy.getInitialSource()).isEmpty();
        assertThat(copy.getSearchQuerySourceInfo()).isEqualTo(searchQuerySourceInfo);
        assertThat(copy.getPromotedSourceInfo()).isEqualTo(promotedSourceInfo);
    }

    @Test
    public void discoverySourceShouldBeParcelable() {
        final PlaySessionSource source = PlaySessionSource.forHistory(Screen.PLAY_HISTORY);

        Parcel parcel = Parcel.obtain();
        source.writeToParcel(parcel, 0);
        parcel.setDataPosition(0);

        PlaySessionSource copy = new PlaySessionSource(parcel);
        assertThat(copy.getDiscoverySource()).isEqualTo(DiscoverySource.HISTORY);
    }

    @Test
    public void shouldParcelAbsentMetadata() {
        PlaySessionSource original = PlaySessionSource.forPlaylist(ORIGIN_PAGE, PLAYLIST_URN, USER_URN, TRACK_COUNT);

        Parcel parcel = Parcel.obtain();
        original.writeToParcel(parcel, 0);

        PlaySessionSource copy = new PlaySessionSource(parcel);
        assertThat(copy.isFromPromotedItem()).isFalse();
        assertThat(copy.isFromSearchQuery()).isFalse();
    }

    @Test
    public void createsPlaySessionSourceFromPreferences() {
        when(sharedPreferences.getString(eq(PlaySessionSource.PREF_KEY_ORIGIN_SCREEN_TAG), anyString())).thenReturn(
                "screen");
        when(sharedPreferences.getString(eq(PlaySessionSource.PREF_KEY_COLLECTION_URN), anyString())).thenReturn(
                "soundcloud:tracks:123");
        when(sharedPreferences.getString(eq(PlaySessionSource.PREF_KEY_COLLECTION_OWNER_URN), anyString())).thenReturn(
                "soundcloud:users:123");
        when(sharedPreferences.getInt(eq(PlaySessionSource.PREF_KEY_COLLECTION_SIZE), anyInt())).thenReturn(2);

        PlaySessionSource playSessionSource = new PlaySessionSource(sharedPreferences);

        assertThat(playSessionSource.getOriginScreen()).isEqualTo("screen");
        assertThat(playSessionSource.getCollectionUrn()).isEqualTo(Urn.forTrack(123));
        assertThat(playSessionSource.getCollectionOwnerUrn()).isEqualTo(Urn.forUser(123));
        assertThat(playSessionSource.getCollectionSize()).isEqualTo(2);
    }

    @Test
    public void createsPlaySessionSourceForStation() {
        final Urn station = Urn.forTrackStation(123);
        final PlaySessionSource playSessionSource = PlaySessionSource.forStation(Screen.COLLECTIONS, station);

        assertThat(playSessionSource.getDiscoverySource()).isEqualTo(DiscoverySource.STATIONS);
        assertThat(playSessionSource.getCollectionUrn()).isEqualTo(station);
    }

    @Test
    public void createsPlaySessionSourceForStationWithDiscoverySource() {
        final Urn station = Urn.forTrackStation(123);
        final PlaySessionSource playSessionSource = PlaySessionSource.forStation("screen",
                                                                                 station,
                                                                                 DiscoverySource.STATIONS_SUGGESTIONS);

        assertThat(playSessionSource.getDiscoverySource()).isEqualTo(DiscoverySource.STATIONS_SUGGESTIONS);
        assertThat(playSessionSource.getCollectionUrn()).isEqualTo(station);
    }

    @Test
    public void createsPlaySessionSourceForPlayHistory() {
        final PlaySessionSource playSessionSource = PlaySessionSource.forHistory(Screen.PLAY_HISTORY);

        assertThat(playSessionSource.getOriginScreen()).isEqualTo(Screen.PLAY_HISTORY.get());
        assertThat(playSessionSource.getDiscoverySource()).isEqualTo(DiscoverySource.HISTORY);
    }

    @Test
    public void getInitialSourceFallsbackToDiscoverySourceIfPresent() {
        final PlaySessionSource playSessionSource = PlaySessionSource.forHistory(Screen.PLAY_HISTORY);

        assertThat(playSessionSource.getInitialSource()).isEqualTo(DiscoverySource.HISTORY.value());
    }

    @Test
    public void isFromPlaylistHistory() {
        assertThat(PlaySessionSource.forHistory(Screen.COLLECTIONS).isFromPlaylistHistory()).isTrue();
    }
}
