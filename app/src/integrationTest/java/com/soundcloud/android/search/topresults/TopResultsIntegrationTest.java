package com.soundcloud.android.search.topresults;

import static com.soundcloud.android.hamcrest.MatcherAssertEventually.assertThatEventually;
import static com.soundcloud.java.collections.Iterables.find;
import static com.soundcloud.java.collections.Iterables.size;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;

import com.soundcloud.android.BaseIntegrationTest;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.analytics.SearchQuerySourceInfo;
import com.soundcloud.android.api.ApiEndpoints;
import com.soundcloud.android.api.ApiRequestException;
import com.soundcloud.android.events.EventContextMetadata;
import com.soundcloud.android.events.Module;
import com.soundcloud.android.events.UIEvent;
import com.soundcloud.android.framework.TestUser;
import com.soundcloud.android.hamcrest.TestAsyncState;
import com.soundcloud.android.main.Screen;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.navigation.NavigationTarget;
import com.soundcloud.android.payments.UpsellContext;
import com.soundcloud.android.playback.PlaybackResult;
import com.soundcloud.android.utils.Supplier;
import com.soundcloud.android.utils.collection.AsyncLoaderState;
import com.soundcloud.android.view.ViewError;
import com.soundcloud.java.optional.Optional;
import io.reactivex.Observable;
import io.reactivex.subjects.PublishSubject;
import org.junit.Test;
import org.junit.runner.RunWith;

import android.support.test.filters.SmallTest;
import android.support.test.runner.AndroidJUnit4;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@RunWith(AndroidJUnit4.class)
@SmallTest
public class TopResultsIntegrationTest extends BaseIntegrationTest {

    private static final String SEARCH_TOP_RESULTS = "search-top-results.json";
    private static final String ENTER_SPEC_REF = "specs/search/top-results-enter.spec";
    private static final String USER_CLICK_SPEC_REF = "specs/search/top-results-user-click.spec";
    private static final String PLAYLIST_CLICK_SPEC_REF = "specs/search/top-results-playlist-click.spec";
    private static final String ALBUM_CLICK_SPEC_REF = "specs/search/top-results-album-click.spec";
    private static final String TRACK_CLICK_SPEC_REF = "specs/search/top-results-track-click.spec";
    private static final String HELP_CLICK_SPEC_REF = "specs/search/top-results-help-click.spec";

    private static final Urn QUERY_URN = new Urn("soundcloud:search:db00142ff19f435098f9a5972879c165");
    private static final String SEARCH_QUERY = "query";

    public TopResultsIntegrationTest() {
        super(TestUser.testUser);
    }

    @Test
    public void presenterDoesNotEmitWhenNotConnected() {
        noNetwork();

        final TopResultsPresenter presenter = createPresenter();
        final TestView testView = new TestView(presenter);

        testView.assertState(empty());
    }

    @Test
    public void presenterStartsWithEmptyModel() {
        unrespondingNetwork();

        final TopResultsPresenter presenter = createPresenter();
        final TestView testView = new TestView(presenter);

        testView.searchPublishSubject.onNext(SearchParams.create(SEARCH_QUERY, SEARCH_QUERY, Optional.absent(), Optional.absent()));

        testView.assertState(contains(AsyncLoaderState.Companion.<TopResultsViewModel, ViewError>loadingNextPage()));
    }

    @Test
    public void presenterShowsNetworkError() {
        noNetwork();

        final TopResultsPresenter presenter = createPresenter();
        final TestView testView = new TestView(presenter);

        testView.searchPublishSubject.onNext(SearchParams.create(SEARCH_QUERY, SEARCH_QUERY, Optional.absent(), Optional.absent()));

        testView.assertState(contains(AsyncLoaderState.Companion.<TopResultsViewModel, ViewError>loadingNextPage(),
                                      AsyncLoaderState.Companion.<TopResultsViewModel, ViewError>firstPageError(ApiRequestException.networkError(null, new IOException()))));
    }

