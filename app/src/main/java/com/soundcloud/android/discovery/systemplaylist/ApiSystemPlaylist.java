package com.soundcloud.android.discovery.systemplaylist;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import com.soundcloud.android.api.model.ApiTrack;
import com.soundcloud.android.api.model.ModelCollection;
import com.soundcloud.android.model.Urn;
import com.soundcloud.java.optional.Optional;

import java.util.Date;

@AutoValue
public abstract class ApiSystemPlaylist {
    public abstract Urn urn();

    public abstract Optional<Integer> trackCount();

    public abstract Optional<Date> lastUpdated();

    public abstract Optional<String> title();

    public abstract Optional<String> description();

    public abstract Optional<String> artworkUrlTemplate();

    public abstract ModelCollection<ApiTrack> tracks();

    @JsonCreator
    public static ApiSystemPlaylist create(@JsonProperty("urn") Urn urn,
                                           @JsonProperty("track_count") Optional<Integer> trackCount,
                                           @JsonProperty("last_updated") Optional<Date> lastUpdated,
                                           @JsonProperty("title") Optional<String> title,
                                           @JsonProperty("description") Optional<String> description,
                                           @JsonProperty("artwork_url_template") Optional<String> artworkUrlTemplate,
                                           @JsonProperty("tracks") ModelCollection<ApiTrack> tracks) {
        return new AutoValue_ApiSystemPlaylist(urn, trackCount, lastUpdated, title, description, artworkUrlTemplate, tracks);
    }
}
