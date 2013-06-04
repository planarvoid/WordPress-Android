package com.soundcloud.android.api;

import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.api.CloudAPI;
import com.soundcloud.api.Token;

import android.content.Context;

class SoundCloudTokenListener implements CloudAPI.TokenListener {
    private final AccountOperations accountOperations;

    public SoundCloudTokenListener(Context context){
        this(new AccountOperations(context));
    }
    SoundCloudTokenListener(AccountOperations accountOperations) {
        this.accountOperations = accountOperations;
    }

    @Override
    public Token onTokenInvalid(final Token expired) {
        //TODO If the token is invalid, shouldnt we be requesting another token from the backend rather than just obtaining
        //the same token from the local cache? SoundCloudAuthenticator.getAuthToken methods are not implemented...
        try {
            if (accountOperations.soundCloudAccountExists()) {
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
        if (accountOperations.soundCloudAccountExists() && token.valid() && token.defaultScoped()) {
            accountOperations.storeSoundCloudTokenData(token);
        }
    }
}
