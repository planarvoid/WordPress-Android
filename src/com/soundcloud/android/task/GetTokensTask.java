package com.soundcloud.android.task;

import static com.soundcloud.android.SoundCloudApplication.TAG;

import com.soundcloud.android.AndroidCloudAPI;
import com.soundcloud.api.CloudAPI;

import android.util.Log;
import android.util.Pair;

import java.io.IOException;
import java.util.Arrays;

public class GetTokensTask extends AsyncApiTask<String, Void, Pair<String, String>> {
    protected IOException mException;

    public GetTokensTask(AndroidCloudAPI api) {
        super(api);
    }

    @Override
    protected Pair<String, String> doInBackground(String... params) {
        if (params == null || params.length < 2) throw new IllegalArgumentException(Arrays.toString(params));
        String login = params[0];
        String password = params[1];
        try {
            CloudAPI api = api().login(login, password);
            return new Pair<String, String>(api.getToken(), api.getRefreshToken());
        } catch (IOException e) {
            mException = e;
            Log.e(TAG, "error logging in", e);
            return null;
        }
    }
}
