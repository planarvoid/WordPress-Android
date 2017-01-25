package com.soundcloud.android.onboarding.auth.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;

@AutoValue
public abstract class UserCredentials implements Credentials {
    @JsonProperty("identifier") public abstract String identifier();
    @JsonProperty("password") public abstract String password();

    static UserCredentials create(String identifier, String password) {
        return new AutoValue_UserCredentials(identifier, password);
    }
}
