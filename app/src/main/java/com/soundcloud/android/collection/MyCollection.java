package com.soundcloud.android.collection;

import com.google.auto.value.AutoValue;
import com.soundcloud.android.playlists.PlaylistItem;
import com.soundcloud.android.stations.StationRecord;
import com.soundcloud.android.tracks.TrackItem;

import java.util.Collections;
import java.util.List;

@AutoValue
public abstract class MyCollection {

    static MyCollection forCollectionWithPlaylists(LikesItem likes, List<PlaylistItem> likedAndPostedPlaylists,
                                                   List<StationRecord> recentStations, boolean atLeastOneError) {
        return new AutoValue_MyCollection(likes, likedAndPostedPlaylists, recentStations,
                Collections.<TrackItem>emptyList(), Collections.<RecentlyPlayedItem>emptyList(), atLeastOneError);
    }

    static MyCollection forCollectionWithPlayHistory(LikesItem likes, List<PlaylistItem> likedAndPostedPlaylists,
                                                     List<TrackItem> playHistoryTrackItems,
                                                     List<RecentlyPlayedItem> recentlyPlayedItems,
                                                     boolean atLeastOneError) {
        return new AutoValue_MyCollection(likes, likedAndPostedPlaylists, Collections.<StationRecord>emptyList(),
                playHistoryTrackItems, recentlyPlayedItems, atLeastOneError);
    }

    public abstract LikesItem getLikes();

    public abstract List<PlaylistItem> getPlaylistItems();

    abstract List<StationRecord> getRecentStations();

    abstract List<TrackItem> getPlayHistoryTrackItems();

    abstract List<RecentlyPlayedItem> getRecentlyPlayedItems();

    abstract boolean hasError();

}
