package com.soundcloud.android.playback;

import static com.soundcloud.android.playback.PlaySessionSource.forChart;
import static com.soundcloud.android.playback.PlaySessionSource.forRecommendations;
import static com.soundcloud.android.playback.PlaybackContext.create;
import static org.assertj.core.api.Assertions.assertThat;

import com.soundcloud.android.analytics.SearchQuerySourceInfo;
import com.soundcloud.android.api.model.ChartType;
import com.soundcloud.android.discovery.charts.Chart;
import com.soundcloud.android.main.Screen;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.PlaybackContext.Bucket;
import com.soundcloud.java.optional.Optional;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class PlaybackContextTest {

    private final Screen ANY_SCREEN = Screen.UNKNOWN;

    @Test
    public void fromALink() {
        final PlaybackContext context = create(new PlaySessionSource(Screen.DEEPLINK));

        assertThat(context.bucket()).isEqualTo(PlaybackContext.Bucket.LINK);
        assertThat(context.query()).isEqualTo(Optional.absent());
        assertThat(context.urn()).isEqualTo(Optional.absent());
    }

    @Test
    public void fromAnUnknownScreen() {
        final PlaybackContext context = create(new PlaySessionSource(Screen.AUTH_USER_DETAILS));

        assertThat(context.bucket()).isEqualTo(PlaybackContext.Bucket.OTHER);
        assertThat(context.query()).isEqualTo(Optional.absent());
        assertThat(context.urn()).isEqualTo(Optional.absent());
    }

    @Test
    public void fromYourStream() {
        final PlaybackContext context = create(new PlaySessionSource(Screen.STREAM));

        assertThat(context.bucket()).isEqualTo(PlaybackContext.Bucket.STREAM);
        assertThat(context.query()).isEqualTo(Optional.absent());
        assertThat(context.urn()).isEqualTo(Optional.absent());
    }

    @Test
    public void fromProfile() {
        final Urn urn = Urn.forUser(123L);
        final Screen screen = ANY_SCREEN;

        final PlaybackContext context = create(PlaySessionSource.forArtist(screen, urn));

        assertThat(context.bucket()).isEqualTo(Bucket.PROFILE);
        assertThat(context.query()).isEqualTo(Optional.<String>absent());
        assertThat(context.urn()).isEqualTo(Optional.of(urn));
    }

    @Test
    public void fromPlaylist() {
        final Urn playlist = Urn.forPlaylist(123L);
        final Urn owner = Urn.forUser(456L);

        final PlaybackContext context = create(PlaySessionSource.forPlaylist(ANY_SCREEN, playlist, owner, 0));

        assertThat(context.bucket()).isEqualTo(Bucket.PLAYLIST);
        assertThat(context.query()).isEqualTo(Optional.<String>absent());
        assertThat(context.urn()).isEqualTo(Optional.of(playlist));
    }

    @Test
    public void fromTrackStationBasedOnTrackTitle() {
        final Urn trackStation = Urn.forTrackStation(123L);

        final PlaySessionSource playSessionSource = PlaySessionSource.forStation(ANY_SCREEN, trackStation);

        PlaybackContext context = create(playSessionSource);
        assertThat(context.bucket()).isEqualTo(Bucket.TRACK_STATION);
        assertThat(context.query()).isEqualTo(Optional.<String>absent());
        assertThat(context.urn()).isEqualTo(Optional.of(trackStation));
    }

    @Test
    public void fromArtistStationBasedOnCreatorName() {
        final Urn artistStation = Urn.forArtistStation(123L);

        final PlaySessionSource playSessionSource = PlaySessionSource.forStation(ANY_SCREEN, artistStation);

        PlaybackContext context = create(playSessionSource);
        assertThat(context.bucket()).isEqualTo(Bucket.ARTIST_STATION);
        assertThat(context.query()).isEqualTo(Optional.<String>absent());
        assertThat(context.urn()).isEqualTo(Optional.of(artistStation));
    }

    @Test
    public void fromYourLikes() {
        assertFromYourLikes(Screen.LIKES);
        assertFromYourLikes(Screen.YOUR_LIKES);
    }

    private void assertFromYourLikes(Screen screen) {
        final PlaySessionSource playSessionSource = new PlaySessionSource(screen);
        final PlaybackContext context = create(playSessionSource);

        assertThat(context.bucket()).isEqualTo(Bucket.YOUR_LIKES);
        assertThat(context.query()).isEqualTo(Optional.absent());
        assertThat(context.urn()).isEqualTo(Optional.absent());
    }

    @Test
    public void fromListeningHistory() {
        assertListeningHistory(Screen.PLAY_HISTORY);
        assertListeningHistory(Screen.COLLECTIONS);
    }

    private void assertListeningHistory(Screen playHistory) {
        final PlaySessionSource playSessionSource = PlaySessionSource.forHistory(playHistory);
        final PlaybackContext context = create(playSessionSource);

        assertThat(context.bucket()).isEqualTo(Bucket.LISTENING_HISTORY);
        assertThat(context.query()).isEqualTo(Optional.absent());
        assertThat(context.urn()).isEqualTo(Optional.absent());
    }

    @Test
    public void fromSearchResultsForQuery() {
        assertSearchResultContext(Screen.SEARCH_EVERYTHING);
        assertSearchResultContext(Screen.SEARCH_TRACKS);
        assertSearchResultContext(Screen.SEARCH_PREMIUM_CONTENT);
    }

    private void assertSearchResultContext(Screen screen) {
        final Urn queryUrn = new Urn("soundcloud:query:453asdf");
        final String queryString = "Michael Jackson";
        final SearchQuerySourceInfo searchQuerySourceInfo = new SearchQuerySourceInfo(queryUrn, queryString);

        final PlaySessionSource playSessionSource = new PlaySessionSource(screen);
        playSessionSource.setSearchQuerySourceInfo(searchQuerySourceInfo);

        final PlaybackContext context = create(playSessionSource);

        assertThat(context.bucket()).isEqualTo(Bucket.SEARCH_RESULT);
        assertThat(context.query()).isEqualTo(Optional.of(queryString));
        assertThat(context.urn()).isEqualTo(Optional.absent());
    }

    @Test
    public void fromSuggestedTracks() {
        assertFromSuggestedTracks(Screen.SEARCH_SUGGESTIONS);
        assertFromSuggestedTracks(Screen.SEARCH_MAIN);
    }

    private void assertFromSuggestedTracks(Screen searchSuggestions) {
        final Urn queryUrn = new Urn("soundcloud:query:453asdf");
        final PlaySessionSource playSessionSource = forRecommendations(searchSuggestions, 0, queryUrn);

        final PlaybackContext context = create(playSessionSource);

        assertThat(context.bucket()).isEqualTo(Bucket.SUGGESTED_TRACKS);
        assertThat(context.query()).isEqualTo(Optional.absent());
        assertThat(context.urn()).isEqualTo(Optional.absent());
    }

    @Test
    public void fromNewAndHotCharts() {
        final Urn queryUrn = new Urn("soundcloud:query:453asdf");
        final Urn genre = Chart.GLOBAL_GENRE;

        assertFromCharts(queryUrn, genre, ChartType.TRENDING, Bucket.CHARTS_TRENDING);
    }

    @Test
    public void fromGenreNewAndHotCharts() {
        final Urn queryUrn = new Urn("soundcloud:query:453asdf");
        final Urn genre = new Urn("soundcloud:genres:all-music:some_genre");

        assertFromCharts(queryUrn, genre, ChartType.TRENDING, Bucket.CHARTS_TRENDING);
    }

    @Test
    public void fromTop50Charts() {
        final Urn queryUrn = new Urn("soundcloud:query:453asdf");
        final Urn genre = Chart.GLOBAL_GENRE;

        assertFromCharts(queryUrn, genre, ChartType.TOP, Bucket.CHARTS_TOP);
    }

    @Test
    public void fromGenreTop50Chart() {
        final Urn queryUrn = new Urn("soundcloud:query:453asdf");
        final Urn genre = new Urn("soundcloud:genres:all-music:some_genre");

        assertFromCharts(queryUrn, genre, ChartType.TOP, Bucket.CHARTS_TOP);
    }


    private void assertFromCharts(Urn queryUrn,
                                  Urn genre,
                                  ChartType chartType,
                                  Bucket charts) {
        final PlaySessionSource playSessionSource = forChart(ANY_SCREEN.get(), 0, queryUrn, chartType, genre);

        final PlaybackContext context = create(playSessionSource);

        assertThat(context.bucket()).isEqualTo(charts);
        assertThat(context.query()).isEqualTo(Optional.absent());
        assertThat(context.urn()).isEqualTo(Optional.of(genre));
    }

}
