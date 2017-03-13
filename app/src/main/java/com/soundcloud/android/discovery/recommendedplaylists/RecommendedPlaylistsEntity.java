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
    public abstract Optional<Urn> queryUrn();
    public abstract List<Urn> playlistUrns();

    public static RecommendedPlaylistsEntity create(long localId, String key, String displayName, Optional<String> artworkUrl, Optional<Urn> queryUrn, List<Urn> playlists) {
        return new AutoValue_RecommendedPlaylistsEntity(localId, key, displayName, artworkUrl, queryUrn, playlists);
    }

    public RecommendedPlaylistsEntity copyWithPlaylistUrns(List<Urn> playlists) {
        return new AutoValue_RecommendedPlaylistsEntity(localId(), key(), displayName(), artworkUrl(), queryUrn(), playlists);
    }

    public static RecommendedPlaylistsEntity create(long localId, String key, String displayName, Optional<String> artworkUrl, Optional<Urn> queryUrn) {
        return new AutoValue_RecommendedPlaylistsEntity(localId, key, displayName, artworkUrl, queryUrn, Collections.emptyList());
    }
}
