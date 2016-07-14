package com.soundcloud.android.collection;

import com.google.auto.value.AutoValue;
import com.soundcloud.android.playlists.PlaylistItem;
import com.soundcloud.android.presentation.ListItem;
import com.soundcloud.android.stations.StationRecord;
import com.soundcloud.java.collections.PropertySet;
import com.soundcloud.java.optional.Optional;

import java.util.List;

@AutoValue
public abstract class PreviewCollectionItem extends CollectionItem {

    public static CollectionItem forLikesAndPlaylists(LikesItem likes, List<PlaylistItem> playlistItems) {
        return new AutoValue_PreviewCollectionItem(CollectionItem.TYPE_PREVIEW,
                                                   likes,
                                                   Optional.<List<StationRecord>>absent(),
                                                   Optional.of(playlistItems));
    }

    static PreviewCollectionItem forLikesAndStations(LikesItem likes, List<StationRecord> stations) {
        return new AutoValue_PreviewCollectionItem(CollectionItem.TYPE_PREVIEW,
                                                   likes, Optional.of(stations), Optional.<List<PlaylistItem>>absent());
    }

    abstract LikesItem getLikes();

    abstract Optional<List<StationRecord>> getStations();

    abstract Optional<List<PlaylistItem>> getPlaylists();

    @Override
    public ListItem update(PropertySet sourceSet) {
        getLikes().update(sourceSet);
        return this;
    }
}
