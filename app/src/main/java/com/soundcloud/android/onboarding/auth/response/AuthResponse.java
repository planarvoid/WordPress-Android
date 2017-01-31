package com.soundcloud.android.onboarding.auth.response;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.soundcloud.android.accounts.Me;
import com.soundcloud.android.api.oauth.Token;

public class AuthResponse {
    public final Me me;
    public final Token token;

    @JsonCreator
    public AuthResponse(@JsonProperty("token") Token token, @JsonProperty("me") Me me) {
        this.me = me;
        this.token = token;
    }
}
