package com.soundcloud.android.task;

import static com.soundcloud.android.SoundCloudApplication.TAG;

import com.soundcloud.android.AndroidCloudAPI;
import com.soundcloud.android.activity.auth.AbstractLoginActivity;
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
        if (Log.isLoggable(TAG, Log.DEBUG)) Log.d(TAG, "GetTokensTask#doInBackGround("+param+")");
        try {
            final String[] scopes = param.getStringArray(AbstractLoginActivity.SCOPES_EXTRA);

            if (param.containsKey(AbstractLoginActivity.CODE_EXTRA)) {
                return mApi.authorizationCode(param.getString(AbstractLoginActivity.CODE_EXTRA), scopes);
            } else if (param.containsKey(AbstractLoginActivity.USERNAME_EXTRA)
                    && param.containsKey(AbstractLoginActivity.PASSWORD_EXTRA)) {
                return mApi.login(param.getString(AbstractLoginActivity.USERNAME_EXTRA),
                        param.getString(AbstractLoginActivity.PASSWORD_EXTRA), scopes);
            } else if (param.containsKey(AbstractLoginActivity.EXTENSION_GRANT_TYPE_EXTRA)) {
                return mApi.extensionGrantType(param.getString(AbstractLoginActivity.EXTENSION_GRANT_TYPE_EXTRA), scopes);
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
