package com.soundcloud.android.search.topresults;

import com.soundcloud.android.api.model.ApiPlaylist;
import com.soundcloud.android.api.model.ApiTrack;
import com.soundcloud.android.api.model.ApiUser;
import com.soundcloud.android.associations.FollowingStatuses;
import com.soundcloud.android.likes.LikedStatuses;
import com.soundcloud.android.main.Screen;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.TrackSourceInfo;
import com.soundcloud.android.playlists.PlaylistItem;
import com.soundcloud.android.presentation.EntityItemCreator;
import com.soundcloud.android.search.ApiUniversalSearchItem;
import com.soundcloud.android.tracks.TrackItem;
import com.soundcloud.android.users.UserItem;
import com.soundcloud.java.collections.Lists;
import com.soundcloud.java.strings.Strings;

import java.util.List;

public class SearchItemHelper {

    private SearchItemHelper() {
    }

    static List<SearchItem> transformApiSearchItems(EntityItemCreator entityItemCreator,
                                                    LikedStatuses likedStatuses,
                                                    FollowingStatuses followingStatuses,
                                                    Urn nowPlayingUrn,
                                                    List<ApiUniversalSearchItem> apiUniversalSearchItems,
                                                    int bucketPosition,
                                                    TopResults.Bucket.Kind kind) {
        return Lists.transform(apiUniversalSearchItems, item -> transformApiSearchItem(entityItemCreator, likedStatuses, followingStatuses, nowPlayingUrn, item, bucketPosition, kind));
    }

    static SearchItem transformApiSearchItem(EntityItemCreator entityItemCreator,
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
