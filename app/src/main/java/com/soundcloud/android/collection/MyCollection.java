package com.soundcloud.android.collection;

import com.google.auto.value.AutoValue;
import com.soundcloud.android.collection.recentlyplayed.RecentlyPlayedPlayableItem;
import com.soundcloud.android.playlists.PlaylistItem;
import com.soundcloud.android.stations.StationRecord;
import com.soundcloud.android.tracks.TrackItem;
import com.soundcloud.java.optional.Optional;

import java.util.List;

@AutoValue
public abstract class MyCollection {

    static MyCollection forCollectionWithPlayHistory(LikesItem likes, List<PlaylistItem> likedAndPostedPlaylists,
                                                     List<StationRecord> stations,
                                                     List<TrackItem> playHistoryTrackItems,
                                                     List<RecentlyPlayedPlayableItem> recentlyPlayedPlayableItems,
                                                     boolean atLeastOneError) {
        return new AutoValue_MyCollection(likes, Optional.of(likedAndPostedPlaylists), Optional.absent(), Optional.absent(),
                                          stations, playHistoryTrackItems,
                                          recentlyPlayedPlayableItems, atLeastOneError);
    }

    static MyCollection forCollectionWithPlayHistoryAndSeparatedAlbums(LikesItem likes, List<PlaylistItem> playlists, List<PlaylistItem> albums,
                                                     List<StationRecord> stations,
                                                     List<TrackItem> playHistoryTrackItems,
                                                     List<RecentlyPlayedPlayableItem> recentlyPlayedPlayableItems,
                                                     boolean atLeastOneError) {
        return new AutoValue_MyCollection(likes, Optional.absent(), Optional.of(playlists), Optional.of(albums),
                                          stations, playHistoryTrackItems,
                                          recentlyPlayedPlayableItems, atLeastOneError);
    }

    public abstract LikesItem getLikes();

    public abstract Optional<List<PlaylistItem>> getPlaylistAndAlbums();

    public abstract Optional<List<PlaylistItem>> getPlaylists();

    public abstract Optional<List<PlaylistItem>> getAlbums();

    public abstract List<StationRecord> getStations();

    public abstract List<TrackItem> getPlayHistoryTrackItems();

    public abstract List<RecentlyPlayedPlayableItem> getRecentlyPlayedItems();

    public abstract boolean hasError();

}
