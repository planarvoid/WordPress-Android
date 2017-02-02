package com.soundcloud.android.search.topresults;

import static com.soundcloud.android.search.topresults.TopResultsBucketViewModel.Kind.TOP_RESULT;
import static com.soundcloud.android.view.AsyncViewModel.fromIdle;
import static com.soundcloud.android.view.AsyncViewModel.fromRefreshing;
import static com.soundcloud.java.optional.Optional.of;
import static java.util.Collections.emptyList;
import static org.mockito.Mockito.when;

import com.soundcloud.android.api.ApiRequestException;
import com.soundcloud.android.api.model.ApiPlaylist;
import com.soundcloud.android.api.model.ApiTrack;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playlists.PlaylistItem;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import com.soundcloud.android.tracks.TrackItem;
import com.soundcloud.android.view.AsyncViewModel;
import com.soundcloud.android.view.ViewError;
import com.soundcloud.java.collections.Pair;
import com.soundcloud.java.optional.Optional;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import rx.Observable;
import rx.observers.AssertableSubscriber;
import rx.subjects.PublishSubject;

import android.support.annotation.NonNull;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;

@SuppressWarnings("unchecked")
public class TopResultsPresenterTest extends AndroidUnitTest {

    private static final String QUERY = "query";
    private TopResultsPresenter presenter;

    @Mock private TopResultsLoader loader;

    private final ApiTrack apiTrack = ModelFixtures.apiTrack();
    private final ApiPlaylist apiPlaylist = ModelFixtures.apiPlaylist();

    private TopResultsViewModel topResultsViewModel;
    private TopResultsViewModel updatedViewModel;

    private PublishSubject<Pair<String, Optional<Urn>>> searchIntent = PublishSubject.create();
    private PublishSubject<Void> refreshIntent = PublishSubject.create();

    private TopResultsPresenter.TopResultsView topResultsView = new TopResultsPresenter.TopResultsView() {
        @Override
        public Observable<Pair<String, Optional<Urn>>> searchIntent() {
            return searchIntent;
        }

        @Override
        public Observable<Void> refreshIntent() {
            return refreshIntent;
        }
    };;

    @Before
    public void setUp() throws Exception {
        presenter = new TopResultsPresenter(loader);

        presenter.attachView(topResultsView);

        topResultsViewModel = TopResultsViewModel.create(Collections.singletonList(getBucketViewModel(TOP_RESULT, 1, SearchItem.Track.create(getTrackItem(apiTrack)))));
        updatedViewModel = TopResultsViewModel.create(Collections.singletonList(getBucketViewModel(TOP_RESULT, 1, SearchItem.Playlist.create(getPlaylistItem(apiPlaylist)))));

        when(loader.getTopSearchResults(Pair.of(QUERY, of(TopResultsFixtures.QUERY_URN)))).thenReturn(Observable.just(topResultsViewModel), Observable.just(updatedViewModel));
    }

    @Test
    public void emitsViewModelOnSearch() throws Exception {
        searchIntent.onNext(Pair.of(QUERY, of(TopResultsFixtures.QUERY_URN)));

        presenter.viewModel().test().assertValues(
                fromIdle(topResultsViewModel)
        );
    }

    @Test
    public void viewModelEmitsNothingAfterDisconnecting() throws Exception {
        when(loader.getTopSearchResults(Pair.of(QUERY, of(TopResultsFixtures.QUERY_URN)))).thenReturn(Observable.just(topResultsViewModel));

        presenter.detachView();

        searchIntent.onNext(Pair.of(QUERY, of(TopResultsFixtures.QUERY_URN)));

        presenter.viewModel().test().assertValue(fromIdle(TopResultsViewModel.create(emptyList())));
    }

    @Test
    public void viewModelEmitsUpdatedTracksAfterRefresh() throws Exception {
        searchIntent.onNext(Pair.of(QUERY, of(TopResultsFixtures.QUERY_URN)));

        AssertableSubscriber<AsyncViewModel<TopResultsViewModel>> testSubscriber = presenter.viewModel().test();

        testSubscriber.assertValuesAndClear(
                fromIdle(topResultsViewModel)
        );

        refreshIntent.onNext(null);

        testSubscriber.assertValues(
                fromRefreshing(topResultsViewModel),
                fromIdle(updatedViewModel)
        );
    }

    @Test
    public void viewModelEmitsError() throws Exception {
        when(loader.getTopSearchResults(Pair.of(QUERY, of(TopResultsFixtures.QUERY_URN)))).thenReturn(Observable.error(ApiRequestException.networkError(null, new IOException())));

        searchIntent.onNext(Pair.of(QUERY, of(TopResultsFixtures.QUERY_URN)));

        presenter.viewModel().test().assertValue(
                AsyncViewModel.create(of(TopResultsViewModel.create(emptyList())), false, of(ViewError.CONNECTION_ERROR))
        );
    }

    @Test
    public void viewModelEmitsServerError() throws Exception {
        when(loader.getTopSearchResults(Pair.of(QUERY, of(TopResultsFixtures.QUERY_URN)))).thenReturn(Observable.error(ApiRequestException.serverError(null, null)));

        searchIntent.onNext(Pair.of(QUERY, of(TopResultsFixtures.QUERY_URN)));

        presenter.viewModel().test().assertValue(
                AsyncViewModel.create(of(TopResultsViewModel.create(emptyList())), false, of(ViewError.SERVER_ERROR))
        );
    }

    @NonNull
    private TopResultsBucketViewModel getBucketViewModel(TopResultsBucketViewModel.Kind bucketKind, int totalResults, SearchItem... searchItems) {
        return TopResultsBucketViewModel.create(Arrays.asList(searchItems), bucketKind, totalResults, TopResultsFixtures.QUERY_URN);
    }

    private TrackItem getTrackItem(ApiTrack apiTrack) {
        return getTrackItem(apiTrack, false);
    }

    private TrackItem getTrackItem(ApiTrack apiTrack, boolean isLiked) {
        return TrackItem.fromLiked(apiTrack, isLiked);
    }

    private PlaylistItem getPlaylistItem(ApiPlaylist apiPlaylist) {
        return getPlaylistItem(apiPlaylist, false);
    }

    private PlaylistItem getPlaylistItem(ApiPlaylist apiPlaylist, boolean isLiked) {
        return PlaylistItem.fromLiked(apiPlaylist, isLiked);
    }

}