    @Test
    public void presenterShowsSearchResults() {
        addMockedResponse(ApiEndpoints.SEARCH_TOP_RESULTS.path(), SEARCH_TOP_RESULTS);

        final TopResultsPresenter presenter = createPresenter();
        final TestView testView = new TestView(presenter);

        testView.searchPublishSubject.onNext(SearchParams.create(SEARCH_QUERY, SEARCH_QUERY, Optional.absent(), Optional.absent()));

        testView.assertLastState(this::hasData, equalTo(true));
        testView.assertLastState(this::topResultsBucketSize, equalTo(1));
        testView.assertLastState(this::tracksBucketSize, equalTo(4));
        testView.assertLastState(this::usersBucketSize, equalTo(4));
        testView.assertLastState(this::albumsBucketSize, equalTo(2));
        testView.assertLastState(this::playlistsBucketSize, equalTo(3));
    }

    @Test
    public void presenterTracksEnterEvent() throws Exception {
        addMockedResponse(ApiEndpoints.SEARCH_TOP_RESULTS.path(), SEARCH_TOP_RESULTS);

        final TopResultsPresenter presenter = createPresenter();
        final TestView testView = new TestView(presenter);

        mrLocalLocal.startEventTracking();

        testView.enterPublishSubject.onNext(UiAction.Enter.create(System.currentTimeMillis(), SEARCH_QUERY));
        testView.searchPublishSubject.onNext(SearchParams.create(SEARCH_QUERY, SEARCH_QUERY, Optional.absent(), Optional.absent()));
        testView.assertLastState(this::hasData, equalTo(true));

        mrLocalLocal.verify(ENTER_SPEC_REF);
    }

    @Test
    public void trackClickTracksAndReturnsResult() throws Exception {
        addMockedResponse(ApiEndpoints.SEARCH_TOP_RESULTS.path(), SEARCH_TOP_RESULTS);

        mrLocalLocal.startEventTracking();

        final TopResultsPresenter presenter = createPresenter();
        final TestView testView = new TestView(presenter);

        testView.enterPublishSubject.onNext(UiAction.Enter.create(System.currentTimeMillis(), SEARCH_QUERY));
        testView.searchPublishSubject.onNext(SearchParams.create(SEARCH_QUERY, SEARCH_QUERY, Optional.absent(), Optional.absent()));
        testView.assertLastState(this::hasData, equalTo(true));

        AsyncLoaderState<TopResultsViewModel, ViewError> lastState = testView.lastState();
        SearchItem.Track trackSearchItem = (SearchItem.Track) lastState.getData().get().buckets().get(1).items().get(1);
        testView.trackClickPublishSubject.onNext(trackSearchItem.clickAction());

        mrLocalLocal.verify(TRACK_CLICK_SPEC_REF);
        testView.assertPlaybackResult(PlaybackResult.success());
    }

    @Test
    public void userClickTracksAndGoesToUser() throws Exception {
        addMockedResponse(ApiEndpoints.SEARCH_TOP_RESULTS.path(), SEARCH_TOP_RESULTS);

        mrLocalLocal.startEventTracking();

        final TopResultsPresenter presenter = createPresenter();
        final TestView testView = new TestView(presenter);

        testView.enterPublishSubject.onNext(UiAction.Enter.create(System.currentTimeMillis(), SEARCH_QUERY));
        testView.searchPublishSubject.onNext(SearchParams.create(SEARCH_QUERY, SEARCH_QUERY, Optional.absent(), Optional.absent()));
        testView.assertLastState(this::hasData, equalTo(true));

        AsyncLoaderState<TopResultsViewModel, ViewError> lastState = testView.lastState();
        SearchItem.User userSearchItem = (SearchItem.User) lastState.getData().get().buckets().get(2).items().get(2);
        testView.userClickPublishSubject.onNext(userSearchItem.clickAction());

        mrLocalLocal.verify(USER_CLICK_SPEC_REF);

        Urn userUrn = userSearchItem.itemUrn().get();
        testView.assertNavigationTarget(NavigationTarget.forProfile(userUrn,
                                                                    Optional.of(navigationUiEvent(userUrn, Screen.SEARCH_USERS.get(), Module.create(Module.SEARCH_PEOPLE, 2))),
                                                                    Optional.of(Screen.SEARCH_EVERYTHING),
                                                                    Optional.of(new SearchQuerySourceInfo(QUERY_URN, 7, userUrn, SEARCH_QUERY))));
    }

