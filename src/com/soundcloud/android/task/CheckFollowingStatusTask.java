package com.soundcloud.android.task;

import android.util.Log;
import com.soundcloud.android.CloudAPI;
import com.soundcloud.utils.http.Http;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpUriRequest;

import java.io.IOException;

import static com.soundcloud.android.SoundCloudApplication.TAG;

public class CheckFollowingStatusTask extends AsyncApiTask<Number, Void, Boolean> {
    public CheckFollowingStatusTask(CloudAPI api) {
        super(api);
    }

    @Override
    protected Boolean doInBackground(Number... params) {
        Number id = params[0];
        try {
            Log.v(TAG, "checking following status for id " + id);

            HttpUriRequest request = api().getRequest(MY_FOLLOWINGS + "/" + id, null);
            HttpResponse resp = Http.noRedirect(request);

            switch (resp.getStatusLine().getStatusCode()) {
                case SC_SEE_OTHER: return true;
                case SC_NOT_FOUND: return false;

                default:
                    Log.w(TAG, "unexpected return code "+ resp.getStatusLine());
                    return null;
            }
        } catch (IOException e) {
            Log.e(TAG, "error", e);
            return null;
        }
    }
}