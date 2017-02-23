package com.soundcloud.android.search.topresults;

import static com.soundcloud.android.search.topresults.TopResultsFixtures.searchPlaylistItem;
import static com.soundcloud.android.search.topresults.TopResultsFixtures.searchTrackItem;
import static com.soundcloud.android.search.topresults.TopResultsFixtures.searchUserItem;
import static com.soundcloud.java.optional.Optional.of;
import static edu.emory.mathcs.backport.java.util.Collections.singleton;
import static edu.emory.mathcs.backport.java.util.Collections.singletonList;
import static java.util.Arrays.asList;
import static java.util.Collections.emptySet;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;

import com.soundcloud.android.api.model.ApiPlaylist;
import com.soundcloud.android.api.model.ApiTrack;
import com.soundcloud.android.api.model.ApiUser;
import com.soundcloud.android.associations.FollowingStateProvider;
import com.soundcloud.android.associations.FollowingStatuses;
import com.soundcloud.android.likes.LikedStatuses;
import com.soundcloud.android.likes.LikesStateProvider;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playlists.PlaylistItem;
import com.soundcloud.android.search.ApiUniversalSearchItem;
import com.soundcloud.android.search.SearchPlayQueueFilter;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import com.soundcloud.android.tracks.TrackItem;
import com.soundcloud.android.users.UserItem;
import com.soundcloud.android.view.adapters.CollectionViewState;
import com.soundcloud.java.collections.Pair;
import com.soundcloud.java.optional.Optional;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import rx.Observable;
import rx.observers.AssertableSubscriber;
import rx.subjects.BehaviorSubject;
import rx.subjects.PublishSubject;

import android.support.annotation.NonNull;

@SuppressWarnings("unchecked")
public class TopResultsPresenterTest extends AndroidUnitTest {

    private static final String QUERY = "query";
    private static final Urn TRACKS_BUCKET_URN = new Urn("soundcloud:search-buckets:freetiertracks");
    private static final Urn TOP_RESULT_BUCKET_URN = new Urn("soundcloud:search-buckets:top");
    private static final int BUCKET_POSITION = 0;

    private final BehaviorSubject<LikedStatuses> likesStatuses = BehaviorSubject.create();
    private final BehaviorSubject<FollowingStatuses> followingStatuses = BehaviorSubject.create();
    private TopResultsPresenter presenter;

    @Mock private TopResultsOperations operations;
    @Mock private LikesStateProvider likesStateProvider;
    @Mock private FollowingStateProvider followingStateProvider;
    @Mock private TopResultsPresenter.TopResultsView topResultsView;
    @Mock private SearchPlayQueueFilter playQueueFilter;

    private final ApiTrack apiTrack = ModelFixtures.apiTrack();
    private final ApiUser apiUser = ModelFixtures.apiUser();
    private final ApiPlaylist apiPlaylist = ModelFixtures.apiPlaylist();

    private final Pair<String, Optional<Urn>> searchQueryPair = Pair.of(QUERY, of(TopResultsFixtures.QUERY_URN));
    private final PublishSubject<Pair<String, Optional<Urn>>> searchIntent = PublishSubject.create();
    private final PublishSubject<Void> refreshIntent = PublishSubject.create();

