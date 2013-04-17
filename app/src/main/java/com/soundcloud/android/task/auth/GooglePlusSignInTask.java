package com.soundcloud.android.task.auth;

import com.google.android.gms.auth.GoogleAuthUtil;
import com.soundcloud.android.SoundCloudApplication;

import android.os.Bundle;

public class GooglePlusSignInTask extends LoginTask {
    public static final String EXTENSION_GRANT_TYPE_EXTRA = "extensionGrantType";

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
        String token;
        try {
            token =  GoogleAuthUtil.getToken(getSoundCloudApplication(), mAccountName, mScope);
            Bundle bundle = new Bundle();
            // TODO : Google + grant type constant once ApiWrapper is updated
            bundle.putString(EXTENSION_GRANT_TYPE_EXTRA, "urn:soundcloud:oauth2:grant-type:google_plus&access_token=" + token);
            return login(bundle);
        } catch (Exception ex) {
            return new Result(ex);
        }
    }

}
