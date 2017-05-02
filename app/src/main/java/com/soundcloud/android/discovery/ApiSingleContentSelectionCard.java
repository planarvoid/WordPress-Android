package com.soundcloud.android.discovery;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import com.soundcloud.android.model.Urn;
import com.soundcloud.java.optional.Optional;

import android.support.annotation.Nullable;

import java.util.List;

@AutoValue
abstract class ApiSingleContentSelectionCard {
    abstract Urn selectionUrn();

    abstract Optional<Urn> queryUrn();

    abstract Optional<String> style();

    abstract Optional<String> title();

    abstract Optional<String> description();

    abstract Optional<String> socialProof();

    abstract ApiSelectionItem selectionItem();

    abstract Optional<List<String>> socialProofAvatarUrlTemplates();

    @JsonCreator
    static ApiSingleContentSelectionCard create(@JsonProperty("selection_urn") Urn selectionUrn,
                                                @JsonProperty("query_urn") @Nullable Urn queryUrn,
                                                @JsonProperty("style") @Nullable String style,
                                                @JsonProperty("title") @Nullable String title,
                                                @JsonProperty("description") @Nullable String description,
                                                @JsonProperty("social_proof") @Nullable String socialProof,
                                                @JsonProperty("selection_item") ApiSelectionItem selectionItem,
                                                @JsonProperty("social_proof_avatar_url_templates") @Nullable List<String> socialProofAvatarUrlTemplates) {
        return new AutoValue_ApiSingleContentSelectionCard(selectionUrn,
                                                       Optional.fromNullable(queryUrn),
                                                       Optional.fromNullable(style),
                                                       Optional.fromNullable(title),
                                                       Optional.fromNullable(description),
                                                       Optional.fromNullable(socialProof),
                                                       selectionItem,
                                                       Optional.fromNullable(socialProofAvatarUrlTemplates));
    }
}
