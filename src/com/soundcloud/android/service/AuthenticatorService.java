package com.soundcloud.android.service;

import com.soundcloud.android.R;
import com.soundcloud.android.activity.auth.Start;
import com.soundcloud.android.utils.CloudUtils;

import android.accounts.AbstractAccountAuthenticator;
import android.accounts.Account;
import android.accounts.AccountAuthenticatorResponse;
import android.accounts.AccountManager;
import android.accounts.NetworkErrorException;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
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
        private Handler handler = new Handler();

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
            final Bundle reply = new Bundle();
            AccountManager mgr = AccountManager.get(mContext);
            Account[] accounts = mgr.getAccountsByType(accountType);
            final String message = mContext.getString(R.string.account_one_active);
            if (accounts.length == 0) {
                Intent intent = new Intent(mContext, Start.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                intent.putExtra(AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE, response);
                reply.putParcelable(AccountManager.KEY_INTENT, intent);
            } else {
                reply.putInt(AccountManager.KEY_ERROR_CODE, 0);
                reply.putString(AccountManager.KEY_ERROR_MESSAGE, message);

                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        CloudUtils.showToast(mContext, message);
                    }
                });
            }
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
