package com.soundcloud.android.onboarding.auth;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;

@AutoValue
abstract class AgeRestrictionError {
    @JsonProperty("minimum_age") abstract String minimumAge();

    @JsonCreator
    public static AgeRestrictionError create(@JsonProperty("minimum_age") String minimumAge) {
        return new AutoValue_AgeRestrictionError(minimumAge);
    }
}
