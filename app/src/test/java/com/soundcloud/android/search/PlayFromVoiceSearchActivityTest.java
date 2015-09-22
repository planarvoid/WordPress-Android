package com.soundcloud.android.search;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.Actions;
import com.soundcloud.android.api.model.ApiPlaylist;
import com.soundcloud.android.api.model.ApiTrack;
import com.soundcloud.android.api.model.Link;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.PlaySessionSource;
import com.soundcloud.android.playback.PlaybackInitiator;
import com.soundcloud.android.playlists.PlaylistDetailActivity;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import com.soundcloud.java.optional.Optional;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.robolectric.Robolectric;
import org.robolectric.Shadows;
import org.robolectric.util.ActivityController;
import rx.Observable;

import android.app.SearchManager;
import android.content.Intent;
import android.provider.MediaStore;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

public class PlayFromVoiceSearchActivityTest extends AndroidUnitTest {
    
    private static final String QUERY = "query";
    private static final String GENRE = "genre";

    private SearchResult searchResult;

    @Mock private SearchOperations searchOperations;
    @Mock private PlaybackInitiator playbackInitiator;
    @Mock private Random random;
    private ActivityController<PlayFromVoiceSearchActivity> controller;

    @Before
    public void setUp() throws Exception {
        final PlayFromVoiceSearchActivity activity = new PlayFromVoiceSearchActivity(searchOperations, playbackInitiator, random);
        controller = ActivityController.of(Robolectric.getShadowsAdapter(), activity);
    }

    @Test
    public void onResumeFinishesWithNoIntentAction() throws Exception {
        controller.withIntent(new Intent()).create().resume();

        assertThat(controller.get().isFinishing()).isTrue();
    }

    @Test
    public void trackSearchErrorFallsBackToSearchActivity() throws Exception {
        Observable<SearchResult> searchObservable = Observable.error(new Throwable("search problem"));
        when(searchOperations.searchResult(QUERY, SearchOperations.TYPE_TRACKS)).thenReturn(searchObservable);

        controller.withIntent(getPlayFromSearchIntent(QUERY)).create().resume();

        Intent searchIntent = Shadows.shadowOf(context()).getShadowApplication().getNextStartedActivity();
        assertThat(searchIntent.getAction()).isEqualTo(Actions.PERFORM_SEARCH);
        assertThat(searchIntent.getStringExtra(SearchManager.QUERY)).isEqualTo(QUERY);
    }

    @Test
    public void trackSearchErrorFallsBackToSearchActivityWithNoResults() throws Exception {
        searchResult = new SearchResult(new ArrayList(), Optional.<Link>absent(), Optional.<Urn>absent());
        when(searchOperations.searchResult(QUERY, SearchOperations.TYPE_TRACKS)).thenReturn(Observable.just(searchResult));

        controller.withIntent(getPlayFromSearchIntent(QUERY)).create().resume();

        Intent searchIntent = Shadows.shadowOf(context()).getShadowApplication().getNextStartedActivity();
        assertThat(searchIntent.getAction()).isEqualTo(Actions.PERFORM_SEARCH);
        assertThat(searchIntent.getStringExtra(SearchManager.QUERY)).isEqualTo(QUERY);
    }

    @Test
    public void callsPlayTrackWithSearchResult() throws Exception {
        final ApiTrack apiTrack = ModelFixtures.create(ApiTrack.class);
        searchResult = new SearchResult(Arrays.asList(apiTrack), Optional.<Link>absent(), Optional.<Urn>absent());
        when(searchOperations.searchResult(QUERY, SearchOperations.TYPE_TRACKS)).thenReturn(Observable.just(searchResult));

        controller.withIntent(getPlayFromSearchIntent(QUERY)).create().resume();

        verify(playbackInitiator).playTrackWithRecommendationsLegacy(eq(apiTrack.getUrn()), any(PlaySessionSource.class));
    }

    @Test
    public void genreSearchErrorFallsBackToSearchActivity() throws Exception {
        Observable<SearchResult> searchObservable = Observable.error(new Throwable("search problem"));
        when(searchOperations.searchResult(GENRE, SearchOperations.TYPE_PLAYLISTS)).thenReturn(searchObservable);

        controller.withIntent(getPlayFromSearchWithGenreIntent(GENRE)).create().resume();

        Intent searchIntent = Shadows.shadowOf(context()).getShadowApplication().getNextStartedActivity();
        assertThat(searchIntent.getAction()).isEqualTo(Actions.PERFORM_SEARCH);
        assertThat(searchIntent.getStringExtra(SearchManager.QUERY)).isEqualTo(GENRE);
    }

    @Test
    public void genreSearchErrorFallsBackToSearchActivityWithNoResults() throws Exception {
        Observable<SearchResult> searchObservable = Observable.error(new Throwable("search problem"));
        when(searchOperations.searchResult(GENRE, SearchOperations.TYPE_PLAYLISTS)).thenReturn(searchObservable);

        controller.withIntent(getPlayFromSearchWithGenreIntent(GENRE)).create().resume();

        Intent searchIntent = Shadows.shadowOf(context()).getShadowApplication().getNextStartedActivity();
        assertThat(searchIntent.getAction()).isEqualTo(Actions.PERFORM_SEARCH);
        assertThat(searchIntent.getStringExtra(SearchManager.QUERY)).isEqualTo(GENRE);
    }

    @Test
    public void startsPlaylistActivityWithRandomPlaylistWithAutoplayIfGenreSearchReturnsAnyResults() throws Exception {
        final ApiPlaylist apiPlaylist = ModelFixtures.create(ApiPlaylist.class);

        List<ApiPlaylist> playlistResults = Arrays.asList(ModelFixtures.create(ApiPlaylist.class), apiPlaylist);
        Observable<SearchResult> searchResultObservable = Observable.just(new SearchResult(playlistResults, Optional.<Link>absent(), Optional.<Urn>absent()));

        when(searchOperations.searchResult(GENRE, SearchOperations.TYPE_PLAYLISTS)).thenReturn(searchResultObservable);
        when(random.nextInt(2)).thenReturn(1);

        controller.withIntent(getPlayFromSearchWithGenreIntent(GENRE)).create().resume();

        Intent searchIntent = Shadows.shadowOf(context()).getShadowApplication().getNextStartedActivity();
        assertThat(searchIntent.getAction()).isEqualTo(Actions.PLAYLIST);
        assertThat(searchIntent.getParcelableExtra(PlaylistDetailActivity.EXTRA_URN)).isEqualTo(apiPlaylist.getUrn());
        assertThat(searchIntent.getBooleanExtra("autoplay", false)).isTrue();
    }

    private Intent getPlayFromSearchIntent(String query) {
        final Intent intent = new Intent(MediaStore.INTENT_ACTION_MEDIA_PLAY_FROM_SEARCH);
        intent.putExtra(SearchManager.QUERY, query);
        return intent;
    }

    private Intent getPlayFromSearchWithGenreIntent(String genre) {
        final Intent intent = new Intent(MediaStore.INTENT_ACTION_MEDIA_PLAY_FROM_SEARCH);
        intent.putExtra(SearchManager.QUERY, genre);
        intent.putExtra(MediaStore.EXTRA_MEDIA_FOCUS, MediaStore.Audio.Genres.ENTRY_CONTENT_TYPE);
        intent.putExtra("android.intent.extra.genre", genre);
        return intent;
    }
}