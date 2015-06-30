package com.soundcloud.api;

import com.soundcloud.android.api.oauth.Token;

/**
 * Interested in changes to the current token.
 */
public interface TokenListener {
    /**
     * Called when token was found to be invalid
     *
     * @param token the invalid token
     * @return a cached token if available, or null
     */
    Token onTokenInvalid(Token token);

    /**
     * Called when the token got successfully refreshed
     *
     * @param token the refreshed token
     */
    void onTokenRefreshed(Token token);
}
