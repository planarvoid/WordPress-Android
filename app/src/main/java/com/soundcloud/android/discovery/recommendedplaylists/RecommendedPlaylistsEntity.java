package com.soundcloud.android.discovery.recommendedplaylists;

import com.google.auto.value.AutoValue;
import com.soundcloud.android.model.Urn;
import com.soundcloud.java.optional.Optional;

import java.util.Collections;
import java.util.List;

@AutoValue
public abstract class RecommendedPlaylistsEntity {

    public abstract Long localId();
    public abstract String key();
    public abstract String displayName();
    public abstract Optional<String> artworkUrl();
    public abstract List<Urn> playlistUrns();

    public static RecommendedPlaylistsEntity create(long localId, String key, String displayName, Optional<String> artworkUrl, List<Urn> playlists) {
        return new AutoValue_RecommendedPlaylistsEntity(localId, key, displayName, artworkUrl, playlists);
    }

    public RecommendedPlaylistsEntity copyWithPlaylistUrns(List<Urn> playlists) {
        return new AutoValue_RecommendedPlaylistsEntity(localId(), key(), displayName(), artworkUrl(), playlists);
    }

    public static RecommendedPlaylistsEntity create(long localId, String key, String displayName, Optional<String> artworkUrl) {
        return new AutoValue_RecommendedPlaylistsEntity(localId, key, displayName, artworkUrl, Collections.<Urn>emptyList());
    }
}
