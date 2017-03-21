package com.soundcloud.android.search.topresults;

import static com.soundcloud.android.search.topresults.TopResultsFixtures.QUERY_URN;
import static com.soundcloud.android.search.topresults.TopResultsFixtures.searchPlaylistItem;
import static com.soundcloud.android.search.topresults.TopResultsFixtures.searchTrackItem;
import static com.soundcloud.android.search.topresults.TopResultsFixtures.searchUserItem;
import static com.soundcloud.java.optional.Optional.absent;
import static com.soundcloud.java.optional.Optional.of;
import static edu.emory.mathcs.backport.java.util.Collections.emptyMap;
import static edu.emory.mathcs.backport.java.util.Collections.singleton;
import static edu.emory.mathcs.backport.java.util.Collections.singletonList;
import static java.util.Arrays.asList;
import static java.util.Collections.emptySet;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.Navigator;
import com.soundcloud.android.analytics.EventTracker;
import com.soundcloud.android.analytics.SearchQuerySourceInfo;
import com.soundcloud.android.analytics.TrackingStateProvider;
import com.soundcloud.android.api.model.ApiPlaylist;
import com.soundcloud.android.api.model.ApiTrack;
import com.soundcloud.android.api.model.ApiUser;
import com.soundcloud.android.api.model.ModelCollection;
import com.soundcloud.android.associations.FollowingStateProvider;
import com.soundcloud.android.associations.FollowingStatuses;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PlayerUICommand;
import com.soundcloud.android.events.ReferringEvent;
import com.soundcloud.android.events.ScreenEvent;
import com.soundcloud.android.events.SearchEvent;
import com.soundcloud.android.likes.LikedStatuses;
import com.soundcloud.android.likes.LikesStateProvider;
import com.soundcloud.android.main.Screen;
import com.soundcloud.android.model.CollectionLoadingState;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.PlaySessionSource;
import com.soundcloud.android.playback.PlaybackInitiator;
import com.soundcloud.android.playback.PlaybackResult;
import com.soundcloud.android.playlists.PlaylistItem;
import com.soundcloud.android.search.ApiUniversalSearchItem;
import com.soundcloud.android.search.SearchPlayQueueFilter;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import com.soundcloud.android.tracks.TrackItem;
import com.soundcloud.android.users.UserItem;
import com.soundcloud.android.view.collection.CollectionRendererState;
import com.soundcloud.java.collections.Pair;
import com.soundcloud.java.optional.Optional;
import com.soundcloud.rx.eventbus.TestEventBus;
import edu.emory.mathcs.backport.java.util.Collections;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import rx.Observable;
import rx.observers.AssertableSubscriber;
import rx.subjects.BehaviorSubject;
import rx.subjects.PublishSubject;

import android.support.annotation.NonNull;

import java.util.List;

@SuppressWarnings("unchecked")
public class TopResultsPresenterTest extends AndroidUnitTest {

    private static final String QUERY = "query";
    private static final Urn TRACKS_BUCKET_URN = new Urn("soundcloud:search-buckets:freetiertracks");
    private static final Urn TOP_RESULT_BUCKET_URN = new Urn("soundcloud:search-buckets:topresult");
    private static final int BUCKET_POSITION = 0;

    private final BehaviorSubject<LikedStatuses> likesStatuses = BehaviorSubject.create();
    private final BehaviorSubject<FollowingStatuses> followingStatuses = BehaviorSubject.create();
    private TopResultsPresenter presenter;

    @Mock private TopResultsOperations operations;
    @Mock private LikesStateProvider likesStateProvider;
    @Mock private FollowingStateProvider followingStateProvider;
    @Mock private TopResultsPresenter.TopResultsView topResultsView;
    @Mock private SearchPlayQueueFilter playQueueFilter;
    @Mock private PlaybackInitiator playbackInitiator;
    @Mock private Navigator navigator;
    @Mock private EventTracker eventTracker;
    @Mock private TrackingStateProvider trackingStateProvider;
    @Captor private ArgumentCaptor<SearchEvent> searchEventCaptor;

