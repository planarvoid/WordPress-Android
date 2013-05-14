package com.soundcloud.android.task.auth;

import static com.soundcloud.android.SoundCloudApplication.TAG;

import com.google.android.gms.auth.GoogleAuthUtil;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.accounts.AuthenticationManager;
import com.soundcloud.android.activity.auth.TokenInformationGenerator;
import com.soundcloud.android.dao.UserStorage;
import com.soundcloud.android.task.fetch.FetchUserTask;
import com.soundcloud.api.CloudAPI;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;

public class GooglePlusSignInTask extends LoginTask {
    private static final String ADD_ACTIVITY    = "http://schemas.google.com/AddActivity";
    private static final String CREATE_ACTIVITY = "http://schemas.google.com/CreateActivity";
    private static final String LISTEN_ACTIVITY = "http://schemas.google.com/ListenActivity";

    private static final String[] REQUEST_ACTIVITIES = { ADD_ACTIVITY, CREATE_ACTIVITY, LISTEN_ACTIVITY };

    private String mAccountName, mScope;
    private Bundle mExtras;
    private AuthenticationManager mAuthenticationManager;

    public GooglePlusSignInTask(SoundCloudApplication application, String accountName, String scope) {
        this(application, accountName, scope, new TokenInformationGenerator(), new FetchUserTask(application), new UserStorage(), new AuthenticationManager());
    }

    protected GooglePlusSignInTask(SoundCloudApplication application, String accountName, String scope,
                                   TokenInformationGenerator tokenInformationGenerator, FetchUserTask fetchUserTask, UserStorage userStorage,
                                   AuthenticationManager authenticationManager) {
        super(application, tokenInformationGenerator, fetchUserTask, userStorage);
        mAccountName = accountName;
        mScope = scope;
        mAuthenticationManager = authenticationManager;

        mExtras = new Bundle();
        mExtras.putString(GoogleAuthUtil.KEY_REQUEST_VISIBLE_ACTIVITIES, TextUtils.join(" ", REQUEST_ACTIVITIES));
    }

    @Override
    protected AuthTaskResult doInBackground(Bundle... params) {
        AuthTaskResult result = null;
        boolean googleTokenValid = false;
        for (int triesLeft = 2; triesLeft > 0 && !googleTokenValid; triesLeft--){
            try {
                String token = mAuthenticationManager.getGoogleAccountToken(getSoundCloudApplication(), mAccountName, mScope, mExtras);
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
        return login(tokenUtils.getGrantBundle(CloudAPI.GOOGLE_PLUS_GRANT_TYPE, token));
    }
}
