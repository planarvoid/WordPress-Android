package com.soundcloud.android.onboarding.auth.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;

@AutoValue
public abstract class ResetPasswordBody {
    @JsonProperty("email") abstract String email();

    public static ResetPasswordBody create(String email) {
        return new AutoValue_ResetPasswordBody(email);
    }
}
