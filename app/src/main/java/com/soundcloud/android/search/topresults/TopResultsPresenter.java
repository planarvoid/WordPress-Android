package com.soundcloud.android.search.topresults;

import com.soundcloud.android.associations.FollowingStateProvider;
import com.soundcloud.android.associations.FollowingStatuses;
import com.soundcloud.android.likes.LikedStatuses;
import com.soundcloud.android.likes.LikesStateProvider;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playlists.PlaylistItem;
import com.soundcloud.android.search.ApiUniversalSearchItem;
import com.soundcloud.android.tracks.TrackItem;
import com.soundcloud.android.users.UserItem;
import com.soundcloud.android.view.adapters.CollectionLoader;
import com.soundcloud.android.view.adapters.CollectionViewState;
import com.soundcloud.java.collections.Pair;
import com.soundcloud.java.functions.Function;
import com.soundcloud.java.optional.Optional;
import rx.Observable;
import rx.subjects.BehaviorSubject;
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

    private CompositeSubscription viewSubscription = new CompositeSubscription();

    private final BehaviorSubject<Pair<String, Optional<Urn>>> firstPageIntent = BehaviorSubject.create();
    private final BehaviorSubject<Pair<String, Optional<Urn>>> refreshIntent = BehaviorSubject.create();

    @Inject
    TopResultsPresenter(TopResultsOperations operations, LikesStateProvider likedStatuses, FollowingStateProvider followingStatuses) {

        CollectionLoader<ApiTopResultsBucket, Pair<String, Optional<Urn>>> loader = new CollectionLoader<>(
                firstPageIntent,
                operations::search,
                refreshIntent,
                operations::search);

        Observable.combineLatest(loader.pages(),
                                 likedStatuses.likedStatuses(),
                                 followingStatuses.followingStatuses(),
                                 this::transformBucketToViewModel)
                  .subscribe(viewModel);
    }

    void attachView(TopResultsView topResultsView) {
        viewSubscription = new CompositeSubscription();

        Observable<Pair<String, Optional<Urn>>> cachedSearchIntent = topResultsView.searchIntent().cache();
        viewSubscription.addAll(
                cachedSearchIntent
                        .subscribe(firstPageIntent),
                topResultsView.refreshIntent()
                              .flatMap(ignored -> cachedSearchIntent)
                              .subscribe(refreshIntent)
        );
    }

    @NonNull
    private TopResultsViewModel transformBucketToViewModel(
            CollectionViewState<ApiTopResultsBucket> collectionViewState,
            LikedStatuses likedStatuses,
            FollowingStatuses followingStatuses) {

        return TopResultsViewModel.create(collectionViewState.withNewType(transformBucket(likedStatuses, followingStatuses)));
    }

    void detachView() {
        viewSubscription.unsubscribe();
    }

    Observable<TopResultsViewModel> viewModel() {
        return viewModel;
    }

    @NonNull
    static Function<ApiTopResultsBucket, TopResultsBucketViewModel> transformBucket(LikedStatuses likedStatuses, FollowingStatuses followingStatuses) {
        return apiBucket -> {

            List<ApiUniversalSearchItem> apiUniversalSearchItems = apiBucket.collection().getCollection();
            List<SearchItem> result = new ArrayList<>(apiUniversalSearchItems.size());

            for (ApiUniversalSearchItem searchItem : apiUniversalSearchItems) {
                if (searchItem.track().isPresent()) {
                    result.add(SearchItem.Track.create(createTrackItem(searchItem, likedStatuses)));

                } else if (searchItem.playlist().isPresent()) {
                    result.add(SearchItem.Playlist.create(createPlaylistItem(searchItem, likedStatuses)));

                } else if (searchItem.user().isPresent()) {
                    result.add(SearchItem.User.create(createUserItem(searchItem, followingStatuses)));
                }
            }
            return TopResultsBucketViewModel.create(result, urnToKind(apiBucket.urn()), apiBucket.totalResults(), apiBucket.queryUrn().or(Urn.NOT_SET));
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


    private static TopResultsBucketViewModel.Kind urnToKind(Urn urn) {
        switch (urn.toString()) {
            case "soundcloud:search-buckets:tracks":
                return TopResultsBucketViewModel.Kind.TRACKS;
            case "soundcloud:search-buckets:top":
                return TopResultsBucketViewModel.Kind.TOP_RESULT;
            default:
                throw new IllegalArgumentException("Unexpected urn type for search");
        }
    }
}
