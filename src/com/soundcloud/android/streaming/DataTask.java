package com.soundcloud.android.streaming;

import com.soundcloud.android.AndroidCloudAPI;
import com.soundcloud.api.Request;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.util.EntityUtils;

import android.os.Bundle;
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
    public Bundle execute() throws IOException {
        Log.d(LOG_TAG, String.format("fetching chunk %d for item %s with range %s", chunkRange.start, item, byteRange));
        HttpResponse resp = api.getHttpClient().execute(
                Request.to(item.redirectUrl()).range(byteRange.start, byteRange.end()-1)
                        .buildRequest(HttpGet.class));

        final int status = resp.getStatusLine().getStatusCode();
        switch (status) {
            case HttpStatus.SC_OK:
            case HttpStatus.SC_PARTIAL_CONTENT:
                buffer.put(EntityUtils.toByteArray(resp.getEntity()));
                buffer.rewind();
                Bundle b = new Bundle();
                b.putInt("status", status);
                return b;

            // link has expired
            case HttpStatus.SC_FORBIDDEN:
                item.invalidateRedirectUrl();
                break;

            // permanent failure
            case HttpStatus.SC_PAYMENT_REQUIRED:
            case HttpStatus.SC_NOT_FOUND:
            case HttpStatus.SC_GONE:
                item.unavailable = true;
                break;

            default:
                throw new IOException("invalid status code received:" + resp.getStatusLine());
        }
        return null;
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
