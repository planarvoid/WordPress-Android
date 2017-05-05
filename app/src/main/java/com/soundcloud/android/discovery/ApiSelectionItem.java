package com.soundcloud.android.discovery;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import com.soundcloud.android.model.Urn;
import com.soundcloud.java.optional.Optional;

import android.support.annotation.Nullable;

@AutoValue
abstract class ApiSelectionItem {
    abstract Optional<Urn> urn();

    abstract Optional<String> artworkUrlTemplate();

    abstract Optional<Integer> count();

    abstract Optional<String> shortTitle();

    abstract Optional<String> shortSubtitle();

    @JsonCreator
    static ApiSelectionItem create(@JsonProperty("urn") @Nullable Urn urn,
                                   @JsonProperty("artwork_url_template") @Nullable String artworkUrlTemplate,
                                   @JsonProperty("count") @Nullable Integer count,
                                   @JsonProperty("short_title") @Nullable String shortTitle,
                                   @JsonProperty("short_subtitle") @Nullable String shortSubtitle) {
        return new AutoValue_ApiSelectionItem(
                Optional.fromNullable(urn),
                Optional.fromNullable(artworkUrlTemplate),
                Optional.fromNullable(count),
                Optional.fromNullable(shortTitle),
                Optional.fromNullable(shortSubtitle));
    }
}
