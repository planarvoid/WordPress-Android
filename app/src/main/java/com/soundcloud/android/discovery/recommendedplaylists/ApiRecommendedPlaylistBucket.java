package com.soundcloud.android.discovery.recommendedplaylists;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import com.soundcloud.android.api.model.ApiPlaylist;
import com.soundcloud.android.api.model.ModelCollection;
import com.soundcloud.java.optional.Optional;

import java.util.List;

@AutoValue
public abstract class ApiRecommendedPlaylistBucket {

    public abstract String key();
    public abstract String displayName();
    public abstract Optional<String> artworkUrl();
    public abstract List<ApiPlaylist> playlists();

    @JsonCreator
    public static ApiRecommendedPlaylistBucket create(@JsonProperty("key") String key,
                                                      @JsonProperty("display_name") String displayName,
                                                      @JsonProperty("artwork_url") String artworkUrl,
                                                      @JsonProperty("content") ModelCollection<ApiPlaylist> playlists) {
        return new AutoValue_ApiRecommendedPlaylistBucket(key, displayName, Optional.fromNullable(artworkUrl), playlists.getCollection());
    }
}
