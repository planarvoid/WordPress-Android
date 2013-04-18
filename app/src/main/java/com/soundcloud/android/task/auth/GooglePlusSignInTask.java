package com.soundcloud.android.task.auth;

import static com.soundcloud.android.SoundCloudApplication.TAG;

import com.google.android.gms.auth.GoogleAuthUtil;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.activity.auth.TokenUtil;
import com.soundcloud.api.CloudAPI;

import android.os.Bundle;
import android.util.Log;

public class GooglePlusSignInTask extends LoginTask {
    protected String mAccountName, mScope;
    protected int mRequestCode;

    public GooglePlusSignInTask(SoundCloudApplication application, String accountName, String scope, int requestCode) {
        super(application);
        mAccountName = accountName;
        mScope = scope;
        mRequestCode = requestCode;
    }

    @Override
    protected Result doInBackground(Bundle... params) {
        Result result = null;
        boolean validToken = false;
        for (int triesLeft = 2; triesLeft > 0 && !validToken; triesLeft--){
            try {
                String token = GoogleAuthUtil.getToken(getSoundCloudApplication(), mAccountName, mScope);
                result = login(token);

                validToken = !(result.getException() instanceof CloudAPI.InvalidTokenException);
                if (!validToken){
                    // whatever token we got from g+ is invalid. force it to invalid and we should get a new one next try
                    GoogleAuthUtil.invalidateToken(getSoundCloudApplication(),token);
                }
            } catch (Exception e) {
                Log.e(TAG, "error retrieving google token", e);
                result = new Result(e);
                triesLeft = 0;
            }
        }
        return result;
    }

    protected Result login(String token) {
        // TODO : Google + grant type constant once ApiWrapper is updated
        return login(TokenUtil.getGrantBundle("urn:soundcloud:oauth2:grant-type:google_plus&access_token=", token));
    }
}
