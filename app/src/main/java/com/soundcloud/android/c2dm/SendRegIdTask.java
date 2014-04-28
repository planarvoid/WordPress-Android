package com.soundcloud.android.c2dm;

import static com.soundcloud.android.c2dm.C2DMReceiver.TAG;

import com.soundcloud.android.api.PublicCloudAPI;
import com.soundcloud.android.api.AsyncApiTask;
import com.soundcloud.api.Request;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;

import android.util.Log;

import java.io.IOException;

public class SendRegIdTask extends AsyncApiTask<String,Void, String> {
    private static final String DEVICE_ENDPOINT = "/me/devices";

    public SendRegIdTask(PublicCloudAPI api) {
        super(api);
    }

    @Override
    protected String doInBackground(String... params) {
        if (params.length < 3)
            throw new IllegalArgumentException("need reg_id, app_identifier and device");

        try {
            HttpResponse resp = api.post(Request.to(DEVICE_ENDPOINT).with(
                    "device_token",   params[0],
                    "app_identifier", params[1],
                    "device",         params[2]));

            if (resp.getStatusLine().getStatusCode() == HttpStatus.SC_CREATED) {
                final Header location = resp.getFirstHeader("Location");
                if (location != null) {
                    return location.getValue();
                } else {
                    Log.w(TAG, "error registering device, location header missing");
                    return null;
                }
            } else {
                Log.w(TAG, "error registering device, unexpected status " + resp.getStatusLine());
                return null;
            }
        } catch (IOException e) {
            Log.w(TAG, "error registering device, unexpected error", e);
            return null;
        }
    }
}