    @Before
    public void setUp() throws Exception {
        when(likesStateProvider.likedStatuses()).thenReturn(likesStatuses);
        when(followingStateProvider.followingStatuses()).thenReturn(followingStatuses);

        presenter = new TopResultsPresenter(operations, playQueueFilter, likesStateProvider, followingStateProvider);

        when(topResultsView.searchIntent()).thenReturn(searchIntent);
        when(topResultsView.refreshIntent()).thenReturn(refreshIntent);
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
        when(operations.search(searchQueryPair)).thenReturn(Observable.just(singletonList(TopResultsFixtures.apiTrackResultsBucket(searchTrackItem(apiTrack)))));
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
        when(operations.search(searchQueryPair)).thenReturn(Observable.just(singletonList(TopResultsFixtures.apiTopResultsBucket(searchPlaylistItem(apiPlaylist)))));
        likesStatuses.onNext(LikedStatuses.create(singleton(apiPlaylist.getUrn())));

        presenter.attachView(topResultsView);
        searchIntent.onNext(searchQueryPair);
        followingStatuses.onNext(FollowingStatuses.create(emptySet()));

        presenter.viewModel().test().assertValue(
                getViewModel(getBucketViewModel(TOP_RESULT_BUCKET_URN, 1, SearchItem.Playlist.create(getPlaylistItem(apiPlaylist, true), BUCKET_POSITION)))
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
    public void handleTrackItemClick() throws Exception {
        final ApiUniversalSearchItem track1 = searchTrackItem(ModelFixtures.apiTrack());
        final ApiUniversalSearchItem track2 = searchTrackItem(ModelFixtures.apiTrack());
        final ApiUniversalSearchItem track3 = searchTrackItem(ModelFixtures.apiTrack());
        final SearchItem.Track searchTrack1 = SearchItem.Track.create(TrackItem.fromLiked(track1.track().get(), false), BUCKET_POSITION);
        final SearchItem.Track searchTrack2 = SearchItem.Track.create(TrackItem.fromLiked(track2.track().get(), false), BUCKET_POSITION);
        final SearchItem.Track searchTrack3 = SearchItem.Track.create(TrackItem.fromLiked(track3.track().get(), false), BUCKET_POSITION);

        final ApiTopResultsBucket apiTopResultsBucket = TopResultsFixtures.apiTrackResultsBucket(track1, track2, track3);
        doAnswer(invocation -> invocation.getArguments()[0]).when(playQueueFilter).correctPosition(anyInt());
        doAnswer(invocation -> invocation.getArguments()[0]).when(playQueueFilter).correctQueue(anyList(), anyInt());

        initTopResultsSearch(apiTopResultsBucket);

        final AssertableSubscriber<TrackItemClick> testSubscriber = presenter.trackItemClicked().test();

        presenter.searchItemClicked().onNext(searchTrack2);

        testSubscriber.assertValueCount(1);
        final TrackItemClick trackItemClick = testSubscriber.getOnNextEvents().get(0);
        assertThat(trackItemClick.position()).isEqualTo(1);
        assertThat(trackItemClick.playQueue()).containsExactly(searchTrack1.trackItem(), searchTrack2.trackItem(), searchTrack3.trackItem());
        assertThat(trackItemClick.searchQuerySourceInfo().getClickPosition()).isEqualTo(BUCKET_POSITION);
        assertThat(trackItemClick.searchQuerySourceInfo().getClickUrn()).isEqualTo(searchTrack2.trackItem().getUrn());
    }

    @Test
    public void handlePlaylistItemClick() throws Exception {
        final ApiUniversalSearchItem playlist = searchPlaylistItem(ModelFixtures.apiPlaylist());
        final PlaylistItem playlistItem = PlaylistItem.fromLiked(playlist.playlist().get(), false);
        final SearchItem.Playlist searchPlaylist = SearchItem.Playlist.create(playlistItem, BUCKET_POSITION);

        final ApiTopResultsBucket apiTopResultsBucket = TopResultsFixtures.apiTrackResultsBucket(playlist);
        initTopResultsSearch(apiTopResultsBucket);

        final AssertableSubscriber<PlaylistItemClick> testSubscriber = presenter.playlistItemClicked().test();

        presenter.searchItemClicked().onNext(searchPlaylist);

        testSubscriber.assertValueCount(1);
        final PlaylistItemClick playlistItemClick = testSubscriber.getOnNextEvents().get(0);
        assertThat(playlistItemClick.playlistItem()).isEqualTo(playlistItem);
        assertThat(playlistItemClick.searchQuerySourceInfo().getClickPosition()).isEqualTo(BUCKET_POSITION);
        assertThat(playlistItemClick.searchQuerySourceInfo().getClickUrn()).isEqualTo(playlistItem.getUrn());
    }

    @Test
    public void handleUserItemClick() throws Exception {
        final ApiUniversalSearchItem user = searchUserItem(ModelFixtures.apiUser());
        final UserItem userItem = UserItem.from(user.user().get());
        final SearchItem.User searchUser = SearchItem.User.create(userItem, BUCKET_POSITION);

        final ApiTopResultsBucket apiTopResultsBucket = TopResultsFixtures.apiTrackResultsBucket(user);
        initTopResultsSearch(apiTopResultsBucket);

        final AssertableSubscriber<UserItemClick> testSubscriber = presenter.userItemClicked().test();

        presenter.searchItemClicked().onNext(searchUser);

        testSubscriber.assertValueCount(1);
        final UserItemClick userItemClick = testSubscriber.getOnNextEvents().get(0);
        assertThat(userItemClick.userItem()).isEqualTo(userItem);
        assertThat(userItemClick.searchQuerySourceInfo().getClickPosition()).isEqualTo(BUCKET_POSITION);
        assertThat(userItemClick.searchQuerySourceInfo().getClickUrn()).isEqualTo(userItem.getUrn());
    }

    private void initTopResultsSearch(ApiTopResultsBucket apiTopResultsBucket) {
        when(operations.search(searchQueryPair))
                .thenReturn(Observable.just(singletonList(apiTopResultsBucket)));

        presenter.attachView(topResultsView);
        searchIntent.onNext(searchQueryPair);
        likesStatuses.onNext(LikedStatuses.create(emptySet()));
        followingStatuses.onNext(FollowingStatuses.create(emptySet()));
    }

    @NonNull
    private TopResultsBucketViewModel getBucketViewModel(Urn bucketUrn, int totalResults, SearchItem... searchItems) {
        return TopResultsBucketViewModel.create(asList(searchItems), bucketUrn, totalResults, TopResultsFixtures.QUERY_URN);
    }


    @NonNull
    private TopResultsViewModel getViewModel(TopResultsBucketViewModel... bucketViewModels) {
        return TopResultsViewModel.create(CollectionViewState.<TopResultsBucketViewModel>builder().hasMorePages(false).items(asList(bucketViewModels)).build());
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

    private UserItem getUserItem(ApiUser apiUser) {
        return UserItem.from(apiUser);
    }

}