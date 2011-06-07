package com.soundcloud.android.task;

import static com.soundcloud.android.SoundCloudApplication.TAG;

import com.soundcloud.android.AndroidCloudAPI;
import com.soundcloud.api.Token;

import android.util.Log;

import java.io.IOException;
import java.util.Arrays;

public class GetTokensTask extends AsyncApiTask<String, Void, Token> {
    protected IOException mException;

    public GetTokensTask(AndroidCloudAPI api) {
        super(api);
    }

    @Override
    protected Token doInBackground(String... params) {
        try {
            switch (params.length) {
                case 0: throw new IllegalArgumentException("need at least one parameter");
                case 1: return mApi.authorizationCode(params[0]);
                case 2: return mApi.login(params[0], params[1]);
                default:throw new IllegalArgumentException("too many parameters");
            }
        } catch (IOException e) {
            mException = e;
            Log.e(TAG, "error logging in", e);
            return null;
        }
    }
}
