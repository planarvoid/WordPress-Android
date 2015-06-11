package com.soundcloud.android.onboarding.auth.tasks;

import static com.soundcloud.android.SoundCloudApplication.TAG;

import com.google.android.gms.auth.GoogleAuthUtil;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.api.legacy.PublicApi;
import com.soundcloud.android.api.oauth.OAuth;
import com.soundcloud.android.configuration.ConfigurationOperations;
import com.soundcloud.android.onboarding.auth.TokenInformationGenerator;
import com.soundcloud.android.rx.eventbus.EventBus;
import com.soundcloud.android.storage.LegacyUserStorage;
import com.soundcloud.android.tasks.FetchUserTask;
import com.soundcloud.api.CloudAPI;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;

public class GooglePlusSignInTask extends LoginTask {
    private static final String ADD_ACTIVITY    = "http://schemas.google.com/AddActivity";
    private static final String CREATE_ACTIVITY = "http://schemas.google.com/CreateActivity";
    private static final String LISTEN_ACTIVITY = "http://schemas.google.com/ListenActivity";

    private static final String[] REQUEST_ACTIVITIES = { ADD_ACTIVITY, CREATE_ACTIVITY, LISTEN_ACTIVITY };

    private Bundle extras;
    protected String accountName, scope;
    private AccountOperations accountOperations;

    public GooglePlusSignInTask(SoundCloudApplication application, String accountName, String scope,
                                ConfigurationOperations configurationOperations, EventBus eventBus, AccountOperations accountOperations) {
        this(application, accountName, scope, new TokenInformationGenerator(new PublicApi(application)),
                new FetchUserTask(new PublicApi(application)),
                new LegacyUserStorage(), accountOperations, configurationOperations, eventBus);
    }

    protected GooglePlusSignInTask(SoundCloudApplication application, String accountName, String scope,
                                   TokenInformationGenerator tokenInformationGenerator, FetchUserTask fetchUserTask, LegacyUserStorage userStorage,
                                   AccountOperations accountOperations, ConfigurationOperations configurationOperations, EventBus eventBus) {
        super(application, tokenInformationGenerator, fetchUserTask, userStorage, configurationOperations, eventBus, accountOperations);
        this.accountName = accountName;
        this.scope = scope;
        extras = new Bundle();
        extras.putString(GoogleAuthUtil.KEY_REQUEST_VISIBLE_ACTIVITIES, TextUtils.join(" ", REQUEST_ACTIVITIES));
        this.accountOperations = accountOperations;
    }

    @Override
    protected AuthTaskResult doInBackground(Bundle... params) {
        AuthTaskResult result = null;
        boolean googleTokenValid = false;
        for (int triesLeft = 2; triesLeft > 0 && !googleTokenValid; triesLeft--){
            try {
                String token = accountOperations.getGoogleAccountToken(accountName, scope, extras);
                result = login(token);

                googleTokenValid = !(result.getException() instanceof CloudAPI.InvalidTokenException);
                if (!googleTokenValid){
                    // whatever token we got from g+ is invalid. force it to invalid and we should get a new one next try
                    accountOperations.invalidateGoogleAccountToken(token);
                }
            } catch (Exception e) {
                Log.e(TAG, "error retrieving google token", e);
                result = AuthTaskResult.failure(e);
                triesLeft = 0;
            }
        }
        return result;
    }

    protected AuthTaskResult login(String token) {
        return login(tokenUtils.getGrantBundle(OAuth.GRANT_TYPE_GOOGLE_PLUS, token));
    }
}