    @Test
    public void playlistClickTracksAndGoesToPlaylist() throws Exception {
        addMockedResponse(ApiEndpoints.SEARCH_TOP_RESULTS.path(), SEARCH_TOP_RESULTS);

        mrLocalLocal.startEventTracking();

        final TopResultsPresenter presenter = createPresenter();
        final TestView testView = new TestView(presenter);

        testView.enterPublishSubject.onNext(UiAction.Enter.create(System.currentTimeMillis(), SEARCH_QUERY));
        testView.searchPublishSubject.onNext(SearchParams.create(SEARCH_QUERY, SEARCH_QUERY, Optional.absent(), Optional.absent()));
        testView.assertLastState(this::hasData, equalTo(true));

        AsyncLoaderState<TopResultsViewModel, ViewError> lastState = testView.lastState();
        SearchItem.Playlist playlistSearchItem = (SearchItem.Playlist) lastState.getData().get().buckets().get(4).items().get(2);
        testView.playlistClickPublishSubject.onNext(playlistSearchItem.clickAction());

        Urn playlistUrn = playlistSearchItem.itemUrn().get();

        mrLocalLocal.verify(PLAYLIST_CLICK_SPEC_REF);

        testView.assertNavigationTarget(NavigationTarget.forPlaylist(playlistUrn,
                                                                     Screen.SEARCH_EVERYTHING,
                                                                     Optional.of(new SearchQuerySourceInfo(QUERY_URN, 13, playlistUrn, SEARCH_QUERY)),
                                                                     Optional.absent(),
                                                                     Optional.of(navigationUiEvent(playlistUrn, Screen.SEARCH_PLAYLISTS.get(), Module.create(Module.SEARCH_PLAYLISTS, 2)))));
    }

    @Test
    public void albumClickTracksAndGoesToPlaylist() throws Exception {
        addMockedResponse(ApiEndpoints.SEARCH_TOP_RESULTS.path(), SEARCH_TOP_RESULTS);

        mrLocalLocal.startEventTracking();

        final TopResultsPresenter presenter = createPresenter();
        final TestView testView = new TestView(presenter);

        testView.enterPublishSubject.onNext(UiAction.Enter.create(System.currentTimeMillis(), SEARCH_QUERY));
        testView.searchPublishSubject.onNext(SearchParams.create(SEARCH_QUERY, SEARCH_QUERY, Optional.absent(), Optional.absent()));
        testView.assertLastState(this::hasData, equalTo(true));

        AsyncLoaderState<TopResultsViewModel, ViewError> lastState = testView.lastState();
        SearchItem.Playlist playlistSearchItem = (SearchItem.Playlist) lastState.getData().get().buckets().get(3).items().get(1);
        testView.playlistClickPublishSubject.onNext(playlistSearchItem.clickAction());

        mrLocalLocal.verify(ALBUM_CLICK_SPEC_REF);
        Urn itemUrn = playlistSearchItem.itemUrn().get();
        testView.assertNavigationTarget(NavigationTarget.forPlaylist(itemUrn,
                                                                     Screen.SEARCH_EVERYTHING,
                                                                     Optional.of(new SearchQuerySourceInfo(QUERY_URN, 10, itemUrn, SEARCH_QUERY)),
                                                                     Optional.absent(),
                                                                     Optional.of(navigationUiEvent(itemUrn, Screen.SEARCH_ALBUMS.get(), Module.create(Module.SEARCH_ALBUMS, 1)))));
    }

