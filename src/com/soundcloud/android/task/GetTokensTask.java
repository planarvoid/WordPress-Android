package com.soundcloud.android.task;

import static com.soundcloud.android.SoundCloudApplication.TAG;

import com.soundcloud.android.AndroidCloudAPI;
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
            final String scope = param.getString("scope");

            if (param.containsKey("code")) {
                return mApi.authorizationCode(param.getString("code"), scope);
            } else if (param.containsKey("username") && param.containsKey("password")) {
                return mApi.login(param.getString("username"), param.getString("password"), scope);
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
