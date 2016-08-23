package com.soundcloud.android.collection;

import com.google.auto.value.AutoValue;
import com.soundcloud.android.collection.recentlyplayed.RecentlyPlayedPlayableItem;
import com.soundcloud.android.playlists.PlaylistItem;
import com.soundcloud.android.stations.StationRecord;
import com.soundcloud.android.tracks.TrackItem;

import java.util.Collections;
import java.util.List;

@AutoValue
public abstract class MyCollection {

    static MyCollection forCollectionWithPlaylists(LikesItem likes, List<PlaylistItem> likedAndPostedPlaylists,
                                                   List<StationRecord> recentStations, boolean atLeastOneError) {
        return new AutoValue_MyCollection(likes,
                                          likedAndPostedPlaylists,
                                          recentStations,
                                          Collections.<TrackItem>emptyList(),
                                          Collections.<RecentlyPlayedPlayableItem>emptyList(),
                                          atLeastOneError);
    }

    static MyCollection forCollectionWithPlayHistory(LikesItem likes, List<PlaylistItem> likedAndPostedPlaylists,
                                                     List<TrackItem> playHistoryTrackItems,
                                                     List<RecentlyPlayedPlayableItem> recentlyPlayedPlayableItems,
                                                     boolean atLeastOneError) {
        return new AutoValue_MyCollection(likes, likedAndPostedPlaylists, Collections.<StationRecord>emptyList(),
                                          playHistoryTrackItems, recentlyPlayedPlayableItems, atLeastOneError);
    }

    public abstract LikesItem getLikes();

    public abstract List<PlaylistItem> getPlaylistItems();

    public abstract List<StationRecord> getRecentStations();

    public abstract List<TrackItem> getPlayHistoryTrackItems();

    public abstract List<RecentlyPlayedPlayableItem> getRecentlyPlayedItems();

    public abstract boolean hasError();

}
