package com.soundcloud.android.onboarding.auth.tasks;

import static com.soundcloud.android.SoundCloudApplication.TAG;

import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.api.ApiClient;
import com.soundcloud.android.api.oauth.OAuth;
import com.soundcloud.android.commands.StoreUsersCommand;
import com.soundcloud.android.configuration.ConfigurationOperations;
import com.soundcloud.android.onboarding.auth.SignInOperations;
import com.soundcloud.android.onboarding.auth.TokenInformationGenerator;
import com.soundcloud.android.onboarding.exceptions.TokenRetrievalException;
import com.soundcloud.android.properties.FeatureFlags;
import com.soundcloud.android.properties.Flag;
import com.soundcloud.android.sync.SyncInitiatorBridge;
import com.soundcloud.annotations.VisibleForTesting;
import com.soundcloud.rx.eventbus.EventBus;

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
    private final FeatureFlags featureFlags;
    protected String accountName;
    protected String scope;
    private Bundle extras;

    public GooglePlusSignInTask(SoundCloudApplication application, String accountName, String scope,
                                TokenInformationGenerator tokenInformationGenerator, StoreUsersCommand userStorage,
                                AccountOperations accountOperations, ConfigurationOperations configurationOperations,
                                EventBus eventBus, ApiClient apiClient, SyncInitiatorBridge syncInitiatorBridge,
                                FeatureFlags featureFlags, SignInOperations signInOperations) {
        super(application,
              tokenInformationGenerator,
              userStorage,
              configurationOperations,
              eventBus,
              accountOperations,
              apiClient,
              syncInitiatorBridge,
              featureFlags,
              signInOperations);
        this.accountName = accountName;
        this.scope = scope;
        this.featureFlags = featureFlags;
        extras = new Bundle();
        extras.putString(KEY_REQUEST_VISIBLE_ACTIVITIES, TextUtils.join(" ", REQUEST_ACTIVITIES));
    }

    @Override
    protected LegacyAuthTaskResult doInBackground(Bundle... params) {
        LegacyAuthTaskResult result = null;
        boolean googleTokenValid = false;
        for (int triesLeft = 2; triesLeft > 0 && !googleTokenValid; triesLeft--) {
            try {
                String token = accountOperations.getGoogleAccountToken(accountName, scope, extras);
                result = login(token);

                googleTokenValid = !(result.getException() instanceof TokenRetrievalException);
                if (!googleTokenValid) {
                    // whatever token we got from g+ is invalid. force it to invalid and we should get a new one next try
                    accountOperations.invalidateGoogleAccountToken(token);
                }
            } catch (IOException e) {
                Log.e(TAG, "error retrieving google token", e);
                result = LegacyAuthTaskResult.networkError(e);
                triesLeft = 0;
            } catch (Exception e) {
                Log.e(TAG, "error retrieving google token", e);
                result = LegacyAuthTaskResult.failure(e);
                triesLeft = 0;
            }
        }
        return result;
    }

    protected LegacyAuthTaskResult login(String token) {
        if (featureFlags.isEnabled(Flag.AUTH_API_MOBILE)) {
            return login(SignInOperations.getGoogleTokenBundle(token));
        } else {
            return login(tokenUtils.getGrantBundle(OAuth.GRANT_TYPE_GOOGLE_PLUS, token));
        }
    }
}
