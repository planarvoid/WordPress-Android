package com.soundcloud.android.sync.suggestedCreators;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import com.soundcloud.java.optional.Optional;
import org.jetbrains.annotations.Nullable;

@AutoValue
public abstract class ApiSuggestedCreatorItem {

    @JsonCreator
    public static ApiSuggestedCreatorItem create(
            @JsonProperty("suggested_creator") @Nullable ApiSuggestedCreator apiSuggestedCreator) {
        return new AutoValue_ApiSuggestedCreatorItem(Optional.fromNullable(apiSuggestedCreator));
    }

    public abstract Optional<ApiSuggestedCreator> getSuggestedCreator();
}