    private final ApiTrack apiTrack = ModelFixtures.apiTrack();
    private final ApiUser apiUser = ModelFixtures.apiUser();
    private final ApiPlaylist apiPlaylist = ModelFixtures.apiPlaylist();
    private final Pair<String, Optional<Urn>> searchQueryPair = Pair.of(QUERY, of(TopResultsFixtures.QUERY_URN));

    private final PublishSubject<Pair<String, Optional<Urn>>> searchIntent = PublishSubject.create();
    private final PublishSubject<Void> refreshIntent = PublishSubject.create();
    private final PublishSubject<Void> enterScreen = PublishSubject.create();
    private final TestEventBus eventBus = new TestEventBus();
    private PublishSubject<PlaybackResult> playbackResultSubject;

    @Before
    public void setUp() throws Exception {
        when(likesStateProvider.likedStatuses()).thenReturn(likesStatuses);
        when(followingStateProvider.followingStatuses()).thenReturn(followingStatuses);

        presenter = new TopResultsPresenter(operations,
                                            playQueueFilter,
                                            likesStateProvider,
                                            followingStateProvider,
                                            playbackInitiator,
                                            eventTracker,
                                            eventBus,
                                            ModelFixtures.entityItemCreator(),
                                            trackingStateProvider);

        when(topResultsView.searchIntent()).thenReturn(searchIntent);
        when(topResultsView.refreshIntent()).thenReturn(refreshIntent);
        when(topResultsView.enterScreen()).thenReturn(enterScreen);
        when(trackingStateProvider.getLastEvent()).thenReturn(absent());
    }

    @Test
    public void emitsViewModel() throws Exception {
        ApiTopResultsBucket multipleTypeBucket = TopResultsFixtures.apiTopResultsBucket(searchTrackItem(apiTrack), searchPlaylistItem(apiPlaylist), searchUserItem(apiUser));
        initTopResultsSearch(multipleTypeBucket);

        presenter.viewModel().test().assertValue(
                getViewModel(getBucketViewModel(TOP_RESULT_BUCKET_URN,
                                                3,
                                                SearchItem.Track.create(getTrackItem(apiTrack), BUCKET_POSITION),
                                                SearchItem.Playlist.create(getPlaylistItem(apiPlaylist), BUCKET_POSITION),
                                                SearchItem.User.create(getUserItem(apiUser), BUCKET_POSITION)))
        );
    }

    @Test
    public void viewModelEmitsLikedTracksBucket() throws Exception {
        final List buckets = singletonList(TopResultsFixtures.apiTrackResultsBucket(searchTrackItem(apiTrack)));
        final ApiTopResults apiTopResults = ApiTopResults.create(1, new ModelCollection(buckets, emptyMap(), QUERY_URN));
        when(operations.search(searchQueryPair)).thenReturn(Observable.just(apiTopResults));
        likesStatuses.onNext(LikedStatuses.create(singleton(apiTrack.getUrn())));

        presenter.attachView(topResultsView);
        searchIntent.onNext(searchQueryPair);
        followingStatuses.onNext(FollowingStatuses.create(emptySet()));

        presenter.viewModel().test().assertValue(
                getViewModel(getBucketViewModel(TRACKS_BUCKET_URN, 1, SearchItem.Track.create(getTrackItem(apiTrack, true), BUCKET_POSITION)))
        );
    }

    @Test
    public void viewModelEmitsLikedPlaylistsBucket() throws Exception {
        final List buckets = singletonList(TopResultsFixtures.apiTrackResultsBucket(searchPlaylistItem(apiPlaylist)));
        final ApiTopResults apiTopResults = ApiTopResults.create(1, new ModelCollection(buckets, emptyMap(), QUERY_URN));
        when(operations.search(searchQueryPair)).thenReturn(Observable.just(apiTopResults));
        likesStatuses.onNext(LikedStatuses.create(singleton(apiPlaylist.getUrn())));

        presenter.attachView(topResultsView);
        searchIntent.onNext(searchQueryPair);
        followingStatuses.onNext(FollowingStatuses.create(emptySet()));

        presenter.viewModel().test().assertValue(
                getViewModel(getBucketViewModel(TRACKS_BUCKET_URN, 1, SearchItem.Playlist.create(getPlaylistItem(apiPlaylist, true), BUCKET_POSITION)))
        );
    }

