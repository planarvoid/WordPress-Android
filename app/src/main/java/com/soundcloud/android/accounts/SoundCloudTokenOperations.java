package com.soundcloud.android.accounts;

import com.google.common.annotations.VisibleForTesting;
import com.soundcloud.android.R;
import com.soundcloud.android.model.User;
import com.soundcloud.api.Token;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.Context;
import android.content.res.Resources;
import android.os.SystemClock;

class SoundCloudTokenOperations {

    private final AccountManager mAccountManager;
    private final Resources mResources;

    public SoundCloudTokenOperations(Context context){
        this(AccountManager.get(context.getApplicationContext()), context.getResources());
    }

    @VisibleForTesting
    protected SoundCloudTokenOperations(AccountManager mAccountManager, Resources mResources) {
        this.mAccountManager = mAccountManager;
        this.mResources = mResources;
    }

    public void storeSoundCloudTokenData(Account account, Token token){

        //TODO shouldnt we be implement the necessary methods in Authenticator Service?
        mAccountManager.setAuthToken(account, User.DataKeys.ACCESS_TOKEN, token.access);
        mAccountManager.setAuthToken(account, User.DataKeys.REFRESH_TOKEN, token.refresh);
        mAccountManager.setUserData(account, User.DataKeys.SCOPE, token.scope);

        //TODO Jon/Matthias do we need to store these? Where are they used?
        mAccountManager.setPassword(account, token.access);
        mAccountManager.setUserData(account, User.DataKeys.EXPIRES_IN, "" + token.expiresIn);
    }

    public Token getSoundCloudToken(Account account) {
        return new Token(getSoundCloudAccessToken(account), getSoundCloudRefreshToken(account), getSoundCloudTokenScope(account));
    }

    public void invalidateToken(Token expired) {
        String accountType = mResources.getString(R.string.account_type);
        mAccountManager.invalidateAuthToken(
                accountType,
                expired.access);

        mAccountManager.invalidateAuthToken(
                accountType,
                expired.refresh);
    }

    private String getSoundCloudTokenScope(Account account) {
        return mAccountManager.getUserData(account, User.DataKeys.SCOPE);
    }

    private String getSoundCloudAccessToken(Account account) {
        return mAccountManager.peekAuthToken(account, User.DataKeys.ACCESS_TOKEN);
    }

    private String getSoundCloudRefreshToken(Account account) {
        return mAccountManager.peekAuthToken(account, User.DataKeys.REFRESH_TOKEN);
    }
}
