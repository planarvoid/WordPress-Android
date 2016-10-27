package com.soundcloud.android.discovery.recommendedplaylists;

import com.google.auto.value.AutoValue;
import com.soundcloud.android.discovery.DiscoveryItem;

@AutoValue
public abstract class RecommendedPlaylistsBucketItem extends DiscoveryItem {

    public static RecommendedPlaylistsBucketItem create(RecommendedPlaylists playlists) {
        return new AutoValue_RecommendedPlaylistsBucketItem(Kind.RecommendedPlaylistsItem, playlists);
    }

    abstract RecommendedPlaylists recommendedPlaylists();
}
