package com.soundcloud.android.streaming;

import com.soundcloud.android.AndroidCloudAPI;
import com.soundcloud.api.Request;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpGet;

import android.os.Bundle;
import android.util.Log;

import java.io.IOException;

public class PlaycountTask extends StreamItemTask {
    static final String LOG_TAG = PlaycountTask.class.getSimpleName();

    public PlaycountTask(StreamItem item, AndroidCloudAPI api) {
        super(item, api);
    }

    @Override
    public Bundle execute() throws IOException {
        Log.d(LOG_TAG, "Logging playcount for item "+item);
        HttpResponse resp = api.getHttpClient().execute(
                Request.to(item.redirectUrl()).range(0, 1)
                        .buildRequest(HttpGet.class));

        final int status = resp.getStatusLine().getStatusCode();
        switch (status) {
            case HttpStatus.SC_OK:
            case HttpStatus.SC_PARTIAL_CONTENT:
                Log.d(LOG_TAG, "logged play count for "+item);
                return null;
            default:
                throw new IOException("invalid status code received:" + resp.getStatusLine());
        }
    }
}
