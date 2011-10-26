package com.soundcloud.android.streaming;

import com.soundcloud.android.AndroidCloudAPI;
import com.soundcloud.api.Request;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.util.EntityUtils;

import android.util.Log;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

class DataTask extends StreamItemTask {
    static final String LOG_TAG = DataTask.class.getSimpleName();

    final Range byteRange, chunkRange;
    final byte[] buffer;

    public DataTask(StreamItem item, Range chunkRange, Range byteRange, AndroidCloudAPI api) {
        super(item, api);
        if (byteRange == null) throw new IllegalArgumentException("byterange cannot be null");
        if (chunkRange == null) throw new IllegalArgumentException("chunkRange cannot be null");
        this.byteRange = byteRange;
        this.chunkRange = chunkRange;
        buffer = new byte[byteRange.length];
    }

    @Override
    public void execute() throws IOException {
        Log.d(LOG_TAG, String.format("fetching item %s with range %s", item, byteRange));
        HttpResponse resp = api.getHttpClient().execute(
                Request.to(item.redirectedURL).range(byteRange.location, byteRange.end()-1)
                        .buildRequest(HttpGet.class));

        if (resp.getStatusLine().getStatusCode() == HttpStatus.SC_OK ||
            resp.getStatusLine().getStatusCode() == HttpStatus.SC_PARTIAL_CONTENT) {
            final byte[] read = EntityUtils.toByteArray(resp.getEntity());

            if (read.length <= buffer.length) {
                System.arraycopy(read, 0, buffer, 0, read.length);
            } else {
                Log.w(LOG_TAG, "read more data than requested!");
            }
        } else {
            Log.w(LOG_TAG, "invalid status code received:" + resp.getStatusLine());
        }
    }

    @Override
    public String toString() {
        return "DataTask{" +
                "item=" + item +
                ", byteRange=" + byteRange +
                ", chunkRange=" + chunkRange +
                '}' ;
    }
}
