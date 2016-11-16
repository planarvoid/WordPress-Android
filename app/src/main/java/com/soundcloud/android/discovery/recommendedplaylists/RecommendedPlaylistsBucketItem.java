package com.soundcloud.android.discovery.recommendedplaylists;

import com.google.auto.value.AutoValue;
import com.soundcloud.android.discovery.DiscoveryItem;
import com.soundcloud.android.playlists.PlaylistItem;
import com.soundcloud.android.model.Urn;
import com.soundcloud.java.optional.Optional;

import java.util.List;

@AutoValue
public abstract class RecommendedPlaylistsBucketItem extends DiscoveryItem {
    public abstract String key();
    abstract String displayName();
    abstract Optional<String> artworkUrl();
    abstract Optional<Urn> queryUrn();
    public abstract List<PlaylistItem> playlists();

    public static RecommendedPlaylistsBucketItem create(RecommendedPlaylistsEntity entity,
                                                        List<PlaylistItem> playlists) {
        return new AutoValue_RecommendedPlaylistsBucketItem(Kind.RecommendedPlaylistsItem,
                                                            entity.key(),
                                                            entity.displayName(),
                                                            entity.artworkUrl(),
                                                            entity.queryUrn(),
                                                            playlists);
    }
}
