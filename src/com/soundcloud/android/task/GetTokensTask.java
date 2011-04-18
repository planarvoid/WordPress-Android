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
        if (params == null || params.length < 2) throw new IllegalArgumentException(Arrays.toString(params));
        String login = params[0];
        String password = params[1];
        try {
            return api().login(login, password);
        } catch (IOException e) {
            mException = e;
            Log.e(TAG, "error logging in", e);
            return null;
        }
    }
}