    @Test
    public void viewAllClickGoesToViewAllBucket() throws Exception {
        addMockedResponse(ApiEndpoints.SEARCH_TOP_RESULTS.path(), SEARCH_TOP_RESULTS);

        final TopResultsPresenter presenter = createPresenter();
        final TestView testView = new TestView(presenter);

        testView.enterPublishSubject.onNext(UiAction.Enter.create(System.currentTimeMillis(), SEARCH_QUERY));
        testView.searchPublishSubject.onNext(SearchParams.create(SEARCH_QUERY, SEARCH_QUERY, Optional.absent(), Optional.absent()));
        testView.assertLastState(this::hasData, equalTo(true));

        AsyncLoaderState<TopResultsViewModel, ViewError> lastState = testView.lastState();
        TopResultsBucketViewModel bucketViewModel = lastState.getData().get().buckets().get(3);
        testView.viewAllClickPublishSubject.onNext(bucketViewModel.viewAllAction().get());

        testView.assertNavigationTarget(NavigationTarget.forSearchViewAll(Optional.of(QUERY_URN),
                                                                          SEARCH_QUERY,
                                                                          TopResultsBucketViewModel.Kind.ALBUMS,
                                                                          false));
    }

    @Test
    public void helpClickTracksAndGoesToHelpScreen() throws Exception {
        addMockedResponse(ApiEndpoints.SEARCH_TOP_RESULTS.path(), SEARCH_TOP_RESULTS);

        mrLocalLocal.startEventTracking();

        final TopResultsPresenter presenter = createPresenter();
        final TestView testView = new TestView(presenter);

        testView.enterPublishSubject.onNext(UiAction.Enter.create(System.currentTimeMillis(), SEARCH_QUERY));
        testView.searchPublishSubject.onNext(SearchParams.create(SEARCH_QUERY, SEARCH_QUERY, Optional.absent(), Optional.absent()));
        testView.assertLastState(this::hasData, equalTo(true));

        testView.helpClickPublishSubject.onNext(UiAction.HelpClick.create());

        AsyncLoaderState<TopResultsViewModel, ViewError> lastState = testView.lastState();
        TopResultsBucketViewModel bucketViewModel = lastState.getData().get().buckets().get(3);
        testView.viewAllClickPublishSubject.onNext(bucketViewModel.viewAllAction().get());

        mrLocalLocal.verify(HELP_CLICK_SPEC_REF);

        testView.assertOpenUpgradeUpsellContext(UpsellContext.PREMIUM_CONTENT);
    }

    private boolean hasData(AsyncLoaderState<TopResultsViewModel, ViewError> topResultsViewModelAsyncLoaderState) {
        return topResultsViewModelAsyncLoaderState.getData().isPresent();
    }

    private int topResultsBucketSize(AsyncLoaderState<TopResultsViewModel, ViewError> topResultsViewModelAsyncLoaderState) {
        return getBucketSize(topResultsViewModelAsyncLoaderState, TopResultsBucketViewModel.Kind.TOP_RESULT);
    }

    private int tracksBucketSize(AsyncLoaderState<TopResultsViewModel, ViewError> topResultsViewModelAsyncLoaderState) {
        return getBucketSize(topResultsViewModelAsyncLoaderState, TopResultsBucketViewModel.Kind.TRACKS);
    }

    private int usersBucketSize(AsyncLoaderState<TopResultsViewModel, ViewError> topResultsViewModelAsyncLoaderState) {
        return getBucketSize(topResultsViewModelAsyncLoaderState, TopResultsBucketViewModel.Kind.USERS);
    }

    private int playlistsBucketSize(AsyncLoaderState<TopResultsViewModel, ViewError> topResultsViewModelAsyncLoaderState) {
        return getBucketSize(topResultsViewModelAsyncLoaderState, TopResultsBucketViewModel.Kind.PLAYLISTS);
    }

    private int albumsBucketSize(AsyncLoaderState<TopResultsViewModel, ViewError> topResultsViewModelAsyncLoaderState) {
        return getBucketSize(topResultsViewModelAsyncLoaderState, TopResultsBucketViewModel.Kind.ALBUMS);
    }

    private int getBucketSize(AsyncLoaderState<TopResultsViewModel, ViewError> topResultsViewModelAsyncLoaderState, TopResultsBucketViewModel.Kind kind) {
        return size(find(topResultsViewModelAsyncLoaderState.getData().get().buckets(), input -> kind.equals(input.kind())).items());
    }

