package com.soundcloud.android.streaming;

import com.soundcloud.android.AndroidCloudAPI;
import com.soundcloud.api.Request;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.util.EntityUtils;

import android.util.Log;

import java.io.IOException;
import java.nio.ByteBuffer;

class DataTask extends StreamItemTask {
    static final String LOG_TAG = DataTask.class.getSimpleName();

    final Range byteRange, chunkRange;
    final ByteBuffer buffer;

    public DataTask(StreamItem item, Range chunkRange, Range byteRange, AndroidCloudAPI api) {
        super(item, api);
        if (byteRange == null) throw new IllegalArgumentException("byterange cannot be null");
        if (chunkRange == null) throw new IllegalArgumentException("chunkRange cannot be null");
        this.byteRange = byteRange;
        this.chunkRange = chunkRange;
        buffer = ByteBuffer.allocate(byteRange.length);
    }

    @Override
    public void execute() throws IOException {
        Log.d(LOG_TAG, String.format("fetching chunk %d for item %s with range %s", chunkRange.start, item, byteRange));
        HttpResponse resp = api.getHttpClient().execute(
                Request.to(item.redirectedURL).range(byteRange.start, byteRange.end()-1)
                        .buildRequest(HttpGet.class));

        if (resp.getStatusLine().getStatusCode() == HttpStatus.SC_OK ||
            resp.getStatusLine().getStatusCode() == HttpStatus.SC_PARTIAL_CONTENT) {
            buffer.put(EntityUtils.toByteArray(resp.getEntity()));
            buffer.rewind();
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
