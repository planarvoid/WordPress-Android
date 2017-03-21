package com.soundcloud.android.search.topresults;

import com.soundcloud.android.api.model.ApiPlaylist;
import com.soundcloud.android.api.model.ApiTrack;
import com.soundcloud.android.api.model.ApiUser;
import com.soundcloud.android.associations.FollowingStatuses;
import com.soundcloud.android.likes.LikedStatuses;
import com.soundcloud.android.playlists.PlaylistItem;
import com.soundcloud.android.presentation.EntityItemCreator;
import com.soundcloud.android.search.ApiUniversalSearchItem;
import com.soundcloud.android.tracks.TrackItem;
import com.soundcloud.android.users.UserItem;
import com.soundcloud.java.collections.Lists;

import java.util.List;

public class SearchItemHelper {

    private SearchItemHelper() {
    }

    static List<SearchItem> transformApiSearchItems(EntityItemCreator entityItemCreator,
                                                    LikedStatuses likedStatuses,
                                                    FollowingStatuses followingStatuses,
                                                    List<ApiUniversalSearchItem> apiUniversalSearchItems,
                                                    int bucketPosition) {
        return Lists.transform(apiUniversalSearchItems, item -> transformApiSearchItem(entityItemCreator, likedStatuses, followingStatuses, item, bucketPosition));
    }

    static SearchItem transformApiSearchItem(EntityItemCreator entityItemCreator,
                                             LikedStatuses likedStatuses,
                                             FollowingStatuses followingStatuses,
                                             ApiUniversalSearchItem searchItem,
                                             int bucketPosition) {
        if (searchItem.track().isPresent()) {

            ApiTrack apiTrack = searchItem.track().get();
            TrackItem trackItem = entityItemCreator.trackItem(apiTrack).updateLikeState(likedStatuses.isLiked(apiTrack.getUrn()));
            return SearchItem.Track.create(trackItem, bucketPosition);

        } else if (searchItem.playlist().isPresent()) {

            final ApiPlaylist apiPlaylist = searchItem.playlist().get();
            PlaylistItem playlistItem = entityItemCreator.playlistItem(apiPlaylist).updateLikeState(likedStatuses.isLiked(apiPlaylist.getUrn()));
            return SearchItem.Playlist.create(playlistItem, bucketPosition);

        } else if (searchItem.user().isPresent()) {
            final ApiUser apiUser = searchItem.user().get();
            UserItem userItem = entityItemCreator.userItem(apiUser).copyWithFollowing(followingStatuses.isFollowed(apiUser.getUrn()));
            return SearchItem.User.create(userItem, bucketPosition);

        }
        throw new IllegalArgumentException("ApiSearchItem has to contain either track or playlist or user");
    }

}
