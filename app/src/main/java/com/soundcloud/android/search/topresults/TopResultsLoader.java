package com.soundcloud.android.search.topresults;

import static com.soundcloud.java.collections.Lists.transform;

import com.soundcloud.android.associations.FollowingStateProvider;
import com.soundcloud.android.associations.FollowingStatuses;
import com.soundcloud.android.likes.LikedStatuses;
import com.soundcloud.android.likes.LikesStateProvider;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playlists.PlaylistItem;
import com.soundcloud.android.search.ApiUniversalSearchItem;
import com.soundcloud.android.tracks.TrackItem;
import com.soundcloud.android.users.UserItem;
import com.soundcloud.java.collections.Pair;
import com.soundcloud.java.optional.Optional;
import rx.Observable;

import android.support.annotation.NonNull;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;

class TopResultsLoader {

    private final LikesStateProvider likesStateProvider;
    private final FollowingStateProvider followingStateProvider;
    private final TopResultsOperations operations;

    @Inject
    TopResultsLoader(LikesStateProvider likesStateProvider,
                     FollowingStateProvider followingStateProvider,
                     TopResultsOperations operations) {
        this.likesStateProvider = likesStateProvider;
        this.followingStateProvider = followingStateProvider;
        this.operations = operations;
    }


    Observable<TopResultsViewModel> getTopSearchResults(Pair<String, Optional<Urn>> query) {
        return Observable.combineLatest(
                operations.search(query.first(), query.second()),
                likesStateProvider.likedStatuses(),
                followingStateProvider.followingStatuses(),
                this::createResults
        );
    }

    @NonNull
    private TopResultsViewModel createResults(List<ApiTopResultsBucket> apiBuckets, LikedStatuses likedStatuses, FollowingStatuses followingStatuses) {
        return TopResultsViewModel.create(transform(apiBuckets, apiBucket -> {

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
        }));
    }

    private UserItem createUserItem(ApiUniversalSearchItem searchItem, FollowingStatuses followingStatuses) {
        return UserItem.from(searchItem.user().get(), followingStatuses.isFollowed(searchItem.user().get().getUrn()));
    }

    private PlaylistItem createPlaylistItem(ApiUniversalSearchItem searchItem, LikedStatuses likedStatuses) {
        return PlaylistItem.fromLiked(searchItem.playlist().get(), likedStatuses.isLiked(searchItem.playlist().get().getUrn()));
    }

    private TrackItem createTrackItem(ApiUniversalSearchItem searchItem, LikedStatuses likedStatuses) {
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
