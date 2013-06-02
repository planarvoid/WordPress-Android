package com.soundcloud.android.accounts;

import com.google.common.annotations.VisibleForTesting;
import com.soundcloud.api.Token;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.Context;

class SoundCloudTokenOperations {

    private enum TokenDataKeys{
        ACCESS_TOKEN("access_token"),
        REFRESH_TOKEN("refresh_token"),
        SCOPE("scope"),
        EXPIRES_IN("expires_in");

        private String mKey;

        private TokenDataKeys(String mKey) {
            this.mKey = mKey;
        }

        public String key(){
            return mKey;
        }
    }

    private final AccountManager mAccountManager;

    public SoundCloudTokenOperations(Context context){
        this(AccountManager.get(context));
    }

    @VisibleForTesting
    protected SoundCloudTokenOperations(AccountManager mAccountManager) {
        this.mAccountManager = mAccountManager;
    }

    public void storeSoundCloudTokenData(Account account, Token token){
        mAccountManager.setUserData(account, TokenDataKeys.EXPIRES_IN.key(), "" + token.expiresIn);
        mAccountManager.setUserData(account, TokenDataKeys.SCOPE.key(), token.scope);
        mAccountManager.setAuthToken(account, TokenDataKeys.ACCESS_TOKEN.key(), token.access);
        mAccountManager.setAuthToken(account, TokenDataKeys.REFRESH_TOKEN.key(), token.refresh);


    }

    public Token getSoundCloudToken(Account account) {
        return new Token(getSoundCloudAccessToken(account), getSoundCloudRefreshToken(account), getSoundCloudTokenScope(account));
    }

    public void invalidateToken(Token expired, Account account) {
        mAccountManager.invalidateAuthToken(
                account.type,
                expired.access);

        mAccountManager.invalidateAuthToken(
                account.type,
                expired.refresh);

        mAccountManager.setUserData(account, TokenDataKeys.EXPIRES_IN.key(), null);
        mAccountManager.setUserData(account, TokenDataKeys.SCOPE.key(), null);
    }

    private String getSoundCloudTokenScope(Account account) {
        return mAccountManager.getUserData(account, TokenDataKeys.SCOPE.key());
    }

    private String getSoundCloudAccessToken(Account account) {
        return mAccountManager.peekAuthToken(account, TokenDataKeys.ACCESS_TOKEN.key());
    }

    private String getSoundCloudRefreshToken(Account account) {
        return mAccountManager.peekAuthToken(account, TokenDataKeys.REFRESH_TOKEN.key());
    }
}
