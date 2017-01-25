package com.soundcloud.android.cast;

import com.soundcloud.android.api.oauth.OAuth;
import com.soundcloud.android.api.oauth.Token;
import com.soundcloud.android.properties.FeatureFlags;

public class CastCredentials {
    private final String authorization;

    public CastCredentials(Token token, FeatureFlags featureFlags) {
        this.authorization = OAuth.createOAuthHeaderValue(featureFlags, token);
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