    private TopResultsPresenter createPresenter() {
            return SoundCloudApplication.getObjectGraph().topResultsPresenter();
    }

    private UIEvent navigationUiEvent(Urn urn, String screen, Module module) {
        return UIEvent.fromNavigation(urn, EventContextMetadata.builder().pageName(screen).module(module).build());
    }

    static class TestView extends TestAsyncState<AsyncLoaderState<TopResultsViewModel, ViewError>> implements TopResultsPresenter.TopResultsView {

        final List<AsyncLoaderState<TopResultsViewModel, ViewError>> models = new ArrayList<>();

        private PublishSubject<SearchParams> searchPublishSubject = PublishSubject.create();
        private PublishSubject<SearchParams> refreshPublishSubject = PublishSubject.create();
        private PublishSubject<UiAction.Enter> enterPublishSubject = PublishSubject.create();
        private PublishSubject<UiAction.TrackClick> trackClickPublishSubject = PublishSubject.create();
        private PublishSubject<UiAction.PlaylistClick> playlistClickPublishSubject = PublishSubject.create();
        private PublishSubject<UiAction.UserClick> userClickPublishSubject = PublishSubject.create();
        private PublishSubject<UiAction.ViewAllClick> viewAllClickPublishSubject = PublishSubject.create();
        private PublishSubject<UiAction.HelpClick> helpClickPublishSubject = PublishSubject.create();
        
        private PlaybackResult lastPlaybackResult;
        private NavigationTarget lastNavigationTarget;
        private UpsellContext lastOpenUpgradeUpsellContext;

        public TestView(TopResultsPresenter presenter) {
            presenter.attachView(this);
        }

        @Override
        public Supplier<List<AsyncLoaderState<TopResultsViewModel, ViewError>>> states() {
            return () -> models;
        }

        @Override
        public Observable<UiAction.Enter> enterScreen() {
            return enterPublishSubject;
        }

        @Override
        public Observable<UiAction.TrackClick> trackClick() {
            return trackClickPublishSubject;
        }

        @Override
        public Observable<UiAction.PlaylistClick> playlistClick() {
            return playlistClickPublishSubject;
        }

        @Override
        public Observable<UiAction.UserClick> userClick() {
            return userClickPublishSubject;
        }

        @Override
        public Observable<UiAction.ViewAllClick> viewAllClick() {
            return viewAllClickPublishSubject;
        }

        @Override
        public Observable<UiAction.HelpClick> helpClick() {
            return helpClickPublishSubject;
        }

        @Override
        public void showPlaybackResult(PlaybackResult playbackResult) {
            lastPlaybackResult = playbackResult;
        }

        @Override
        public void navigateTo(NavigationTarget navigationTarget) {
            lastNavigationTarget = navigationTarget;
        }

        @Override
        public void openUpgrade(UpsellContext upsellContext) {
            lastOpenUpgradeUpsellContext = upsellContext;
        }

        @Override
        public Observable<SearchParams> initialLoadSignal() {
            return searchPublishSubject;
        }

        @Override
        public Observable<SearchParams> refreshSignal() {
            return refreshPublishSubject;
        }

        @Override
        public Observable<ViewError> actionPerformedSignal() {
            return Observable.empty();
        }

        private void assertPlaybackResult(PlaybackResult playbackResult) {
            assertThatEventually(() -> lastPlaybackResult, equalTo(playbackResult));
        }

        private void assertNavigationTarget(NavigationTarget navigationTarget) {
            assertThatEventually(() -> lastNavigationTarget, equalTo(navigationTarget));
        }

        private void assertOpenUpgradeUpsellContext(UpsellContext upsellContext) {
            assertThatEventually(() -> lastOpenUpgradeUpsellContext, equalTo(upsellContext));
        }

        @Override
        public void accept(AsyncLoaderState<TopResultsViewModel, ViewError> newState) throws Exception {
            models.add(newState);
        }
    }
}
