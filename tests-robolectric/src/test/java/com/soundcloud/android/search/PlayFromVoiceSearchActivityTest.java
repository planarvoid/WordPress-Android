package com.soundcloud.android.search;

import static com.soundcloud.android.Expect.expect;
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
import com.soundcloud.android.playback.PlaybackOperations;
import com.soundcloud.android.playlists.PlaylistDetailActivity;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import com.soundcloud.java.optional.Optional;
import com.xtremelabs.robolectric.Robolectric;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import rx.Observable;

import android.app.SearchManager;
import android.content.Intent;
import android.provider.MediaStore;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

@RunWith(SoundCloudTestRunner.class)
public class PlayFromVoiceSearchActivityTest {

    private static final String QUERY = "query";
    private static final String GENRE = "genre";

    private PlayFromVoiceSearchActivity activity;
    private SearchResult searchResult;

    @Mock private SearchOperations searchOperations;
    @Mock private PlaybackOperations playbackOperations;
    @Mock private Random random;

    @Before
    public void setUp() throws Exception {
        activity = new PlayFromVoiceSearchActivity(searchOperations, playbackOperations, random);
        activity.onCreate(null);
    }

    @Test
    public void onResumeFinishesWithNoIntentAction() throws Exception {
        activity.setIntent(new Intent());
        activity.onResume();
        expect(activity.isFinishing()).toBeTrue();
    }

    @Test
    public void trackSearchErrorFallsBackToSearchActivity() throws Exception {
        activity.setIntent(getPlayFromSearchIntent(QUERY));

        Observable<SearchResult> searchObservable = Observable.error(new Throwable("search problem"));
        when(searchOperations.searchResult(QUERY, SearchOperations.TYPE_TRACKS)).thenReturn(searchObservable);

        activity.onResume();

        Intent searchIntent = Robolectric.shadowOf(Robolectric.application).getNextStartedActivity();
        expect(searchIntent.getAction()).toEqual(Actions.PERFORM_SEARCH);
        expect(searchIntent.getStringExtra(SearchManager.QUERY)).toEqual(QUERY);
    }

    @Test
    public void trackSearchErrorFallsBackToSearchActivityWithNoResults() throws Exception {
        activity.setIntent(getPlayFromSearchIntent(QUERY));
        searchResult = new SearchResult(new ArrayList(), Optional.<Link>absent(), Optional.<Urn>absent());

        when(searchOperations.searchResult(QUERY, SearchOperations.TYPE_TRACKS)).thenReturn(Observable.just(searchResult));

        activity.onResume();

        Intent searchIntent = Robolectric.shadowOf(Robolectric.application).getNextStartedActivity();
        expect(searchIntent.getAction()).toEqual(Actions.PERFORM_SEARCH);
        expect(searchIntent.getStringExtra(SearchManager.QUERY)).toEqual(QUERY);
    }

    @Test
    public void callsPlayTrackWithSearchResult() throws Exception {
        final ApiTrack apiTrack = ModelFixtures.create(ApiTrack.class);
        searchResult = new SearchResult(Arrays.asList(apiTrack), Optional.<Link>absent(), Optional.<Urn>absent());

        activity.setIntent(getPlayFromSearchIntent(QUERY));
        when(searchOperations.searchResult(QUERY, SearchOperations.TYPE_TRACKS)).thenReturn(Observable.just(searchResult));

        activity.onResume();

        verify(playbackOperations).playTrackWithRecommendationsLegacy(eq(apiTrack.getUrn()), any(PlaySessionSource.class));
    }

    @Test
    public void genreSearchErrorFallsBackToSearchActivity() throws Exception {
        activity.setIntent(getPlayFromSearchWithGenreIntent(GENRE));

        Observable<SearchResult> searchObservable = Observable.error(new Throwable("search problem"));
        when(searchOperations.searchResult(GENRE, SearchOperations.TYPE_PLAYLISTS)).thenReturn(searchObservable);

        activity.onResume();

        Intent searchIntent = Robolectric.shadowOf(Robolectric.application).getNextStartedActivity();
        expect(searchIntent.getAction()).toEqual(Actions.PERFORM_SEARCH);
        expect(searchIntent.getStringExtra(SearchManager.QUERY)).toEqual(GENRE);
    }

    @Test
    public void genreSearchErrorFallsBackToSearchActivityWithNoResults() throws Exception {
        activity.setIntent(getPlayFromSearchWithGenreIntent(GENRE));

        Observable<SearchResult> searchObservable = Observable.error(new Throwable("search problem"));
        when(searchOperations.searchResult(GENRE, SearchOperations.TYPE_PLAYLISTS)).thenReturn(searchObservable);

        activity.onResume();

        Intent searchIntent = Robolectric.shadowOf(Robolectric.application).getNextStartedActivity();
        expect(searchIntent.getAction()).toEqual(Actions.PERFORM_SEARCH);
        expect(searchIntent.getStringExtra(SearchManager.QUERY)).toEqual(GENRE);
    }

    @Test
    public void startsPlaylistActivityWithRandomPlaylistWithAutoplayIfGenreSearchReturnsAnyResults() throws Exception {
        activity.setIntent(getPlayFromSearchWithGenreIntent(GENRE));

        final ApiPlaylist apiPlaylist = ModelFixtures.create(ApiPlaylist.class);

        List<ApiPlaylist> playlistResults = Arrays.asList(ModelFixtures.create(ApiPlaylist.class), apiPlaylist);
        Observable<SearchResult> searchResultObservable = Observable.just(new SearchResult(playlistResults, Optional.<Link>absent(), Optional.<Urn>absent()));

        when(searchOperations.searchResult(GENRE, SearchOperations.TYPE_PLAYLISTS)).thenReturn(searchResultObservable);
        when(random.nextInt(2)).thenReturn(1);

        activity.onResume();

        Intent searchIntent = Robolectric.shadowOf(Robolectric.application).getNextStartedActivity();
        expect(searchIntent.getAction()).toEqual(Actions.PLAYLIST);
        expect(searchIntent.getParcelableExtra(PlaylistDetailActivity.EXTRA_URN)).toEqual(apiPlaylist.getUrn());
        expect(searchIntent.getBooleanExtra("autoplay", false)).toBeTrue();
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