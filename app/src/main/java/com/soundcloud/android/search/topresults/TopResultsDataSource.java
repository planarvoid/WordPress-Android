package com.soundcloud.android.search.topresults;

import com.soundcloud.android.api.model.ApiPlaylist;
import com.soundcloud.android.api.model.ApiTrack;
import com.soundcloud.android.api.model.ApiUser;
import com.soundcloud.android.associations.FollowingStateProvider;
import com.soundcloud.android.associations.FollowingStatuses;
import com.soundcloud.android.likes.LikedStatuses;
import com.soundcloud.android.likes.LikesStateProvider;
import com.soundcloud.android.main.Screen;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.PlaySessionStateProvider;
import com.soundcloud.android.playback.TrackSourceInfo;
import com.soundcloud.android.playlists.PlaylistItem;
import com.soundcloud.android.presentation.EntityItemCreator;
import com.soundcloud.android.search.ApiUniversalSearchItem;
import com.soundcloud.android.tracks.TrackItem;
import com.soundcloud.android.users.UserItem;
import com.soundcloud.android.utils.collection.CollectionLoader;
import com.soundcloud.android.utils.collection.CollectionLoaderState;
import com.soundcloud.android.view.collection.CollectionRendererState;
import com.soundcloud.java.collections.Lists;
import com.soundcloud.java.optional.Optional;
import com.soundcloud.java.strings.Strings;
import rx.Observable;
import rx.Subscription;
import rx.subjects.BehaviorSubject;

import javax.inject.Inject;
import java.util.List;

public class TopResultsDataSource {
    private final TopResultsOperations operations;
    private final LikesStateProvider likedStatuses;
    private final FollowingStateProvider followingStatuses;
    private final PlaySessionStateProvider playSessionStateProvider;
    private final EntityItemCreator entityItemCreator;

    private final BehaviorSubject<TopResultsViewModel> viewModel = BehaviorSubject.create();

    @Inject
    public TopResultsDataSource(TopResultsOperations operations,
                                LikesStateProvider likedStatuses,
                                FollowingStateProvider followingStatuses,
                                PlaySessionStateProvider playSessionStateProvider,
                                EntityItemCreator entityItemCreator) {
        this.operations = operations;
        this.likedStatuses = likedStatuses;
        this.followingStatuses = followingStatuses;
        this.playSessionStateProvider = playSessionStateProvider;
        this.entityItemCreator = entityItemCreator;
    }

    Observable<TopResultsViewModel> viewModel() {
        return viewModel;
    }

    Observable<Optional<Urn>> queryUrn() {
        return viewModel.map(TopResultsViewModel::queryUrn).filter(Optional::isPresent);
    }

    public Subscription subscribe(Observable<SearchParams> initialSearch, Observable<SearchParams> refresh) {
        final CollectionLoader<TopResults, SearchParams> loader = new CollectionLoader<>(
                initialSearch,
                operations::search,
                refresh,
                operations::search);

        return Observable.combineLatest(loader.pages(),
                                        likedStatuses.likedStatuses(),
                                        followingStatuses.followingStatuses(),
                                        playSessionStateProvider.nowPlayingUrn(),
                                        this::transformBucketsToViewModel)
                         .subscribe(viewModel);
    }


    private TopResultsViewModel transformBucketsToViewModel(
            CollectionLoaderState<TopResults> collectionLoaderState,
            LikedStatuses likedStatuses,
            FollowingStatuses followingStatuses,
            Urn nowPlayingUrn) {

        Optional<Urn> queryUrn = Optional.absent();
        final List<TopResultsBucketViewModel> viewModelItems = Lists.newArrayList();
        if (collectionLoaderState.data().isPresent()) {
            final TopResults topResults = collectionLoaderState.data().get();
            final List<TopResults.Bucket> buckets = topResults.buckets();
            queryUrn = topResults.queryUrn();
            for (TopResults.Bucket bucket : buckets) {
                final List<ApiUniversalSearchItem> apiUniversalSearchItems = bucket.items();
                final int bucketPosition = buckets.indexOf(bucket);
                final List<SearchItem> result = transformApiSearchItems(entityItemCreator, likedStatuses, followingStatuses, nowPlayingUrn, apiUniversalSearchItems, bucketPosition, bucket.kind());
                viewModelItems.add(TopResultsBucketViewModel.create(result, bucket.kind(), bucket.totalResults()));
            }
        }
        return TopResultsViewModel.create(queryUrn, CollectionRendererState.create(collectionLoaderState.collectionLoadingState(), viewModelItems));
    }

    private List<SearchItem> transformApiSearchItems(EntityItemCreator entityItemCreator,
                                                     LikedStatuses likedStatuses,
                                                     FollowingStatuses followingStatuses,
                                                     Urn nowPlayingUrn,
                                                     List<ApiUniversalSearchItem> apiUniversalSearchItems,
                                                     int bucketPosition,
                                                     TopResults.Bucket.Kind kind) {
        return Lists.transform(apiUniversalSearchItems, item -> transformApiSearchItem(entityItemCreator, likedStatuses, followingStatuses, nowPlayingUrn, item, bucketPosition, kind));
    }

    private SearchItem transformApiSearchItem(EntityItemCreator entityItemCreator,
                                              LikedStatuses likedStatuses,
                                              FollowingStatuses followingStatuses,
                                              Urn nowPlayingUrn,
                                              ApiUniversalSearchItem searchItem,
                                              int bucketPosition,
                                              TopResults.Bucket.Kind kind) {
        if (searchItem.track().isPresent()) {
            final TrackSourceInfo trackSourceInfo = getTrackSourceInfo(kind);
            ApiTrack apiTrack = searchItem.track().get();
            TrackItem trackItem = entityItemCreator.trackItem(apiTrack).updateLikeState(likedStatuses.isLiked(apiTrack.getUrn())).updateNowPlaying(nowPlayingUrn);
            return SearchItem.Track.create(trackItem, bucketPosition, trackSourceInfo);
        } else if (searchItem.playlist().isPresent()) {
            final ApiPlaylist apiPlaylist = searchItem.playlist().get();
            PlaylistItem playlistItem = entityItemCreator.playlistItem(apiPlaylist).updateLikeState(likedStatuses.isLiked(apiPlaylist.getUrn()));
            return SearchItem.Playlist.create(playlistItem, bucketPosition, kind.toClickSource());
        } else if (searchItem.user().isPresent()) {
            final ApiUser apiUser = searchItem.user().get();
            UserItem userItem = entityItemCreator.userItem(apiUser).copyWithFollowing(followingStatuses.isFollowed(apiUser.getUrn()));
            return SearchItem.User.create(userItem, bucketPosition, kind.toClickSource());
        }
        throw new IllegalArgumentException("ApiSearchItem has to contain either track or playlist or user");
    }

    private static TrackSourceInfo getTrackSourceInfo(TopResults.Bucket.Kind kind) {
        final TrackSourceInfo trackSourceInfo = new TrackSourceInfo(Screen.SEARCH_EVERYTHING.get(), true);
        trackSourceInfo.setSource(kind.toClickSource().key, Strings.EMPTY);
        return trackSourceInfo;
    }
}
