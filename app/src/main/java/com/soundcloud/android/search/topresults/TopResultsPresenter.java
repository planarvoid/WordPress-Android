package com.soundcloud.android.search.topresults;

import static com.soundcloud.java.collections.Iterables.filter;

import com.soundcloud.android.analytics.SearchQuerySourceInfo;
import com.soundcloud.android.associations.FollowingStateProvider;
import com.soundcloud.android.associations.FollowingStatuses;
import com.soundcloud.android.likes.LikedStatuses;
import com.soundcloud.android.likes.LikesStateProvider;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playlists.PlaylistItem;
import com.soundcloud.android.search.ApiUniversalSearchItem;
import com.soundcloud.android.search.SearchPlayQueueFilter;
import com.soundcloud.android.tracks.TrackItem;
import com.soundcloud.android.users.UserItem;
import com.soundcloud.android.view.adapters.CollectionLoader;
import com.soundcloud.android.view.adapters.CollectionViewState;
import com.soundcloud.java.collections.Iterables;
import com.soundcloud.java.collections.Lists;
import com.soundcloud.java.collections.Pair;
import com.soundcloud.java.functions.Function;
import com.soundcloud.java.optional.Optional;
import rx.Observable;
import rx.Subscription;
import rx.subjects.BehaviorSubject;
import rx.subjects.PublishSubject;
import rx.subscriptions.CompositeSubscription;

import android.support.annotation.NonNull;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;

public class TopResultsPresenter {

    interface TopResultsView {

        Observable<Pair<String, Optional<Urn>>> searchIntent();

        Observable<Void> refreshIntent();
    }

    private final BehaviorSubject<TopResultsViewModel> viewModel = BehaviorSubject.create();
    private final PublishSubject<SearchItem> searchItemClicked = PublishSubject.create();

    private final Subscription loaderSubscription;
    private CompositeSubscription viewSubscription = new CompositeSubscription();
    private final SearchPlayQueueFilter playQueueFilter;

    private final BehaviorSubject<Pair<String, Optional<Urn>>> firstPageIntent = BehaviorSubject.create();
    private final BehaviorSubject<Pair<String, Optional<Urn>>> refreshIntent = BehaviorSubject.create();

    @Inject
    TopResultsPresenter(TopResultsOperations operations,
                        SearchPlayQueueFilter playQueueFilter,
                        LikesStateProvider likedStatuses,
                        FollowingStateProvider followingStatuses) {
        this.playQueueFilter = playQueueFilter;

        CollectionLoader<ApiTopResultsBucket, Pair<String, Optional<Urn>>> loader = new CollectionLoader<>(
                firstPageIntent,
                operations::search,
                refreshIntent,
                operations::search);

        loaderSubscription = Observable.combineLatest(loader.pages(),
                                                      likedStatuses.likedStatuses(),
                                                      followingStatuses.followingStatuses(),
                                                      this::transformBucketToViewModel)
                                       .subscribe(viewModel);
    }

    public PublishSubject<SearchItem> searchItemClicked() {
        return searchItemClicked;
    }

    public Observable<TrackItemClick> trackItemClicked() {
        return searchItemClicked.filter(clickedItem -> clickedItem.kind() == SearchItem.Kind.TRACK)
                                .map(this::addSearchQuery)
                                .map(this::toTrackItemClick);
    }

    public Observable<PlaylistItemClick> playlistItemClicked() {
        return searchItemClicked.filter(clickedItem -> clickedItem.kind() == SearchItem.Kind.PLAYLIST)
                                .map(this::addSearchQuery)
                                .map(this::toPlaylistItemClick);
    }

    public Observable<UserItemClick> userItemClicked() {
        return searchItemClicked.filter(clickedItem -> clickedItem.kind() == SearchItem.Kind.USER)
                                .map(this::addSearchQuery)
                                .map(this::toUserItemClick)
                                .doOnError(Throwable::printStackTrace);
    }

    private Pair<SearchItem, String> addSearchQuery(SearchItem searchItem) {
        return Pair.of(searchItem, firstPageIntent.getValue().first());
    }

    void attachView(TopResultsView topResultsView) {
        viewSubscription = new CompositeSubscription();

        viewSubscription.addAll(
                topResultsView.searchIntent()
                        .subscribe(firstPageIntent),
                topResultsView.refreshIntent()
                              .flatMap(ignored -> topResultsView.searchIntent())
                              .subscribe(refreshIntent)
        );
    }

    @NonNull
    private TopResultsViewModel transformBucketToViewModel(
            CollectionViewState<ApiTopResultsBucket> collectionViewState,
            LikedStatuses likedStatuses,
            FollowingStatuses followingStatuses) {

        return TopResultsViewModel.create(collectionViewState.withNewType(transformBucket(collectionViewState, likedStatuses, followingStatuses)));
    }

