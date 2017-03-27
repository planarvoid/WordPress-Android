package com.soundcloud.android.search.topresults;

import static com.soundcloud.android.events.SearchEvent.ClickSource.PLAYLISTS_BUCKET;
import static com.soundcloud.android.events.SearchEvent.ClickSource.TRACKS_BUCKET;
import static com.soundcloud.android.search.topresults.TopResultsFixtures.QUERY_URN;
import static com.soundcloud.android.search.topresults.TopResultsFixtures.searchPlaylistItem;
import static com.soundcloud.android.search.topresults.TopResultsFixtures.searchTrackItem;
import static com.soundcloud.android.search.topresults.TopResultsFixtures.searchUserItem;
import static com.soundcloud.java.optional.Optional.absent;
import static com.soundcloud.java.optional.Optional.of;
import static edu.emory.mathcs.backport.java.util.Collections.singleton;
import static edu.emory.mathcs.backport.java.util.Collections.singletonList;
import static java.util.Arrays.asList;
import static java.util.Collections.emptySet;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
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
import com.soundcloud.android.playback.PlaySessionStateProvider;
import com.soundcloud.android.playback.PlaybackInitiator;
import com.soundcloud.android.playback.PlaybackResult;
import com.soundcloud.android.playback.TrackSourceInfo;
import com.soundcloud.android.playlists.PlaylistItem;
import com.soundcloud.android.search.ApiUniversalSearchItem;
import com.soundcloud.android.search.SearchPlayQueueFilter;
import com.soundcloud.android.search.SearchTracker;
import com.soundcloud.android.search.topresults.TopResults.Bucket.Kind;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import com.soundcloud.android.tracks.TrackItem;
import com.soundcloud.android.users.UserItem;
import com.soundcloud.android.view.collection.CollectionRendererState;
import com.soundcloud.java.optional.Optional;
import com.soundcloud.java.strings.Strings;
import com.soundcloud.rx.eventbus.TestEventBus;
import edu.emory.mathcs.backport.java.util.Arrays;
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
    private static final int BUCKET_POSITION = 0;
    private static final int QUERY_POSITION = 1;
    private final BehaviorSubject<LikedStatuses> likesStatuses = BehaviorSubject.create();

    private final BehaviorSubject<FollowingStatuses> followingStatuses = BehaviorSubject.create();
    private final BehaviorSubject<Urn> nowPlaying = BehaviorSubject.create();
    private TopResultsPresenter presenter;
    @Mock private TopResultsOperations operations;

    @Mock private LikesStateProvider likesStateProvider;
    @Mock private FollowingStateProvider followingStateProvider;
    @Mock private TopResultsPresenter.TopResultsView topResultsView;
    @Mock private SearchPlayQueueFilter playQueueFilter;
    @Mock private PlaybackInitiator playbackInitiator;
    @Mock private Navigator navigator;
    @Mock private EventTracker eventTracker;
    @Mock private SearchTracker searchTracker;
    @Mock private TrackingStateProvider trackingStateProvider;
    @Mock private PlaySessionStateProvider playSessionStateProvider;
    @Captor private ArgumentCaptor<SearchEvent> searchEventCaptor;
    private final ApiTrack apiTrack = ModelFixtures.apiTrack();

    private final ApiUser apiUser = ModelFixtures.apiUser();

    private final ApiPlaylist apiPlaylist = ModelFixtures.apiPlaylist();
    private final SearchParams searchParams = SearchParams.create(QUERY, QUERY, of(TopResultsFixtures.QUERY_URN), of(QUERY_POSITION));

    private final PublishSubject<SearchParams> searchIntent = PublishSubject.create();
    private final PublishSubject<Void> refreshIntent = PublishSubject.create();
    private final PublishSubject<Void> enterScreen = PublishSubject.create();
    private final TestEventBus eventBus = new TestEventBus();
    private PublishSubject<PlaybackResult> playbackResultSubject;

    @Before
    public void setUp() throws Exception {
        when(likesStateProvider.likedStatuses()).thenReturn(likesStatuses);
        when(followingStateProvider.followingStatuses()).thenReturn(followingStatuses);
        when(playSessionStateProvider.nowPlayingUrn()).thenReturn(nowPlaying);

        presenter = new TopResultsPresenter(operations,
                                            playQueueFilter,
                                            likesStateProvider,
                                            followingStateProvider,
                                            playbackInitiator,
                                            eventTracker,
                                            searchTracker,
                                            eventBus,
                                            ModelFixtures.entityItemCreator(),
                                            trackingStateProvider,
                                            playSessionStateProvider);

        when(topResultsView.searchIntent()).thenReturn(searchIntent);
        when(topResultsView.refreshIntent()).thenReturn(refreshIntent);
        when(topResultsView.enterScreen()).thenReturn(enterScreen);
        when(trackingStateProvider.getLastEvent()).thenReturn(absent());
    }

    @Test
    public void emitsViewModel() throws Exception {
        TopResults.Bucket multipleTypeBucket = TopResultsFixtures.topResultsBucket(searchTrackItem(apiTrack), searchPlaylistItem(apiPlaylist), searchUserItem(apiUser));
        initTopResultsSearch(multipleTypeBucket);

        final TrackSourceInfo trackSourceInfo = getTrackSourceInfo(SearchEvent.ClickSource.TOP_RESULTS_BUCKET);
        presenter.viewModel().test().assertValue(
                getViewModel(getBucketViewModel(Kind.TOP_RESULT,
                                                3,
                                                SearchItem.Track.create(getTrackItem(apiTrack), BUCKET_POSITION, trackSourceInfo),
                                                SearchItem.Playlist.create(getPlaylistItem(apiPlaylist), BUCKET_POSITION, Kind.TOP_RESULT.toClickSource()),
                                                SearchItem.User.create(getUserItem(apiUser), BUCKET_POSITION, Kind.TOP_RESULT.toClickSource())))
        );
    }

    @Test
    public void viewModelEmitsLikedTracksBucket() throws Exception {
        final List buckets = singletonList(TopResultsFixtures.trackResultsBucket(searchTrackItem(apiTrack)));
        final TopResults topResults = TopResults.create(1, of(QUERY_URN), buckets);
        when(operations.search(searchParams)).thenReturn(Observable.just(topResults));
        likesStatuses.onNext(LikedStatuses.create(singleton(apiTrack.getUrn())));

        presenter.attachView(topResultsView);
        searchIntent.onNext(searchParams);
        followingStatuses.onNext(FollowingStatuses.create(emptySet()));
        nowPlaying.onNext(Urn.NOT_SET);

        presenter.viewModel().test().assertValue(
                getViewModel(getBucketViewModel(Kind.TRACKS, 1, SearchItem.Track.create(getTrackItem(apiTrack, true), BUCKET_POSITION, getTrackSourceInfo(TRACKS_BUCKET))))
        );
    }

    @Test
    public void viewModelEmitsNowPlayingTracksBucket() throws Exception {
        final List buckets = singletonList(TopResultsFixtures.trackResultsBucket(searchTrackItem(apiTrack)));
        final TopResults topResults = TopResults.create(1, of(QUERY_URN), buckets);
        when(operations.search(searchParams)).thenReturn(Observable.just(topResults));
        nowPlaying.onNext(apiTrack.getUrn());

        presenter.attachView(topResultsView);
        searchIntent.onNext(searchParams);
        likesStatuses.onNext(LikedStatuses.create(emptySet()));
        followingStatuses.onNext(FollowingStatuses.create(emptySet()));

        presenter.viewModel().test().assertValue(
                getViewModel(getBucketViewModel(Kind.TRACKS, 1, SearchItem.Track.create(getNowPlayingTrack(apiTrack), BUCKET_POSITION, getTrackSourceInfo(TRACKS_BUCKET))))
        );
    }

    @Test
    public void viewModelEmitsLikedPlaylistsBucket() throws Exception {
        final List buckets = singletonList(TopResultsFixtures.trackResultsBucket(searchPlaylistItem(apiPlaylist)));
        final TopResults topResults = TopResults.create(1, of(QUERY_URN), buckets);
        when(operations.search(searchParams)).thenReturn(Observable.just(topResults));
        likesStatuses.onNext(LikedStatuses.create(singleton(apiPlaylist.getUrn())));

        presenter.attachView(topResultsView);
        searchIntent.onNext(searchParams);
        followingStatuses.onNext(FollowingStatuses.create(emptySet()));
        nowPlaying.onNext(Urn.NOT_SET);

        presenter.viewModel().test().assertValue(
                getViewModel(getBucketViewModel(Kind.TRACKS, 1, SearchItem.Playlist.create(getPlaylistItem(apiPlaylist, true), BUCKET_POSITION, TRACKS_BUCKET)))
        );
    }

    @Test
    public void viewModelEmitsLikedTracksBucketAfterUpdate() throws Exception {
        initTopResultsSearch(TopResultsFixtures.trackResultsBucket(searchTrackItem(apiTrack)));

        presenter.viewModel().test().assertValue(
                getViewModel(getBucketViewModel(Kind.TRACKS, 1, SearchItem.Track.create(getTrackItem(apiTrack), BUCKET_POSITION, getTrackSourceInfo(TRACKS_BUCKET)))));

        likesStatuses.onNext(LikedStatuses.create(singleton(apiTrack.getUrn())));

        presenter.viewModel().test().assertValue(
                getViewModel(getBucketViewModel(Kind.TRACKS, 1, SearchItem.Track.create(getTrackItem(apiTrack, true), BUCKET_POSITION, getTrackSourceInfo(TRACKS_BUCKET))))
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
        final SearchItem.Track searchTrack1 = SearchItem.Track.create(ModelFixtures.trackItem(track1.track().get()), BUCKET_POSITION, getTrackSourceInfo(TRACKS_BUCKET));
        final SearchItem.Track searchTrack2 = SearchItem.Track.create(ModelFixtures.trackItem(track2.track().get()), BUCKET_POSITION, getTrackSourceInfo(TRACKS_BUCKET));
        final SearchItem.Track searchTrack3 = SearchItem.Track.create(ModelFixtures.trackItem(track3.track().get()), BUCKET_POSITION, getTrackSourceInfo(TRACKS_BUCKET));

        final TopResults.Bucket apiTopResultsBucket = TopResultsFixtures.trackResultsBucket(track1, track2, track3);
        doAnswer(invocation -> invocation.getArguments()[0]).when(playQueueFilter).correctPosition(anyInt());
        doAnswer(invocation -> invocation.getArguments()[0]).when(playQueueFilter).correctQueue(anyList(), anyInt());

        initTopResultsSearch(apiTopResultsBucket);

        List<Urn> expectedQueue = asList(searchTrack1.trackItem().getUrn(), searchTrack2.trackItem().getUrn(), searchTrack3.trackItem().getUrn());
        final PlaySessionSource playSessionSource = new PlaySessionSource(Screen.SEARCH_EVERYTHING);
        final SearchQuerySourceInfo searchQuerySourceInfo = new SearchQuerySourceInfo(searchParams.queryUrn().get(),
                                                                                      BUCKET_POSITION,
                                                                                      searchTrack2.trackItem().getUrn(),
                                                                                      searchParams.apiQuery());
        playSessionSource.setSearchQuerySourceInfo(searchQuerySourceInfo);

        this.playbackResultSubject = PublishSubject.create();
        when(playbackInitiator.playPosts(anyList(), any(Urn.class), anyInt(), any(PlaySessionSource.class))).thenReturn(playbackResultSubject);

        presenter.searchItemClicked().onNext(searchTrack2);

        assertThat(playbackResultSubject.hasObservers()).isTrue();
        verify(playbackInitiator).playPosts(expectedQueue, searchTrack2.itemUrn().get(), 1, playSessionSource);
        return searchTrack2;
    }

    @Test
    public void buildsPlayQueueWithTrackAndPlaylistsFromSeparateBuckets() {
        final ApiUniversalSearchItem track = searchTrackItem(ModelFixtures.apiTrack());
        final ApiUniversalSearchItem playlist = searchPlaylistItem(ModelFixtures.apiPlaylist());
        final SearchItem.Track searchTrack = SearchItem.Track.create(ModelFixtures.trackItem(track.track().get()), 0, getTrackSourceInfo(TRACKS_BUCKET));
        final SearchItem.Playlist searchPlaylist = SearchItem.Playlist.create(ModelFixtures.playlistItem(playlist.playlist().get()), 1, PLAYLISTS_BUCKET);

        final TopResults.Bucket apiTrackBucket = TopResultsFixtures.trackResultsBucket(track);
        final TopResults.Bucket apiPlaylistBucket = TopResultsFixtures.playlistResultsBucket(playlist);
        doAnswer(invocation -> invocation.getArguments()[0]).when(playQueueFilter).correctPosition(anyInt());
        doAnswer(invocation -> invocation.getArguments()[0]).when(playQueueFilter).correctQueue(anyList(), anyInt());

        initTopResultsSearch(apiTrackBucket, apiPlaylistBucket);

        List<Urn> expectedQueue = asList(searchTrack.trackItem().getUrn(), searchPlaylist.playlistItem().getUrn());
        final PlaySessionSource playSessionSource = new PlaySessionSource(Screen.SEARCH_EVERYTHING);
        final SearchQuerySourceInfo searchQuerySourceInfo = new SearchQuerySourceInfo(searchParams.queryUrn().get(),
                                                                                      BUCKET_POSITION,
                                                                                      searchTrack.trackItem().getUrn(),
                                                                                      searchParams.apiQuery());
        playSessionSource.setSearchQuerySourceInfo(searchQuerySourceInfo);

        this.playbackResultSubject = PublishSubject.create();
        when(playbackInitiator.playPosts(anyList(), any(Urn.class), anyInt(), any(PlaySessionSource.class))).thenReturn(playbackResultSubject);

        presenter.searchItemClicked().onNext(searchTrack);

        assertThat(playbackResultSubject.hasObservers()).isTrue();
        verify(playbackInitiator).playPosts(expectedQueue, searchTrack.itemUrn().get(), 0, playSessionSource);
    }

    @Test
    public void goesToPlaylistOnPlaylistItemClick() throws Exception {
        final ApiUniversalSearchItem playlist = searchPlaylistItem(ModelFixtures.apiPlaylist());
        final PlaylistItem playlistItem = ModelFixtures.playlistItem(playlist.playlist().get());
        final SearchItem.Playlist searchPlaylist = SearchItem.Playlist.create(playlistItem, BUCKET_POSITION, TRACKS_BUCKET);

        final TopResults.Bucket apiTopResultsBucket = TopResultsFixtures.trackResultsBucket(playlist);
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
        final SearchItem.User searchUser = SearchItem.User.create(userItem, BUCKET_POSITION, TRACKS_BUCKET);

        final TopResults.Bucket topResultsBucket = TopResultsFixtures.trackResultsBucket(user);
        initTopResultsSearch(topResultsBucket);

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

        initTopResultsSearch(TopResultsFixtures.trackResultsBucket(searchUserItem(ModelFixtures.apiUser())));

        final TopResultsViewAllArgs topResultsViewAllArgs = TopResultsViewAllArgs.create(Kind.ALBUMS);

        final AssertableSubscriber<TopResultsViewAllArgs> testSubscriber = presenter.goToViewAllPage().test();

        presenter.viewAllClicked().onNext(topResultsViewAllArgs);

        testSubscriber.assertValueCount(1);
        final TopResultsViewAllArgs viewAllClickWithQuery = testSubscriber.getOnNextEvents().get(0);
        assertThat(viewAllClickWithQuery).isEqualTo(topResultsViewAllArgs.copyWithSearchQuery(QUERY, of(QUERY_URN)));
    }

    @Test
    public void trackEnterScreenEvent() throws Exception {
        initTopResultsSearch(TopResultsFixtures.trackResultsBucket(searchUserItem(ModelFixtures.apiUser())));
        final Optional<ReferringEvent> referringEvent = of(ReferringEvent.create("123A", "kind"));
        when(trackingStateProvider.getLastEvent()).thenReturn(referringEvent);

        enterScreen.onNext(null);

        verify(eventTracker).trackScreen(eq(ScreenEvent.create(Screen.SEARCH_EVERYTHING.get(), new SearchQuerySourceInfo(QUERY_URN, QUERY))), eq(referringEvent));

    }

    private void initTopResultsSearch(TopResults.Bucket... buckets) {

        final List<TopResults.Bucket> bucketList = Arrays.asList(buckets);
        final TopResults topResults = TopResults.create(1, of(QUERY_URN), bucketList);
        when(operations.search(searchParams)).thenReturn(Observable.just(topResults));

        presenter.attachView(topResultsView);
        searchIntent.onNext(searchParams);
        likesStatuses.onNext(LikedStatuses.create(emptySet()));
        followingStatuses.onNext(FollowingStatuses.create(emptySet()));
        nowPlaying.onNext(Urn.NOT_SET);
    }

    @NonNull
    private TopResultsBucketViewModel getBucketViewModel(Kind kind, int totalResults, SearchItem... searchItems) {
        return TopResultsBucketViewModel.create(asList(searchItems), kind, totalResults);
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

    private TrackItem getNowPlayingTrack(ApiTrack apiTrack) {
        return ModelFixtures.trackItemBuilder(apiTrack).isPlaying(true).build();
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

    @NonNull
    private TrackSourceInfo getTrackSourceInfo(SearchEvent.ClickSource clickSource) {
        final TrackSourceInfo trackSourceInfo = new TrackSourceInfo(Screen.SEARCH_EVERYTHING.get(), true);
        trackSourceInfo.setSource(clickSource.key, Strings.EMPTY);
        return trackSourceInfo;
    }
}
