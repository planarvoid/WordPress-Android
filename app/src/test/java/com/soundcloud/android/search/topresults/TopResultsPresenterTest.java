package com.soundcloud.android.search.topresults;

import static com.soundcloud.android.events.SearchEvent.ClickSource.TRACKS_BUCKET;
import static com.soundcloud.android.helpers.NavigationTargetMatcher.matchesNavigationTarget;
import static com.soundcloud.android.search.topresults.TopResultsFixtures.searchPlaylistItem;
import static com.soundcloud.android.search.topresults.TopResultsFixtures.searchTrackItem;
import static com.soundcloud.android.search.topresults.TopResultsFixtures.searchUserItem;
import static com.soundcloud.java.optional.Optional.absent;
import static com.soundcloud.java.optional.Optional.of;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.collect.Lists;
import com.soundcloud.android.analytics.EventTracker;
import com.soundcloud.android.analytics.SearchQuerySourceInfo;
import com.soundcloud.android.analytics.TrackingStateProvider;
import com.soundcloud.android.events.Module;
import com.soundcloud.android.events.ReferringEvent;
import com.soundcloud.android.events.ScreenEvent;
import com.soundcloud.android.events.SearchEvent;
import com.soundcloud.android.main.Screen;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.navigation.NavigationExecutor;
import com.soundcloud.android.navigation.NavigationTarget;
import com.soundcloud.android.navigation.Navigator;
import com.soundcloud.android.playback.PlaySessionStateProvider;
import com.soundcloud.android.playback.PlaybackInitiator;
import com.soundcloud.android.playback.PlaybackResult;
import com.soundcloud.android.playback.TrackSourceInfo;
import com.soundcloud.android.playback.ui.view.PlaybackFeedbackHelper;
import com.soundcloud.android.playlists.PlaylistItem;
import com.soundcloud.android.search.SearchPlayQueueFilter;
import com.soundcloud.android.search.SearchTracker;
import com.soundcloud.android.search.topresults.TopResults.Bucket.Kind;
import com.soundcloud.android.sync.SyncFailedException;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import com.soundcloud.android.tracks.TrackItem;
import com.soundcloud.android.users.UserItem;
import com.soundcloud.java.optional.Optional;
import com.soundcloud.java.strings.Strings;
import edu.emory.mathcs.backport.java.util.Arrays;
import io.reactivex.Observable;
import io.reactivex.Scheduler;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.subjects.PublishSubject;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InOrder;
import org.mockito.Mock;

import android.app.Activity;

import java.util.List;

@SuppressWarnings("unchecked")
public class TopResultsPresenterTest extends AndroidUnitTest {

    private static final String QUERY = "query";
    private static final Urn QUERY_URN = new Urn("queryUrn");
    private static final int BUCKET_POSITION = 0;
    private static final int QUERY_POSITION = 1;

    private PublishSubject<UiAction.TrackClick> trackClick;
    private PublishSubject<UiAction.PlaylistClick> playlistClick;
    private PublishSubject<UiAction.UserClick> userClick;
    private PublishSubject<UiAction.ViewAllClick> viewAllClick;
    private PublishSubject<UiAction.HelpClick> helpClick;

    private TopResultsPresenter presenter;
    @Mock private TopResultsOperations operations;

    @Mock private TopResultsPresenter.TopResultsView topResultsView;
    @Mock private SearchPlayQueueFilter playQueueFilter;
    @Mock private PlaybackInitiator playbackInitiator;
    @Mock private Navigator navigator;
    @Mock private NavigationExecutor navigationExecutor;
    @Mock private EventTracker eventTracker;
    @Mock private SearchTracker searchTracker;
    @Mock private TrackingStateProvider trackingStateProvider;
    @Mock private PlaySessionStateProvider playSessionStateProvider;
    @Mock private PlaybackFeedbackHelper playbackFeedbackHelper;
    @Mock private SearchClickListener searchClickListener;
    @Captor private ArgumentCaptor<SearchEvent> searchEventCaptor;
    @Captor private ArgumentCaptor<GoToItemArgs> itemClickCaptor;
    @Captor private ArgumentCaptor<TopResultsViewAllArgs> viewAllArgsCaptor;
    @Captor private ArgumentCaptor<ViewModel> viewModelCaptor;
    @Captor private ArgumentCaptor<ClickResultAction> actionCaptor;
    @Captor private ArgumentCaptor<SearchClickListener.ClickParams> clickParamsArgumentCaptor;
    @Captor private ArgumentCaptor<SearchClickListener.TrackClickParams> trackClickParamsArgumentCaptor;
    private final Scheduler scheduler = Schedulers.trampoline();

