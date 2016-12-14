package com.soundcloud.android.collection;

import com.google.auto.value.AutoValue;
import com.soundcloud.android.collection.recentlyplayed.RecentlyPlayedPlayableItem;
import com.soundcloud.android.playlists.PlaylistItem;
import com.soundcloud.android.stations.StationRecord;
import com.soundcloud.android.tracks.TrackItem;

import java.util.List;

@AutoValue
public abstract class MyCollection {

    static MyCollection forCollectionWithPlayHistory(LikesItem likes, List<PlaylistItem> likedAndPostedPlaylists,
                                                     List<StationRecord> stations,
                                                     List<TrackItem> playHistoryTrackItems,
                                                     List<RecentlyPlayedPlayableItem> recentlyPlayedPlayableItems,
                                                     boolean atLeastOneError) {
        return new AutoValue_MyCollection(likes, likedAndPostedPlaylists, stations, playHistoryTrackItems,
                                          recentlyPlayedPlayableItems, atLeastOneError);
    }

    public abstract LikesItem getLikes();

    public abstract List<PlaylistItem> getPlaylistItems();

    public abstract List<StationRecord> getStations();

    public abstract List<TrackItem> getPlayHistoryTrackItems();

    public abstract List<RecentlyPlayedPlayableItem> getRecentlyPlayedItems();

    public abstract boolean hasError();

}
