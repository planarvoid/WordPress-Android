package com.soundcloud.android.streaming;

import com.soundcloud.android.AndroidCloudAPI;
import com.soundcloud.api.Request;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;

import android.util.Log;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;

class DataTask extends StreamItemTask {
    static final String LOG_TAG = DataTask.class.getSimpleName();

    final Range byteRange;
    final byte[] buffer;

    public DataTask(StreamItem item, Range byteRange, AndroidCloudAPI api) {
        super(item, api);
        if (byteRange == null) throw new IllegalArgumentException("byterange cannot be null");
        this.byteRange = byteRange;
        buffer = new byte[byteRange.length];
    }

    @Override
    public void execute() throws IOException {
        Log.d(LOG_TAG, "fetching " + item);
        HttpResponse resp = api.getHttpClient().execute(
                Request.to(item.redirectedURL).range(byteRange.location, byteRange.end())
                        .buildRequest(HttpGet.class));

        if (resp.getStatusLine().getStatusCode() == 200 || resp.getStatusLine().getStatusCode() == 206) {
            InputStream is = new BufferedInputStream(resp.getEntity().getContent());
            int n = is.read(buffer);
            Log.d(LOG_TAG, "read " + n + " bytes");
        } else {
            Log.w(LOG_TAG, "invalid status code received:" + resp.getStatusLine());
        }
    }
}
