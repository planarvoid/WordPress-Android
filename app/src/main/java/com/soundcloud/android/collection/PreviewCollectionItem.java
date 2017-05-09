package com.soundcloud.android.collection;

import com.google.auto.value.AutoValue;
import com.soundcloud.android.offline.OfflineState;
import com.soundcloud.android.playlists.PlaylistItem;
import com.soundcloud.android.presentation.OfflineItem;
import com.soundcloud.android.stations.StationRecord;
import com.soundcloud.java.optional.Optional;

import java.util.List;

@AutoValue
public abstract class PreviewCollectionItem extends CollectionItem implements OfflineItem {

    static PreviewCollectionItem forLikesPlaylistsAndStations(LikesItem likes,
                                                              Optional<List<PlaylistItem>> playlistsAndAlbums,
                                                              Optional<List<PlaylistItem>> playlists,
                                                              Optional<List<PlaylistItem>> albums,
                                                              List<StationRecord> stations) {
        return new AutoValue_PreviewCollectionItem(CollectionItem.TYPE_PREVIEW,
                                                   likes, Optional.of(stations), playlistsAndAlbums, playlists, albums);
    }

    PreviewCollectionItem copyWithLikes(LikesItem likes) {
        return new AutoValue_PreviewCollectionItem(CollectionItem.TYPE_PREVIEW, likes, getStations(), getPlaylistsAndAlbums(), getPlaylists(), getAlbums());
    }

    abstract LikesItem getLikes();

    abstract Optional<List<StationRecord>> getStations();

    abstract Optional<List<PlaylistItem>> getPlaylistsAndAlbums();

    abstract Optional<List<PlaylistItem>> getPlaylists();

    abstract Optional<List<PlaylistItem>> getAlbums();

    @Override
    public PreviewCollectionItem updatedWithOfflineState(OfflineState offlineState) {
        return copyWithLikes(getLikes().offlineState(offlineState));
    }
}
