package com.soundcloud.android.service;

import com.soundcloud.android.activity.Authorize;
import com.soundcloud.android.activity.WebViewAuthorize;

import android.accounts.AbstractAccountAuthenticator;
import android.accounts.Account;
import android.accounts.AccountAuthenticatorResponse;
import android.accounts.AccountManager;
import android.accounts.NetworkErrorException;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.IBinder;

public class AuthenticatorService extends Service {
    public static final String KEY_ACCOUNT_RESULT = "com.soundcloud.android.account-result";
    private static SoundCloudAuthenticator sAuthenticator;

    @Override
    public IBinder onBind(Intent intent) {
        IBinder ret = null;
        if (intent.getAction().equals(AccountManager.ACTION_AUTHENTICATOR_INTENT)) {
            if (sAuthenticator == null) {
                sAuthenticator = new SoundCloudAuthenticator(this);
            }
            ret = sAuthenticator.getIBinder();
        }
        return ret;
    }

    public static class SoundCloudAuthenticator extends AbstractAccountAuthenticator {
        private Context mContext;

        public SoundCloudAuthenticator(Context context) {
            super(context);
            this.mContext = context;
        }

        @Override
        public Bundle editProperties(AccountAuthenticatorResponse response, String accountType) {
            return null;
        }

        @Override
        public Bundle addAccount(AccountAuthenticatorResponse response, String accountType, String authTokenType, String[] requiredFeatures, Bundle options) throws NetworkErrorException {
            Bundle reply = new Bundle();
            Intent intent = new Intent(mContext, WebViewAuthorize.class);
            intent.addFlags(Intent.FLAG_FROM_BACKGROUND |
                    Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS|
                    Intent.FLAG_ACTIVITY_NO_HISTORY);
            intent.putExtra(AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE, response);
            reply.putParcelable(AccountManager.KEY_INTENT, intent);
            return reply;
        }

        @Override
        public Bundle confirmCredentials(AccountAuthenticatorResponse response, Account account, Bundle options) throws NetworkErrorException {
            return null;
        }

        @Override
        public Bundle getAuthToken(AccountAuthenticatorResponse response, Account account, String authTokenType, Bundle options) throws NetworkErrorException {
            return null;
        }

        @Override
        public String getAuthTokenLabel(String authTokenType) {
            return null;
        }

        @Override
        public Bundle updateCredentials(AccountAuthenticatorResponse response, Account account, String authTokenType, Bundle options) throws NetworkErrorException {
            return null;
        }

        @Override
        public Bundle hasFeatures(AccountAuthenticatorResponse response, Account account, String[] features) throws NetworkErrorException {
            return null;
        }
    }
}
