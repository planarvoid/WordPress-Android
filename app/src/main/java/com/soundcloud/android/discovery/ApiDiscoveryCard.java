package com.soundcloud.android.discovery;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import com.soundcloud.java.optional.Optional;
import org.jetbrains.annotations.Nullable;

@AutoValue
abstract class ApiDiscoveryCard {

    abstract Optional<ApiSingletonSelectionCard> singletonSelectionCard();

    abstract Optional<ApiSelectionCard> selectionCard();

    @JsonCreator
    static ApiDiscoveryCard create(@JsonProperty("singleton_selection_card") @Nullable ApiSingletonSelectionCard singletonSelectionCard,
                                   @JsonProperty("selection_card") @Nullable ApiSelectionCard selectionCard) {
        return new AutoValue_ApiDiscoveryCard(Optional.fromNullable(singletonSelectionCard), Optional.fromNullable(selectionCard));
    }
}
