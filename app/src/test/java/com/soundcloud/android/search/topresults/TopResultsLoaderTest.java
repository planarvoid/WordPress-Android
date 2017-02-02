package com.soundcloud.android.search.topresults;

import static com.soundcloud.android.search.topresults.TopResultsBucketViewModel.Kind.TOP_RESULT;
import static com.soundcloud.android.search.topresults.TopResultsBucketViewModel.Kind.TRACKS;
import static com.soundcloud.android.search.topresults.TopResultsFixtures.searchPlaylistItem;
import static com.soundcloud.android.search.topresults.TopResultsFixtures.searchTrackItem;
import static com.soundcloud.android.search.topresults.TopResultsFixtures.searchUserItem;
import static com.soundcloud.java.optional.Optional.of;
import static edu.emory.mathcs.backport.java.util.Collections.singleton;
import static java.util.Arrays.asList;
import static java.util.Collections.emptySet;
import static java.util.Collections.singletonList;
import static org.mockito.Mockito.when;

import com.soundcloud.android.api.model.ApiPlaylist;
import com.soundcloud.android.api.model.ApiTrack;
import com.soundcloud.android.api.model.ApiUser;
import com.soundcloud.android.associations.FollowingStateProvider;
import com.soundcloud.android.associations.FollowingStatuses;
import com.soundcloud.android.likes.LikedStatuses;
import com.soundcloud.android.likes.LikesStateProvider;
import com.soundcloud.android.playlists.PlaylistItem;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import com.soundcloud.android.tracks.TrackItem;
import com.soundcloud.android.users.UserItem;
import com.soundcloud.java.collections.Pair;
import edu.emory.mathcs.backport.java.util.Arrays;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import rx.Observable;
import rx.subjects.BehaviorSubject;

import android.support.annotation.NonNull;

@SuppressWarnings("unchecked")
public class TopResultsLoaderTest extends AndroidUnitTest {

    private static final String QUERY = "query";
    private final BehaviorSubject<LikedStatuses> likesStatuses = BehaviorSubject.create();
    private final BehaviorSubject<FollowingStatuses> followingStatuses = BehaviorSubject.create();
    private TopResultsLoader loader;

    @Mock private TopResultsOperations operations;
    @Mock private LikesStateProvider likesStateProvider;
    @Mock private FollowingStateProvider followingStateProvider;

    private final ApiTrack apiTrack = ModelFixtures.apiTrack();
    private final ApiUser apiUser = ModelFixtures.apiUser();
    private final ApiPlaylist apiPlaylist = ModelFixtures.apiPlaylist();

    @Before
    public void setUp() throws Exception {
        when(likesStateProvider.likedStatuses()).thenReturn(likesStatuses);
        when(followingStateProvider.followingStatuses()).thenReturn(followingStatuses);
        loader = new TopResultsLoader(likesStateProvider, followingStateProvider, operations);

        likesStatuses.onNext(LikedStatuses.create(emptySet()));
        followingStatuses.onNext(FollowingStatuses.create(emptySet()));
    }

    @Test
    public void emitsViewModel() throws Exception {
        ApiTopResultsBucket multipleTypeBucket = TopResultsFixtures.apiTopResultsBucket(searchTrackItem(apiTrack), searchPlaylistItem(apiPlaylist), searchUserItem(apiUser));
        when(operations.search(QUERY, of(TopResultsFixtures.QUERY_URN))).thenReturn(Observable.just(singletonList(multipleTypeBucket)));

        loader.getTopSearchResults(Pair.of(QUERY, of(TopResultsFixtures.QUERY_URN))).test().assertValue(
                getViewModel(getBucketViewModel(TOP_RESULT,
                                                3,
                                                SearchItem.Track.create(getTrackItem(apiTrack)),
                                                SearchItem.Playlist.create(getPlaylistItem(apiPlaylist)),
                                                SearchItem.User.create(getUserItem(apiUser))))
        );
    }

    @Test
    public void viewModelEmitsLikedTracksBucket() throws Exception {
        when(operations.search(QUERY, of(TopResultsFixtures.QUERY_URN))).thenReturn(Observable.just(singletonList(TopResultsFixtures.apiTrackResultsBucket(searchTrackItem(apiTrack)))));
        likesStatuses.onNext(LikedStatuses.create(singleton(apiTrack.getUrn())));

        loader.getTopSearchResults(Pair.of(QUERY, of(TopResultsFixtures.QUERY_URN))).test().assertValue(
                getViewModel(getBucketViewModel(TRACKS, 1, SearchItem.Track.create(getTrackItem(apiTrack, true))))
        );
    }

    @Test
    public void viewModelEmitsLikedPlaylistsBucket() throws Exception {
        when(operations.search(QUERY, of(TopResultsFixtures.QUERY_URN))).thenReturn(Observable.just(singletonList(TopResultsFixtures.apiTopResultsBucket(searchPlaylistItem(apiPlaylist)))));
        likesStatuses.onNext(LikedStatuses.create(singleton(apiPlaylist.getUrn())));

        loader.getTopSearchResults(Pair.of(QUERY, of(TopResultsFixtures.QUERY_URN))).test().assertValue(
                getViewModel(getBucketViewModel(TOP_RESULT, 1, SearchItem.Playlist.create(getPlaylistItem(apiPlaylist, true))))
        );
    }

    @Test
    public void viewModelEmitsLikedTracksBucketAfterUpdate() throws Exception {
        when(operations.search(QUERY, of(TopResultsFixtures.QUERY_URN)))
                .thenReturn(Observable.just(singletonList(TopResultsFixtures.apiTrackResultsBucket(searchTrackItem(apiTrack)))));

        loader.getTopSearchResults(Pair.of(QUERY, of(TopResultsFixtures.QUERY_URN))).test().assertValue(
                getViewModel(getBucketViewModel(TRACKS, 1, SearchItem.Track.create(getTrackItem(apiTrack)))));

        likesStatuses.onNext(LikedStatuses.create(singleton(apiTrack.getUrn())));

        loader.getTopSearchResults(Pair.of(QUERY, of(TopResultsFixtures.QUERY_URN))).test().assertValue(
                getViewModel(getBucketViewModel(TRACKS, 1, SearchItem.Track.create(getTrackItem(apiTrack, true))))
        );
    }

    @NonNull
    private TopResultsBucketViewModel getBucketViewModel(TopResultsBucketViewModel.Kind bucketKind, int totalResults, SearchItem... searchItems) {
        return TopResultsBucketViewModel.create(Arrays.asList(searchItems), bucketKind, totalResults, TopResultsFixtures.QUERY_URN);
    }


    @NonNull
    private TopResultsViewModel getViewModel(TopResultsBucketViewModel... bucketViewModels) {
        return TopResultsViewModel.create(asList(bucketViewModels));
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