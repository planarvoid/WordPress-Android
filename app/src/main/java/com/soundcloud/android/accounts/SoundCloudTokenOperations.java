package com.soundcloud.android.accounts;

import com.soundcloud.android.api.oauth.Token;
import org.jetbrains.annotations.Nullable;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.Context;

import javax.inject.Inject;

class SoundCloudTokenOperations {

    private final AccountManager accountManager;

    public SoundCloudTokenOperations(Context context) {
        this(AccountManager.get(context));
    }


    @Inject
    public SoundCloudTokenOperations(AccountManager accountManager) {
        this.accountManager = accountManager;
    }

    public void storeSoundCloudTokenData(@Nullable Account account, Token token) {
        accountManager.setUserData(account, TokenDataKeys.EXPIRES_IN.key(), Long.toString(token.getExpiresAt()));
        accountManager.setUserData(account, TokenDataKeys.SCOPE.key(), token.getScope());
        accountManager.setAuthToken(account, TokenDataKeys.ACCESS_TOKEN.key(), token.getAccessToken());
        accountManager.setAuthToken(account, TokenDataKeys.REFRESH_TOKEN.key(), token.getRefreshToken());
    }

    public Token getSoundCloudToken(@Nullable Account account) {
        return new Token(getSoundCloudAccessToken(account), getSoundCloudRefreshToken(account), getSoundCloudTokenScope(account));
    }

    public void invalidateToken(Token expired, @Nullable Account account) {
        if (account != null) {
            accountManager.invalidateAuthToken(
                    account.type,
                    expired.getAccessToken());

            accountManager.invalidateAuthToken(
                    account.type,
                    expired.getRefreshToken());

            accountManager.setUserData(account, TokenDataKeys.EXPIRES_IN.key(), null);
            accountManager.setUserData(account, TokenDataKeys.SCOPE.key(), null);
        }
    }

    private String getSoundCloudTokenScope(Account account) {
        return accountManager.getUserData(account, TokenDataKeys.SCOPE.key());
    }

    private String getSoundCloudAccessToken(Account account) {
        return accountManager.peekAuthToken(account, TokenDataKeys.ACCESS_TOKEN.key());
    }

    private String getSoundCloudRefreshToken(Account account) {
        return accountManager.peekAuthToken(account, TokenDataKeys.REFRESH_TOKEN.key());
    }

    private enum TokenDataKeys {
        ACCESS_TOKEN("access_token"),
        REFRESH_TOKEN("refresh_token"),
        SCOPE("scope"),
        EXPIRES_IN("expires_in");

        private String key;

        private TokenDataKeys(String key) {
            this.key = key;
        }

        public String key() {
            return key;
        }
    }
}
