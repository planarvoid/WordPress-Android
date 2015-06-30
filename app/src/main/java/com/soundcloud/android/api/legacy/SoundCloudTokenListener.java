package com.soundcloud.android.api.legacy;

import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.api.oauth.Token;
import com.soundcloud.api.TokenListener;

class SoundCloudTokenListener implements TokenListener {
    private final AccountOperations accountOperations;

    public SoundCloudTokenListener(AccountOperations accountOperations) {
        this.accountOperations = accountOperations;
    }

    @Override
    public Token onTokenInvalid(final Token expired) {
        //TODO If the token is invalid, shouldnt we be requesting another token from the backend rather than just obtaining
        //the same token from the local cache? SoundCloudAuthenticator.getAuthToken methods are not implemented...
        try {
            if (accountOperations.isUserLoggedIn()) {
                Token newToken = accountOperations.getSoundCloudToken();
                if (!newToken.equals(expired)) {
                    return newToken;
                }
            }
            return null;
        } finally {
            accountOperations.invalidateSoundCloudToken(expired);
        }
    }

    @Override
    public void onTokenRefreshed(Token token) {
        if (accountOperations.isUserLoggedIn() && token.valid() && token.hasDefaultScope()) {
            accountOperations.storeSoundCloudTokenData(token);
        }
    }
}
