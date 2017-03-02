package com.soundcloud.android.onboarding.auth.tasks;

import static com.soundcloud.android.SoundCloudApplication.TAG;

import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.commands.StoreUsersCommand;
import com.soundcloud.android.onboarding.auth.SignInOperations;
import com.soundcloud.android.onboarding.exceptions.TokenRetrievalException;
import com.soundcloud.android.sync.SyncInitiatorBridge;
import com.soundcloud.annotations.VisibleForTesting;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;

import java.io.IOException;

public class GooglePlusSignInTask extends LoginTask {
    //Workaround since this key does not exist anymore in Google Service 9.8.0
    //We should get rid of this when when kill sign in legacy code
    @VisibleForTesting
    static final String KEY_REQUEST_VISIBLE_ACTIVITIES = "request_visible_actions";

    private static final String ADD_ACTIVITY = "http://schemas.google.com/AddActivity";
    private static final String CREATE_ACTIVITY = "http://schemas.google.com/CreateActivity";
    private static final String LISTEN_ACTIVITY = "http://schemas.google.com/ListenActivity";

    private static final String[] REQUEST_ACTIVITIES = {ADD_ACTIVITY, CREATE_ACTIVITY, LISTEN_ACTIVITY};
    protected String accountName;
    protected String scope;
    private final SignInOperations signInOperations;
    private Bundle extras;

    public GooglePlusSignInTask(SoundCloudApplication application,
                                String accountName,
                                String scope,
                                StoreUsersCommand userStorage,
                                AccountOperations accountOperations,
                                SyncInitiatorBridge syncInitiatorBridge,
                                SignInOperations signInOperations) {
        super(application,
              userStorage,
              accountOperations,
              syncInitiatorBridge,
              signInOperations);
        this.accountName = accountName;
        this.scope = scope;
        this.signInOperations = signInOperations;
        extras = new Bundle();
        extras.putString(KEY_REQUEST_VISIBLE_ACTIVITIES, TextUtils.join(" ", REQUEST_ACTIVITIES));
    }

    @Override
    protected AuthTaskResult doInBackground(Bundle... params) {
        AuthTaskResult result = null;
        boolean googleTokenInvalid = true;
        for (int triesLeft = 2; triesLeft > 0 && googleTokenInvalid; triesLeft--) {
            try {
                String token = accountOperations.getGoogleAccountToken(accountName, scope, extras);
                result = signInOperations.signIn(SignInOperations.getGoogleTokenBundle(token));

                googleTokenInvalid = !(result.getException() instanceof TokenRetrievalException);
                if (googleTokenInvalid) {
                    // whatever token we got from g+ is invalid. force it to invalid and we should get a new one next try
                    accountOperations.invalidateGoogleAccountToken(token);
                }
            } catch (IOException e) {
                Log.e(TAG, "error retrieving google token", e);
                result = AuthTaskResult.networkError(e);
                triesLeft = 0;
            } catch (Exception e) {
                Log.e(TAG, "error retrieving google token", e);
                result = AuthTaskResult.failure(e);
                triesLeft = 0;
            }
        }
        return result;
    }
}
