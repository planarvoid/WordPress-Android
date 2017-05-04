package com.soundcloud.android.onboarding.auth.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;

@AutoValue
public abstract class ResetPasswordBody {
    @JsonProperty("identifier") abstract String identifier();

    public static ResetPasswordBody create(String identifier) {
        return new AutoValue_ResetPasswordBody(identifier);
    }
}