    void detachView() {
        viewSubscription.unsubscribe();
        loaderSubscription.unsubscribe();
    }

    BehaviorSubject<TopResultsViewModel> viewModel() {
        return viewModel;
    }


    @NonNull
    static Function<ApiTopResultsBucket, TopResultsBucketViewModel> transformBucket(CollectionViewState<ApiTopResultsBucket> collectionViewState,
                                                                                    LikedStatuses likedStatuses,
                                                                                    FollowingStatuses followingStatuses) {
        return apiBucket -> {
            List<ApiUniversalSearchItem> apiUniversalSearchItems = apiBucket.collection().getCollection();
            List<SearchItem> result = new ArrayList<>(apiUniversalSearchItems.size());
            int index = collectionViewState.items().indexOf(apiBucket);

            for (ApiUniversalSearchItem searchItem : apiUniversalSearchItems) {
                if (searchItem.track().isPresent()) {
                    result.add(SearchItem.Track.create(createTrackItem(searchItem, likedStatuses), index));

                } else if (searchItem.playlist().isPresent()) {
                    result.add(SearchItem.Playlist.create(createPlaylistItem(searchItem, likedStatuses), index));

                } else if (searchItem.user().isPresent()) {
                    result.add(SearchItem.User.create(createUserItem(searchItem, followingStatuses), index));
                }
            }
            return TopResultsBucketViewModel.create(result, apiBucket.urn(), apiBucket.totalResults(), apiBucket.queryUrn().or(Urn.NOT_SET));
        };
    }

    private static UserItem createUserItem(ApiUniversalSearchItem searchItem, FollowingStatuses followingStatuses) {
        return UserItem.from(searchItem.user().get(), followingStatuses.isFollowed(searchItem.user().get().getUrn()));
    }

    private static PlaylistItem createPlaylistItem(ApiUniversalSearchItem searchItem, LikedStatuses likedStatuses) {
        return PlaylistItem.fromLiked(searchItem.playlist().get(), likedStatuses.isLiked(searchItem.playlist().get().getUrn()));
    }

    private static TrackItem createTrackItem(ApiUniversalSearchItem searchItem, LikedStatuses likedStatuses) {
        return TrackItem.fromLiked(searchItem.track().get(), likedStatuses.isLiked(searchItem.track().get().getUrn()));
    }

    private TrackItemClick toTrackItemClick(Pair<SearchItem, String> data) {
        final SearchItem.Track clickedItem = (SearchItem.Track) data.first();

        final List<TopResultsBucketViewModel> buckets = viewModel.getValue().buckets().items();
        final int bucketPosition = clickedItem.bucketPosition();
        final TopResultsBucketViewModel currentBucket = buckets.get(bucketPosition);
        final SearchQuerySourceInfo searchQuerySourceInfo = new SearchQuerySourceInfo(currentBucket.queryUrn(), bucketPosition, clickedItem.urn(), data.second());
        final List<TrackItem> tracks = Lists.newArrayList(Iterables.transform(filter(currentBucket.items(), SearchItem.Track.class), SearchItem.Track::trackItem));
        final int position = tracks.indexOf(clickedItem.trackItem());
        return TrackItemClick.create(searchQuerySourceInfo, playQueueFilter.correctQueue(tracks, position), playQueueFilter.correctPosition(position));
    }

    private PlaylistItemClick toPlaylistItemClick(Pair<SearchItem, String> data) {
        final SearchItem.Playlist clickedItem = (SearchItem.Playlist) data.first();
        final SearchQuerySourceInfo searchQuerySourceInfo = new SearchQuerySourceInfo(viewModel.getValue().buckets().items().get(clickedItem.bucketPosition()).queryUrn(),
                                                                                      clickedItem.bucketPosition(),
                                                                                      clickedItem.urn(),
                                                                                      data.second());
        return PlaylistItemClick.create(searchQuerySourceInfo, clickedItem.playlistItem());
    }

    private UserItemClick toUserItemClick(Pair<SearchItem, String> data) {
        final SearchItem.User clickedItem = (SearchItem.User) data.first();
        final SearchQuerySourceInfo searchQuerySourceInfo = new SearchQuerySourceInfo(viewModel.getValue().buckets().items().get(clickedItem.bucketPosition()).queryUrn(),
                                                                                      clickedItem.bucketPosition(),
                                                                                      clickedItem.urn(),
                                                                                      data.second());
        return UserItemClick.create(searchQuerySourceInfo, clickedItem.userItem());
    }

}
