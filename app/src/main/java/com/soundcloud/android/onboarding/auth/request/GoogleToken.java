package com.soundcloud.android.onboarding.auth.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;

@AutoValue
public abstract class GoogleToken implements Credentials {
    @JsonProperty("token") public abstract String token();

    static GoogleToken create(String token) {
        return new AutoValue_GoogleToken(token);
    }
}
