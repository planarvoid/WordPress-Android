package com.soundcloud.android.c2dm;

import static com.soundcloud.android.c2dm.C2DMReceiver.TAG;

import com.soundcloud.android.AndroidCloudAPI;
import com.soundcloud.android.task.AsyncApiTask;
import com.soundcloud.api.Request;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;

import android.os.PowerManager;
import android.util.Log;

import java.io.IOException;

public class SendRegIdTask extends AsyncApiTask<String,Void, String> {
    private PowerManager.WakeLock lock;

    public SendRegIdTask(AndroidCloudAPI api, PowerManager.WakeLock wakeLock) {
        super(api);
        lock = wakeLock;
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        if (lock != null) lock.acquire();
    }

    @Override
    protected void onPostExecute(String location) {
        super.onPostExecute(location);
        if (lock != null) lock.release();
    }

    @Override
    protected String doInBackground(String... params) {
        if (params.length < 2) throw new IllegalArgumentException("need reg_id and app_identifier");

        try {
            HttpResponse resp = mApi.post(Request.to("/me/devices").with(
                    "device_token",   params[0],
                    "app_identifier", params[1]));

           if (resp.getStatusLine().getStatusCode() == HttpStatus.SC_CREATED) {
                return resp.getFirstHeader("Location").getValue();
            } else {
                Log.w(TAG, "error registering device, unexpected status "+resp.getStatusLine());
                return null;
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
