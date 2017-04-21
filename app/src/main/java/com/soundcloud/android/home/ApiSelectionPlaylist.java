package com.soundcloud.android.home;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import com.soundcloud.android.model.Urn;
import com.soundcloud.java.optional.Optional;

import android.support.annotation.Nullable;

@AutoValue
abstract class ApiSelectionPlaylist {
    abstract Urn urn();

    abstract Optional<String> artworkUrlTemplate();

    abstract Optional<Integer> trackCount();

    abstract Optional<String> shortTitle();

    abstract Optional<String> shortSubtitle();

    @JsonCreator
    static ApiSelectionPlaylist create(@JsonProperty("urn") Urn urn,
                                       @JsonProperty("artwork_url_template") @Nullable String artworkUrlTemplate,
                                       @JsonProperty("track_count") @Nullable Integer trackCount,
                                       @JsonProperty("short_title") @Nullable String shortTitle,
                                       @JsonProperty("short_subtitle") @Nullable String shortSubtitle) {
        return new AutoValue_ApiSelectionPlaylist(urn,
                                                  Optional.fromNullable(artworkUrlTemplate),
                                                  Optional.fromNullable(trackCount),
                                                  Optional.fromNullable(shortTitle),
                                                  Optional.fromNullable(shortSubtitle));
    }
}