    private final TrackItem track = ModelFixtures.trackItem();

    private final UserItem apiUser = ModelFixtures.userItem();
    private final PlaylistItem apiPlaylist = ModelFixtures.playlistItem();

    private final UiAction.Search search = UiAction.Search.create(SearchParams.create(QUERY, QUERY, of(TopResultsFixtures.QUERY_URN), of(QUERY_POSITION)));
    private final UiAction.Refresh refreshingSearch = UiAction.Refresh.create(SearchParams.create(QUERY, QUERY, of(TopResultsFixtures.QUERY_URN), of(QUERY_POSITION)));
    private final PublishSubject<UiAction.Search> searchIntent = PublishSubject.create();
    private final PublishSubject<UiAction.Refresh> refreshIntent = PublishSubject.create();
    private final PublishSubject<UiAction.Enter> enterScreen = PublishSubject.create();

    @Before
    public void setUp() throws Exception {
        trackClick = PublishSubject.create();
        playlistClick = PublishSubject.create();
        userClick = PublishSubject.create();
        viewAllClick = PublishSubject.create();
        helpClick = PublishSubject.create();

        presenter = new TopResultsPresenter(eventTracker,
                                            searchTracker,
                                            trackingStateProvider,
                                            operations,
                                            scheduler,
                                            navigator,
                                            navigationExecutor,
                                            playbackFeedbackHelper,
                                            searchClickListener);

        when(topResultsView.trackClick()).thenReturn(trackClick);
        when(topResultsView.playlistClick()).thenReturn(playlistClick);
        when(topResultsView.userClick()).thenReturn(userClick);
        when(topResultsView.viewAllClick()).thenReturn(viewAllClick);
        when(topResultsView.helpClick()).thenReturn(helpClick);

        when(topResultsView.searchIntent()).thenReturn(searchIntent);
        when(topResultsView.refreshIntent()).thenReturn(refreshIntent);
        when(topResultsView.enterScreen()).thenReturn(enterScreen);
        when(trackingStateProvider.getLastEvent()).thenReturn(absent());
        when(searchClickListener.trackClickToPlaybackResult(any(SearchClickListener.TrackClickParams.class))).thenReturn(Observable.empty());

        doNothing().when(topResultsView).accept(viewModelCaptor.capture());
        doNothing().when(topResultsView).handleActionResult(actionCaptor.capture());
    }