    @Test
    public void viewModelEmitsLikedTracksBucketAfterUpdate() throws Exception {
        initTopResultsSearch(TopResultsFixtures.apiTrackResultsBucket(searchTrackItem(apiTrack)));

        presenter.viewModel().test().assertValue(
                getViewModel(getBucketViewModel(TRACKS_BUCKET_URN, 1, SearchItem.Track.create(getTrackItem(apiTrack), BUCKET_POSITION))));

        likesStatuses.onNext(LikedStatuses.create(singleton(apiTrack.getUrn())));

        presenter.viewModel().test().assertValue(
                getViewModel(getBucketViewModel(TRACKS_BUCKET_URN, 1, SearchItem.Track.create(getTrackItem(apiTrack, true), BUCKET_POSITION)))
        );
    }


    @Test
    public void playsTrackOnTrackItemClick() throws Exception {
        final SearchItem.Track clickedTrack = setupPlayback();

        playbackResultSubject.onNext(PlaybackResult.success());

        assertThat(eventBus.lastEventOn(EventQueue.PLAYER_COMMAND)).isEqualTo(PlayerUICommand.expandPlayer());

        verify(eventTracker).trackSearch(searchEventCaptor.capture());

        assertThat(searchEventCaptor.getValue().pageName().get()).isEqualTo(Screen.SEARCH_EVERYTHING.get());
        assertThat(searchEventCaptor.getValue().clickName().get()).isEqualTo(SearchEvent.ClickName.ITEM_NAVIGATION);
        assertThat(searchEventCaptor.getValue().clickObject().get()).isEqualTo(clickedTrack.itemUrn().get());

    }

    @Test
    public void showsErrorOnTrackItemClick() throws Exception {
        setupPlayback();

        AssertableSubscriber<PlaybackResult.ErrorReason> assertableSubscriber = presenter.playbackError().test();

        playbackResultSubject.onNext(PlaybackResult.error(PlaybackResult.ErrorReason.TRACK_UNAVAILABLE_OFFLINE));

        assertableSubscriber.assertValue(PlaybackResult.ErrorReason.TRACK_UNAVAILABLE_OFFLINE);
    }

    private SearchItem.Track setupPlayback() {
        final ApiUniversalSearchItem track1 = searchTrackItem(ModelFixtures.apiTrack());
        final ApiUniversalSearchItem track2 = searchTrackItem(ModelFixtures.apiTrack());
        final ApiUniversalSearchItem track3 = searchTrackItem(ModelFixtures.apiTrack());
        final SearchItem.Track searchTrack1 = SearchItem.Track.create(ModelFixtures.trackItem(track1.track().get()), BUCKET_POSITION);
        final SearchItem.Track searchTrack2 = SearchItem.Track.create(ModelFixtures.trackItem(track2.track().get()), BUCKET_POSITION);
        final SearchItem.Track searchTrack3 = SearchItem.Track.create(ModelFixtures.trackItem(track3.track().get()), BUCKET_POSITION);

        final ApiTopResultsBucket apiTopResultsBucket = TopResultsFixtures.apiTrackResultsBucket(track1, track2, track3);
        doAnswer(invocation -> invocation.getArguments()[0]).when(playQueueFilter).correctPosition(anyInt());
        doAnswer(invocation -> invocation.getArguments()[0]).when(playQueueFilter).correctQueue(anyList(), anyInt());

        initTopResultsSearch(apiTopResultsBucket);

        List<Urn> expectedQueue = asList(searchTrack1.trackItem().getUrn(), searchTrack2.trackItem().getUrn(), searchTrack3.trackItem().getUrn());
        final PlaySessionSource playSessionSource = new PlaySessionSource(Screen.SEARCH_EVERYTHING);
        final SearchQuerySourceInfo searchQuerySourceInfo = new SearchQuerySourceInfo(searchQueryPair.second().get(),
                                                                                      BUCKET_POSITION,
                                                                                      searchTrack2.trackItem().getUrn(),
                                                                                      searchQueryPair.first());
        playSessionSource.setSearchQuerySourceInfo(searchQuerySourceInfo);

        this.playbackResultSubject = PublishSubject.create();
        when(playbackInitiator.playTracks(expectedQueue, 1, playSessionSource)).thenReturn(playbackResultSubject);

        presenter.searchItemClicked().onNext(searchTrack2);

        assertThat(playbackResultSubject.hasObservers()).isTrue();
        return searchTrack2;
    }

