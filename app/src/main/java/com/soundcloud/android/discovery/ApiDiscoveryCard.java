package com.soundcloud.android.discovery;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import com.soundcloud.java.optional.Optional;
import org.jetbrains.annotations.Nullable;

@AutoValue
abstract class ApiDiscoveryCard {

    abstract Optional<ApiSingleContentSelectionCard> singleContentSelectionCard();

    abstract Optional<ApiMultipleContentSelectionCard> multipleContentSelectionCard();

    @JsonCreator
    static ApiDiscoveryCard create(@JsonProperty("single_content_selection_card") @Nullable ApiSingleContentSelectionCard apiSingleContentSelectionCard,
                                   @JsonProperty("multiple_content_selection_card") @Nullable ApiMultipleContentSelectionCard apiMultipleContentSelectionCard) {
        return new AutoValue_ApiDiscoveryCard(Optional.fromNullable(apiSingleContentSelectionCard), Optional.fromNullable(apiMultipleContentSelectionCard));
    }
}
