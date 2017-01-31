package com.soundcloud.android.onboarding.auth.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;

@AutoValue
public abstract class FacebookToken implements Credentials {
    @JsonProperty("token") public abstract String token();

    static FacebookToken create(String token) {
        return new AutoValue_FacebookToken(token);
    }
}
