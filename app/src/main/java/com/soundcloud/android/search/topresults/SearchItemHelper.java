package com.soundcloud.android.search.topresults;

import com.soundcloud.android.api.model.ApiPlaylist;
import com.soundcloud.android.api.model.ApiTrack;
import com.soundcloud.android.api.model.ApiUser;
import com.soundcloud.android.associations.FollowingStatuses;
import com.soundcloud.android.likes.LikedStatuses;
import com.soundcloud.android.playlists.PlaylistItem;
import com.soundcloud.android.search.ApiUniversalSearchItem;
import com.soundcloud.android.tracks.TrackItem;
import com.soundcloud.android.users.UserItem;
import com.soundcloud.java.collections.Lists;

import java.util.List;

public class SearchItemHelper {

    private SearchItemHelper() {
    }

    static List<SearchItem> transformApiSearchItems(LikedStatuses likedStatuses,
                                             FollowingStatuses followingStatuses,
                                             List<ApiUniversalSearchItem> apiUniversalSearchItems,
                                             int bucketPosition) {
        return Lists.transform(apiUniversalSearchItems, item -> transformApiSearchItem(likedStatuses, followingStatuses, item, bucketPosition));
    }

    static SearchItem transformApiSearchItem(LikedStatuses likedStatuses,
                                      FollowingStatuses followingStatuses,
                                      ApiUniversalSearchItem searchItem,
                                      int bucketPosition) {
        if (searchItem.track().isPresent()) {
            return SearchItem.Track.create(createTrackItem(searchItem, likedStatuses), bucketPosition);

        } else if (searchItem.playlist().isPresent()) {
            return SearchItem.Playlist.create(createPlaylistItem(searchItem, likedStatuses), bucketPosition);

        } else if (searchItem.user().isPresent()) {
            return SearchItem.User.create(createUserItem(searchItem, followingStatuses), bucketPosition);
        }
        throw new IllegalArgumentException("ApiSearchItem has to contain either track or playlist or user");
    }

    private static UserItem createUserItem(ApiUniversalSearchItem searchItem, FollowingStatuses followingStatuses) {
        final ApiUser apiUser = searchItem.user().get();
        return UserItem.from(apiUser, followingStatuses.isFollowed(apiUser.getUrn()));
    }

    private static PlaylistItem createPlaylistItem(ApiUniversalSearchItem searchItem, LikedStatuses likedStatuses) {
        final ApiPlaylist apiPlaylist = searchItem.playlist().get();
        return PlaylistItem.fromLiked(apiPlaylist, likedStatuses.isLiked(apiPlaylist.getUrn()));
    }

    private static TrackItem createTrackItem(ApiUniversalSearchItem searchItem, LikedStatuses likedStatuses) {
        final ApiTrack apiTrack = searchItem.track().get();
        return TrackItem.fromLiked(apiTrack, likedStatuses.isLiked(apiTrack.getUrn()));
    }
}