    @Test
    public void goesToPlaylistOnPlaylistItemClick() throws Exception {
        final ApiUniversalSearchItem playlist = searchPlaylistItem(ModelFixtures.apiPlaylist());
        final PlaylistItem playlistItem = ModelFixtures.playlistItem(playlist.playlist().get());
        final SearchItem.Playlist searchPlaylist = SearchItem.Playlist.create(playlistItem, BUCKET_POSITION);

        final ApiTopResultsBucket apiTopResultsBucket = TopResultsFixtures.apiTrackResultsBucket(playlist);
        initTopResultsSearch(apiTopResultsBucket);

        final AssertableSubscriber<GoToItemArgs> testSubscriber = presenter.onGoToPlaylist().test();

        presenter.searchItemClicked().onNext(searchPlaylist);

        testSubscriber.assertValueCount(1);
        final GoToItemArgs goToItemArgs = testSubscriber.getOnNextEvents().get(0);
        assertThat(goToItemArgs.itemUrn()).isEqualTo(playlistItem.getUrn());
        assertThat(goToItemArgs.searchQuerySourceInfo().getClickPosition()).isEqualTo(BUCKET_POSITION);
        assertThat(goToItemArgs.searchQuerySourceInfo().getClickUrn()).isEqualTo(playlistItem.getUrn());

        verify(eventTracker).trackSearch(searchEventCaptor.capture());

        assertThat(searchEventCaptor.getValue().pageName().get()).isEqualTo(Screen.SEARCH_EVERYTHING.get());
        assertThat(searchEventCaptor.getValue().clickName().get()).isEqualTo(SearchEvent.ClickName.ITEM_NAVIGATION);
        assertThat(searchEventCaptor.getValue().clickObject().get()).isEqualTo(playlistItem.getUrn());
    }

    @Test
    public void goesToProfileOnUserItemClick() throws Exception {
        final ApiUniversalSearchItem user = searchUserItem(ModelFixtures.apiUser());
        final UserItem userItem = ModelFixtures.userItem(user.user().get());
        final SearchItem.User searchUser = SearchItem.User.create(userItem, BUCKET_POSITION);

        final ApiTopResultsBucket apiTopResultsBucket = TopResultsFixtures.apiTrackResultsBucket(user);
        initTopResultsSearch(apiTopResultsBucket);

        final AssertableSubscriber<GoToItemArgs> testSubscriber = presenter.onGoToProfile().test();

        presenter.searchItemClicked().onNext(searchUser);

        testSubscriber.assertValueCount(1);
        final GoToItemArgs goToProfileArgs = testSubscriber.getOnNextEvents().get(0);
        assertThat(goToProfileArgs.itemUrn()).isEqualTo(userItem.getUrn());
        assertThat(goToProfileArgs.searchQuerySourceInfo().getClickPosition()).isEqualTo(BUCKET_POSITION);
        assertThat(goToProfileArgs.searchQuerySourceInfo().getClickUrn()).isEqualTo(userItem.getUrn());

        verify(eventTracker).trackSearch(searchEventCaptor.capture());

        assertThat(searchEventCaptor.getValue().pageName().get()).isEqualTo(Screen.SEARCH_EVERYTHING.get());
        assertThat(searchEventCaptor.getValue().clickName().get()).isEqualTo(SearchEvent.ClickName.ITEM_NAVIGATION);
        assertThat(searchEventCaptor.getValue().clickObject().get()).isEqualTo(userItem.getUrn());
    }

