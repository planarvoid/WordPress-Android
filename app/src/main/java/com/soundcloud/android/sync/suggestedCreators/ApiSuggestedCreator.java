package com.soundcloud.android.sync.suggestedCreators;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import com.soundcloud.android.api.model.ApiUser;

@AutoValue
public abstract class ApiSuggestedCreator {
    @JsonCreator
    public static ApiSuggestedCreator create(@JsonProperty("seed_user") ApiUser seedUser,
                                             @JsonProperty("suggested_user") ApiUser suggestedUser,
                                             @JsonProperty("relation_key") String relationKey) {
        return new AutoValue_ApiSuggestedCreator(seedUser, suggestedUser, relationKey);
    }

    public abstract ApiUser getSeedUser();

    public abstract ApiUser getSuggestedUser();

    public abstract String getRelationKey();
}
