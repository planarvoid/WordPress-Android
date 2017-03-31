package com.soundcloud.android.search;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.Navigator;
import com.soundcloud.android.R;
import com.soundcloud.android.analytics.performance.PerformanceMetricsEngine;
import com.soundcloud.android.configuration.experiments.MiniplayerExperiment;
import com.soundcloud.android.main.Screen;
import com.soundcloud.android.playback.PlaySessionSource;
import com.soundcloud.android.playback.PlaybackInitiator;
import com.soundcloud.android.playback.ui.view.PlaybackFeedbackHelper;
import com.soundcloud.android.playlists.PlaylistItem;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import com.soundcloud.android.tracks.TrackItem;
import com.soundcloud.java.optional.Optional;
import com.soundcloud.rx.eventbus.TestEventBus;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import rx.Observable;

import android.app.SearchManager;
import android.content.Intent;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class PlayFromVoiceSearchPresenterTest extends AndroidUnitTest {

    private static final String QUERY = "query";
    private static final String GENRE = "genre";

    private SearchResult searchResult;

    @Mock private AppCompatActivity activity;
    @Mock private SearchOperations searchOperations;
    @Mock private PlaybackInitiator playbackInitiator;
    @Mock private Random random;
    @Mock private Navigator navigator;
    @Mock private PlaybackFeedbackHelper playbackFeedbackHelper;
    @Mock private MiniplayerExperiment miniplayerExperiment;
    @Mock private PerformanceMetricsEngine performanceMetricsEngine;
    private TestEventBus eventBus;
    private PlayFromVoiceSearchPresenter presenter;

    @Before
    public void setUp() throws Exception {
        eventBus = new TestEventBus();
        when(activity.findViewById(R.id.progress)).thenReturn(new View(context()));
        presenter = new PlayFromVoiceSearchPresenter(searchOperations,
                                                     playbackInitiator,
                                                     random,
                                                     playbackFeedbackHelper,
                                                     navigator,
                                                     eventBus,
                                                     miniplayerExperiment,
                                                     performanceMetricsEngine);
    }

    @Test
    public void onResumeFinishesWithNoIntentAction() throws Exception {
        when(activity.getIntent()).thenReturn(new Intent());

        presenter.onCreate(activity, null);
        presenter.onResume(activity);

        verify(activity).finish();
    }

    @Test
    public void trackSearchErrorFallsBackToSearchActivity() throws Exception {
        Observable<SearchResult> searchObservable = Observable.error(new Throwable("search problem"));
        when(searchOperations.searchResult(QUERY,
                                           Optional.absent(),
                                           SearchType.TRACKS)).thenReturn(searchObservable);
        when(activity.getIntent()).thenReturn(getPlayFromSearchIntent(QUERY));

        presenter.onCreate(activity, null);
        presenter.onResume(activity);

        verify(navigator).performSearch(activity, QUERY);
    }

    @Test
    public void trackSearchErrorFallsBackToSearchActivityWithNoResults() throws Exception {
        searchResult = SearchResult.fromSearchableItems(new ArrayList(),
                                                        Optional.absent(),
                                                        Optional.absent());
        when(searchOperations.searchResult(QUERY,
                                           Optional.absent(),
                                           SearchType.TRACKS)).thenReturn(Observable.just(searchResult));
        when(activity.getIntent()).thenReturn(getPlayFromSearchIntent(QUERY));

        presenter.onCreate(activity, null);
        presenter.onResume(activity);

        verify(navigator).performSearch(activity, QUERY);
    }

    @Test
    public void callsPlayTrackWithSearchResult() throws Exception {
        final TrackItem apiTrack = ModelFixtures.trackItem();
        searchResult = SearchResult.fromSearchableItems(Collections.singletonList(apiTrack), Optional.absent(),
                                                        Optional.absent());
        when(searchOperations.searchResult(QUERY,
                                           Optional.absent(),
                                           SearchType.TRACKS)).thenReturn(Observable.just(searchResult));
        when(activity.getIntent()).thenReturn(getPlayFromSearchIntent(QUERY));

        presenter.onCreate(activity, null);
        presenter.onResume(activity);

        verify(playbackInitiator).playTrackWithRecommendationsLegacy(eq(apiTrack.getUrn()),
                                                                     any(PlaySessionSource.class));
    }

    @Test
    public void genreSearchErrorFallsBackToSearchActivity() throws Exception {
        Observable<SearchResult> searchObservable = Observable.error(new Throwable("search problem"));
        when(searchOperations.searchResult(GENRE,
                                           Optional.absent(),
                                           SearchType.PLAYLISTS)).thenReturn(searchObservable);
        when(activity.getIntent()).thenReturn(getPlayFromSearchWithGenreIntent(GENRE));

        presenter.onCreate(activity, null);
        presenter.onResume(activity);

        verify(navigator).performSearch(activity, GENRE);
    }

    @Test
    public void genreSearchErrorFallsBackToSearchActivityWithNoResults() throws Exception {
        Observable<SearchResult> searchObservable = Observable.error(new Throwable("search problem"));
        when(searchOperations.searchResult(GENRE,
                                           Optional.absent(),
                                           SearchType.PLAYLISTS)).thenReturn(searchObservable);
        when(activity.getIntent()).thenReturn(getPlayFromSearchWithGenreIntent(GENRE));

        presenter.onCreate(activity, null);
        presenter.onResume(activity);

        verify(navigator).performSearch(activity, GENRE);
    }

    @Test
    public void startsPlaylistActivityWithRandomPlaylistWithAutoplayIfGenreSearchReturnsAnyResults() throws Exception {
        final PlaylistItem playlistItem = ModelFixtures.playlistItem();

        List<PlaylistItem> playlistResults = Arrays.asList(ModelFixtures.playlistItem(), playlistItem);
        Observable<SearchResult> searchResultObservable = Observable.just(SearchResult.fromSearchableItems(
                playlistResults,
                Optional.absent(),
                Optional.absent()));

        when(searchOperations.searchResult(GENRE,
                                           Optional.absent(),
                                           SearchType.PLAYLISTS)).thenReturn(searchResultObservable);
        when(random.nextInt(2)).thenReturn(1);
        when(activity.getIntent()).thenReturn(getPlayFromSearchWithGenreIntent(GENRE));

        presenter.onCreate(activity, null);
        presenter.onResume(activity);

        verify(navigator).openPlaylistWithAutoPlay(activity, playlistItem.getUrn(), Screen.SEARCH_PLAYLIST_DISCO);
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
