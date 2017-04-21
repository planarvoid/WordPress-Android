package com.soundcloud.android.home;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import com.soundcloud.android.api.model.ModelCollection;
import com.soundcloud.android.model.Urn;
import com.soundcloud.java.optional.Optional;

import android.support.annotation.Nullable;

@AutoValue
abstract class ApiSelectionCard {

    abstract Urn selectionUrn();

    abstract Optional<Urn> queryUrn();

    abstract Optional<String> style();

    abstract Optional<String> title();

    abstract Optional<String> description();

    abstract Optional<ModelCollection<ApiSelectionPlaylist>> selectionPlaylists();

    @JsonCreator
    static ApiSelectionCard create(@JsonProperty("selection_urn") Urn selectionUrn,
                                   @JsonProperty("query_urn") @Nullable Urn queryUrn,
                                   @JsonProperty("style") @Nullable String style,
                                   @JsonProperty("title") @Nullable String title,
                                   @JsonProperty("description") @Nullable String description,
                                   @JsonProperty("selection_playlists") @Nullable ModelCollection<ApiSelectionPlaylist> selectionPlaylists) {
        return new AutoValue_ApiSelectionCard(selectionUrn,
                                              Optional.fromNullable(queryUrn),
                                              Optional.fromNullable(style),
                                              Optional.fromNullable(title),
                                              Optional.fromNullable(description),
                                              Optional.fromNullable(selectionPlaylists));
    }
}