    @Test
    public void handleViewAllClick() throws Exception {

        initTopResultsSearch(TopResultsFixtures.apiTrackResultsBucket(searchUserItem(ModelFixtures.apiUser())));

        final TopResultsViewAllArgs topResultsViewAllArgs = TopResultsViewAllArgs.create(TopResultsBucketViewModel.Kind.ALBUMS);

        final AssertableSubscriber<TopResultsViewAllArgs> testSubscriber = presenter.goToViewAllPage().test();

        presenter.viewAllClicked().onNext(topResultsViewAllArgs);

        testSubscriber.assertValueCount(1);
        final TopResultsViewAllArgs viewAllClickWithQuery = testSubscriber.getOnNextEvents().get(0);
        assertThat(viewAllClickWithQuery).isEqualTo(topResultsViewAllArgs.copyWithSearchQuery(QUERY, of(QUERY_URN)));
    }

    @Test
    public void trackEnterScreenEvent() throws Exception {
        initTopResultsSearch(TopResultsFixtures.apiTrackResultsBucket(searchUserItem(ModelFixtures.apiUser())));
        final Optional<ReferringEvent> referringEvent = of(ReferringEvent.create("123A", "kind"));
        when(trackingStateProvider.getLastEvent()).thenReturn(referringEvent);

        enterScreen.onNext(null);

        verify(eventTracker).trackScreen(eq(ScreenEvent.create(Screen.SEARCH_EVERYTHING.get(), new SearchQuerySourceInfo(QUERY_URN, QUERY))), eq(referringEvent));

    }

    private void initTopResultsSearch(ApiTopResultsBucket apiTopResultsBucket) {

        final List buckets = singletonList(apiTopResultsBucket);
        final ApiTopResults apiTopResults = ApiTopResults.create(1, new ModelCollection(buckets, Collections.emptyMap(), QUERY_URN));
        when(operations.search(searchQueryPair)).thenReturn(Observable.just(apiTopResults));

        presenter.attachView(topResultsView);
        searchIntent.onNext(searchQueryPair);
        likesStatuses.onNext(LikedStatuses.create(emptySet()));
        followingStatuses.onNext(FollowingStatuses.create(emptySet()));
    }

    @NonNull
    private TopResultsBucketViewModel getBucketViewModel(Urn bucketUrn, int totalResults, SearchItem... searchItems) {
        return TopResultsBucketViewModel.create(asList(searchItems), bucketUrn, totalResults);
    }

    @NonNull
    private TopResultsViewModel getViewModel(TopResultsBucketViewModel... bucketViewModels) {
        final CollectionLoadingState collectionLoadingState = CollectionLoadingState.builder().hasMorePages(false).build();
        return TopResultsViewModel.create(of(TopResultsFixtures.QUERY_URN), CollectionRendererState.create(collectionLoadingState, asList(bucketViewModels)));
    }

    private TrackItem getTrackItem(ApiTrack apiTrack) {
        return getTrackItem(apiTrack, false);
    }

    private TrackItem getTrackItem(ApiTrack apiTrack, boolean isLiked) {
        return ModelFixtures.trackItemBuilder(apiTrack).isUserLike(isLiked).build();
    }

    private PlaylistItem getPlaylistItem(ApiPlaylist apiPlaylist) {
        return getPlaylistItem(apiPlaylist, false);
    }

    private PlaylistItem getPlaylistItem(ApiPlaylist apiPlaylist, boolean isLiked) {
        return ModelFixtures.playlistItemBuilder(apiPlaylist).isUserLike(isLiked).build();
    }

    private UserItem getUserItem(ApiUser apiUser) {
        return ModelFixtures.userItem(apiUser);
    }

}
