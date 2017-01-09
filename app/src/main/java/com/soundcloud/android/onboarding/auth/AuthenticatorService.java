package com.soundcloud.android.onboarding.auth;

import static com.soundcloud.android.rx.observers.DefaultSubscriber.fireAndForget;

import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.onboarding.OnboardActivity;
import com.soundcloud.android.utils.AndroidUtils;

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

import javax.inject.Inject;

public class AuthenticatorService extends Service {

    @Inject SoundCloudAuthenticator authenticator;

    @Override
    public void onCreate() {
        super.onCreate();
        SoundCloudApplication.getObjectGraph().inject(this);
    }

    @Override
    public IBinder onBind(Intent intent) {
        IBinder ret = null;
        if (intent.getAction().equals(AccountManager.ACTION_AUTHENTICATOR_INTENT)) {
            ret = authenticator.getIBinder();
        }
        return ret;
    }

    public static class SoundCloudAuthenticator extends AbstractAccountAuthenticator {
        private final AccountOperations accountOperations;
        private final Context context;
        private final Handler handler = new Handler();

        @Inject
        public SoundCloudAuthenticator(Context context, AccountOperations accountOperations) {
            super(context);
            this.context = context;
            this.accountOperations = accountOperations;
        }

        @Override
        public Bundle editProperties(AccountAuthenticatorResponse response, String accountType) {
            return null;
        }

        @Override
        @SuppressWarnings("MissingPermission") //safe since we are manipulating our own SC account.
        public Bundle addAccount(AccountAuthenticatorResponse response,
                                 String accountType,
                                 String authTokenType,
                                 String[] requiredFeatures,
                                 Bundle options) throws NetworkErrorException {
            final Bundle reply = new Bundle();
            final Account[] accounts = AndroidUtils.getAccounts(context, accountType);
            if (accounts.length == 0) {
                Intent intent = new Intent(context, OnboardActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                intent.putExtra(AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE, response);
                reply.putParcelable(AccountManager.KEY_INTENT, intent);
            } else {
                reply.putInt(AccountManager.KEY_ERROR_CODE, 0);
                reply.putString(AccountManager.KEY_ERROR_MESSAGE, context.getString(R.string.account_one_active));

                handler.post(() -> AndroidUtils.showToast(context, R.string.account_one_active));
            }
            return reply;
        }

        /**
         * We override this as a workaround for the absence of an "account removed" event fired by Android.
         * We hook into this method instead which always gets invoked when accounts are attempting to get removed
         * and subsequently trigger a user data wipe.
         */
        @Override
        public Bundle getAccountRemovalAllowed(AccountAuthenticatorResponse response,
                                               Account account) throws NetworkErrorException {
            Bundle result = super.getAccountRemovalAllowed(response, account);

            if (result != null && result.containsKey(AccountManager.KEY_BOOLEAN_RESULT)
                    && !result.containsKey(AccountManager.KEY_INTENT)) {
                final boolean removalAllowed = result.getBoolean(AccountManager.KEY_BOOLEAN_RESULT);

                if (removalAllowed) {
                    fireAndForget(accountOperations.purgeUserData());
                }
            }

            return result;
        }

        @Override
        public Bundle confirmCredentials(AccountAuthenticatorResponse response,
                                         Account account,
                                         Bundle options) throws NetworkErrorException {
            return null;
        }

        @Override
        public Bundle getAuthToken(AccountAuthenticatorResponse response,
                                   Account account,
                                   String authTokenType,
                                   Bundle options) throws NetworkErrorException {
            return null;
        }

        @Override
        public String getAuthTokenLabel(String authTokenType) {
            return null;
        }

        @Override
        public Bundle updateCredentials(AccountAuthenticatorResponse response,
                                        Account account,
                                        String authTokenType,
                                        Bundle options) throws NetworkErrorException {
            return null;
        }

        @Override
        public Bundle hasFeatures(AccountAuthenticatorResponse response,
                                  Account account,
                                  String[] features) throws NetworkErrorException {
            return null;
        }
    }
}
