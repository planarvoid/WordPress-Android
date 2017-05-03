package com.soundcloud.android.discovery;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import com.soundcloud.android.api.model.ModelCollection;
import com.soundcloud.android.model.Urn;
import com.soundcloud.java.optional.Optional;

import android.support.annotation.Nullable;

@AutoValue
abstract class ApiMultipleContentSelectionCard {

    abstract Urn selectionUrn();

    abstract Optional<String> style();

    abstract Optional<String> title();

    abstract Optional<String> description();

    abstract ModelCollection<ApiSelectionItem> selectionItems();

    @JsonCreator
    static ApiMultipleContentSelectionCard create(@JsonProperty("selection_urn") Urn selectionUrn,
                                                  @JsonProperty("style") @Nullable String style,
                                                  @JsonProperty("title") @Nullable String title,
                                                  @JsonProperty("description") @Nullable String description,
                                                  @JsonProperty("selection_items") @Nullable ModelCollection<ApiSelectionItem> selectionItems) {
        return new AutoValue_ApiMultipleContentSelectionCard(selectionUrn,
                                              Optional.fromNullable(style),
                                              Optional.fromNullable(title),
                                              Optional.fromNullable(description),
                                              selectionItems);
    }
}
