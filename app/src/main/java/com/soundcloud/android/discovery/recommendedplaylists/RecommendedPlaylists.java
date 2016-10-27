package com.soundcloud.android.discovery.recommendedplaylists;

import com.google.auto.value.AutoValue;
import com.soundcloud.android.playlists.PlaylistItem;
import com.soundcloud.java.optional.Optional;

import java.util.List;

@AutoValue
public abstract class RecommendedPlaylists {

    abstract String key();
    abstract String displayName();
    abstract Optional<String> artworkUrl();
    abstract List<PlaylistItem> playlists();

    public static RecommendedPlaylists create(String key, String displayName, Optional<String> artworkUrl, List<PlaylistItem> playlists) {
        return new AutoValue_RecommendedPlaylists(key, displayName, artworkUrl, playlists);
    }
}
