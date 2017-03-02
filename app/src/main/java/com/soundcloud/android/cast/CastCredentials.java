package com.soundcloud.android.cast;

import com.soundcloud.android.api.oauth.OAuth;
import com.soundcloud.android.api.oauth.Token;

public class CastCredentials {
    private final String authorization;

    public CastCredentials(Token token) {
        this.authorization = OAuth.createOAuthHeaderValue(token);
    }

    public String getAuthorization() {
        // for deserialization
        return authorization;
    }

    @Override
    public String toString() {
        return "CastCredentials{" +
                "authorization='" + authorization + '\'' +
                '}';
    }
}
