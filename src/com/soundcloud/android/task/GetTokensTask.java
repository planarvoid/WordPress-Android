package com.soundcloud.android.task;

import static com.soundcloud.android.SoundCloudApplication.TAG;

import com.soundcloud.android.AndroidCloudAPI;
import com.soundcloud.android.activity.auth.LoginActivity;
import com.soundcloud.api.Token;

import android.os.Bundle;
import android.util.Log;

import java.io.IOException;

public class GetTokensTask extends AsyncApiTask<Bundle, Void, Token> {
    protected IOException mException;

    public GetTokensTask(AndroidCloudAPI api) {
        super(api);
    }

    @Override
    protected Token doInBackground(Bundle... params) {
        Bundle param = params[0];
        try {
            final String[] scopes = param.getStringArray(LoginActivity.SCOPES_EXTRA);

            if (param.containsKey(LoginActivity.CODE_EXTRA)) {
                return mApi.authorizationCode(param.getString(LoginActivity.CODE_EXTRA), scopes);
            } else if (param.containsKey(LoginActivity.USERNAME_EXTRA)
                    && param.containsKey(LoginActivity.PASSWORD_EXTRA)) {
                return mApi.login(param.getString(LoginActivity.USERNAME_EXTRA),
                        param.getString(LoginActivity.PASSWORD_EXTRA), scopes);
            } else if (param.containsKey(LoginActivity.EXTENSION_GRANT_TYPE_EXTRA)) {
                return mApi.extensionGrantType(param.getString(LoginActivity.EXTENSION_GRANT_TYPE_EXTRA), scopes);
            } else {
                throw new IllegalArgumentException("invalid param " + param);
            }
        } catch (IOException e) {
            mException = e;
            Log.e(TAG, "error logging in", e);
            return null;
        }
    }
}
