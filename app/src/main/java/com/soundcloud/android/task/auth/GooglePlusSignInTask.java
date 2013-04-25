package com.soundcloud.android.task.auth;

import static com.soundcloud.android.SoundCloudApplication.TAG;

import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.accounts.AuthenticationManager;
import com.soundcloud.android.activity.auth.TokenInformationGenerator;
import com.soundcloud.android.dao.UserStorage;
import com.soundcloud.android.task.fetch.FetchUserTask;
import com.soundcloud.api.CloudAPI;

import android.os.Bundle;
import android.util.Log;

public class GooglePlusSignInTask extends LoginTask {
    protected String mAccountName, mScope;
    private AuthenticationManager mAuthenticationManager;

    public GooglePlusSignInTask(SoundCloudApplication application, String accountName, String scope) {
        this(application, accountName, scope, new TokenInformationGenerator(), new FetchUserTask(application), new UserStorage(application), new AuthenticationManager());
    }

    protected GooglePlusSignInTask(SoundCloudApplication application, String accountName, String scope,
                                   TokenInformationGenerator tokenInformationGenerator, FetchUserTask fetchUserTask, UserStorage userStorage,
                                   AuthenticationManager authenticationManager) {
        super(application, tokenInformationGenerator, fetchUserTask, userStorage);
        mAccountName = accountName;
        mScope = scope;
        mAuthenticationManager = authenticationManager;
    }

    @Override
    protected AuthTaskResult doInBackground(Bundle... params) {
        AuthTaskResult result = null;
        boolean googleTokenValid = false;
        for (int triesLeft = 2; triesLeft > 0 && !googleTokenValid; triesLeft--){
            try {
                String token = mAuthenticationManager.getGoogleAccountToken(getSoundCloudApplication(), mAccountName, mScope);
                result = login(token);

                googleTokenValid = !(result.getException() instanceof CloudAPI.InvalidTokenException);
                if (!googleTokenValid){
                    // whatever token we got from g+ is invalid. force it to invalid and we should get a new one next try
                    mAuthenticationManager.invalidateGoogleAccountToken(getSoundCloudApplication(), token);
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
        // TODO : Google + grant type constant once ApiWrapper is updated
        return login(tokenUtils.getGrantBundle("urn:soundcloud:oauth2:grant-type:google_plus&access_token=", token));
    }
}