    @Test
    public void emitsViewModelLoadingAndResultOnSearch() throws Exception {
        final TopResults topResults = initTopResults();
        final SearchResult.Loading loadingState = SearchResult.Loading.create(false);
        final SearchResult searchResultState = SearchResult.Data.create(topResults);
        initSearch(loadingState, searchResultState);

        presenter.attachView(topResultsView);

        searchIntent.onNext(search);

        final InOrder inOrder = inOrder(topResultsView);
        inOrder.verify(topResultsView).accept(ViewModel.empty());
        inOrder.verify(topResultsView).accept(ViewModel.empty().with(loadingState));
        inOrder.verify(topResultsView).accept(ViewModel.empty().with(searchResultState));
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void emitsViewModelRefreshingAndResultOnPullToRefresh() throws Exception {
        final TopResults topResults = initTopResults();
        final SearchResult.Loading loadingState = SearchResult.Loading.create(true);
        final SearchResult searchResultState = SearchResult.Data.create(topResults);
        initSearch(loadingState, searchResultState);

        presenter.attachView(topResultsView);

        refreshIntent.onNext(refreshingSearch);

        final InOrder inOrder = inOrder(topResultsView);
        inOrder.verify(topResultsView).accept(ViewModel.empty());
        inOrder.verify(topResultsView).accept(ViewModel.empty().with(loadingState));
        inOrder.verify(topResultsView).accept(ViewModel.empty().with(searchResultState));
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void emitsViewModelLoadingAndErrorOnSearchError() throws Exception {
        final SearchResult.Loading loadingState = SearchResult.Loading.create(false);
        final SearchResult errorState = SearchResult.Error.create(new SyncFailedException());
        initSearch(loadingState, errorState);

        presenter.attachView(topResultsView);

        searchIntent.onNext(search);

        final InOrder inOrder = inOrder(topResultsView);
        inOrder.verify(topResultsView).accept(ViewModel.empty());
        inOrder.verify(topResultsView).accept(ViewModel.empty().with(loadingState));
        inOrder.verify(topResultsView).accept(ViewModel.empty().with(errorState));
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void playsTrackOnTrackItemClickAndDoesNotUpdateViewWhenPlaybackSuccessful() throws Exception {
        final TrackItem trackItem0 = ModelFixtures.trackItem();
        final TrackItem trackItem2 = ModelFixtures.trackItem();
        initTopResultsSearch(DomainSearchItem.track(trackItem0), DomainSearchItem.track(track), DomainSearchItem.track(trackItem2));
        final SearchItem.Track searchItem = SearchItem.Track.create(this.track, BUCKET_POSITION, getTrackSourceInfo(TRACKS_BUCKET));

        when(searchClickListener.trackClickToPlaybackResult(trackClickParamsArgumentCaptor.capture())).thenReturn(Observable.just(PlaybackResult.success()));

        trackClick.onNext(UiAction.TrackClick.create(QUERY, searchItem));

        final SearchClickListener.TrackClickParams trackClickParams = trackClickParamsArgumentCaptor.getValue();
        assertThat(trackClickParams.clickParams().screen()).isEqualTo(Screen.SEARCH_EVERYTHING);
        assertThat(trackClickParams.clickParams().urn()).isEqualTo(track.getUrn());
        assertThat(trackClickParams.clickParams().clickSource()).isEqualTo(SearchEvent.ClickSource.TRACKS_BUCKET);
        assertThat(trackClickParams.clickParams().position()).isEqualTo(1);
        assertThat(trackClickParams.clickParams().module()).isEqualTo(Module.create("search:tracks", 1));
        assertThat(trackClickParams.clickParams().queryUrn()).isEqualTo(of(QUERY_URN));
        assertThat(trackClickParams.clickParams().searchQuery()).isEqualTo(QUERY);
        assertThat(trackClickParams.playableItems()).isEqualTo(Lists.newArrayList(trackItem0.getUrn(), track.getUrn(), trackItem2.getUrn()));

        verify(topResultsView, never()).handleActionResult(any(ClickResultAction.class));
    }

    @Test
    public void showErrorOnPlaybackError() throws Exception {
        initTopResultsSearch(DomainSearchItem.track(track));
        final SearchItem.Track searchItem = SearchItem.Track.create(this.track, BUCKET_POSITION, getTrackSourceInfo(TRACKS_BUCKET));

        when(searchClickListener.trackClickToPlaybackResult(trackClickParamsArgumentCaptor.capture())).thenReturn(Observable.just(PlaybackResult.error(PlaybackResult.ErrorReason.UNSKIPPABLE)));

        trackClick.onNext(UiAction.TrackClick.create(QUERY, searchItem));

        verify(topResultsView).handleActionResult(actionCaptor.capture());

        actionCaptor.getValue().run(mock(Activity.class));
        verify(playbackFeedbackHelper).showFeedbackOnPlaybackError(PlaybackResult.ErrorReason.UNSKIPPABLE);
    }

    @Test
    public void goesToPlaylistOnPlaylistItemClick() throws Exception {
        final PlaylistItem playlistItem = ModelFixtures.playlistItem();
        final DomainSearchItem playlist = searchPlaylistItem(playlistItem);
        final SearchItem.Playlist searchPlaylist = SearchItem.Playlist.create(playlistItem, BUCKET_POSITION, SearchEvent.ClickSource.PLAYLISTS_BUCKET);

        final TopResults.Bucket apiTopResultsBucket = TopResultsFixtures.playlistResultsBucket(playlist);
        initTopResultsSearch(apiTopResultsBucket);

        final ClickResultAction action = mock(ClickResultAction.class);
        when(searchClickListener.playlistClickToNavigateAction(clickParamsArgumentCaptor.capture())).thenReturn(action);

        playlistClick.onNext(UiAction.PlaylistClick.create(QUERY, searchPlaylist));

        final SearchClickListener.ClickParams value = clickParamsArgumentCaptor.getValue();
        assertThat(value.screen()).isEqualTo(Screen.SEARCH_EVERYTHING);
        assertThat(value.urn()).isEqualTo(playlistItem.getUrn());
        assertThat(value.clickSource()).isEqualTo(SearchEvent.ClickSource.PLAYLISTS_BUCKET);
        assertThat(value.position()).isEqualTo(0);
        assertThat(value.module()).isEqualTo(Module.create("search:playlists", 0));
        assertThat(value.queryUrn()).isEqualTo(of(QUERY_URN));
        assertThat(value.searchQuery()).isEqualTo(QUERY);

        verify(topResultsView).handleActionResult(action);
    }

    @Test
    public void goesToProfileOnUserItemClick() throws Exception {
        final UserItem userItem = ModelFixtures.userItem();
        final DomainSearchItem user = searchUserItem(userItem);
        final SearchItem.User searchUser = SearchItem.User.create(userItem, BUCKET_POSITION, SearchEvent.ClickSource.PEOPLE_BUCKET);

        final TopResults.Bucket apiTopResultsBucket = TopResultsFixtures.peopleResultsBucket(user);
        initTopResultsSearch(apiTopResultsBucket);

        final ClickResultAction action = mock(ClickResultAction.class);
        when(searchClickListener.userClickToNavigateAction(clickParamsArgumentCaptor.capture())).thenReturn(action);

        userClick.onNext(UiAction.UserClick.create(QUERY, searchUser));

        final SearchClickListener.ClickParams value = clickParamsArgumentCaptor.getValue();
        assertThat(value.screen()).isEqualTo(Screen.SEARCH_EVERYTHING);
        assertThat(value.urn()).isEqualTo(userItem.getUrn());
        assertThat(value.clickSource()).isEqualTo(SearchEvent.ClickSource.PEOPLE_BUCKET);
        assertThat(value.position()).isEqualTo(0);
        assertThat(value.module()).isEqualTo(Module.create("search:people", 0));
        assertThat(value.queryUrn()).isEqualTo(of(QUERY_URN));
        assertThat(value.searchQuery()).isEqualTo(QUERY);

        verify(topResultsView).handleActionResult(action);
    }

    @Test
    public void handleViewAllClick() throws Exception {
        initTopResultsSearch(DomainSearchItem.track(track));

        viewAllClick.onNext(UiAction.ViewAllClick.create(QUERY, Kind.TRACKS));

        verify(topResultsView).handleActionResult(actionCaptor.capture());

        final Activity context = mock(Activity.class);
        actionCaptor.getValue().run(context);
        verify(navigator).navigateTo(eq(context), argThat(matchesNavigationTarget(NavigationTarget.forSearchViewAll(of(QUERY_URN), QUERY, Kind.TRACKS, false))));
    }

    @Test
    public void trackEnterScreenEvent() throws Exception {
        initTopResultsSearch(TopResultsFixtures.trackResultsBucket(searchUserItem(ModelFixtures.userItem())));
        final Optional<ReferringEvent> referringEvent = of(ReferringEvent.create("123A", "kind"));
        when(trackingStateProvider.getLastEvent()).thenReturn(referringEvent);

        enterScreen.onNext(UiAction.Enter.create(123L, QUERY));

        verify(eventTracker).trackScreen(eq(ScreenEvent.create(Screen.SEARCH_EVERYTHING.get(), new SearchQuerySourceInfo(QUERY_URN, QUERY))), eq(referringEvent));

    }

    private void initTopResultsSearch(DomainSearchItem... tracks) {
        final TopResults.Bucket bucket = TopResults.Bucket.create(Kind.TRACKS, 10, Lists.newArrayList(tracks));
        final TopResults topResults = TopResults.create(1, of(QUERY_URN), Lists.newArrayList(bucket));
        initSearch(SearchResult.Data.create(topResults));
        presenter.attachView(topResultsView);
        searchIntent.onNext(search);
    }

    private void initTopResultsSearch(TopResults.Bucket... buckets) {
        final TopResults topResults = TopResults.create(1, of(QUERY_URN), Arrays.asList(buckets));
        initSearch(SearchResult.Data.create(topResults));
        presenter.attachView(topResultsView);
        searchIntent.onNext(search);
    }

    private TrackSourceInfo getTrackSourceInfo(SearchEvent.ClickSource clickSource) {
        final TrackSourceInfo trackSourceInfo = new TrackSourceInfo(Screen.SEARCH_EVERYTHING.get(), true);
        trackSourceInfo.setSource(clickSource.key, Strings.EMPTY);
        return trackSourceInfo;
    }

    private TopResults initTopResults() {
        TopResults.Bucket multipleTypeBucket = TopResultsFixtures.topResultsBucket(searchTrackItem(track), searchPlaylistItem(apiPlaylist), searchUserItem(apiUser));

        final List<TopResults.Bucket> bucketList = Lists.newArrayList(multipleTypeBucket);
        return TopResults.create(1, of(QUERY_URN), bucketList);
    }

    private void initSearch(SearchResult... searchResults) {
        when(operations.search(any(SearchParams.class))).thenReturn(Observable.fromArray(searchResults));
    }
}
