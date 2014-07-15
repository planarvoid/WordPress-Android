package com.soundcloud.android.playback.streaming;

import com.soundcloud.android.api.legacy.PublicApi;
import com.soundcloud.android.api.legacy.PublicCloudAPI;
import com.soundcloud.android.utils.BufferUtils;
import com.soundcloud.android.utils.Log;
import com.soundcloud.api.Request;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpGet;

import android.content.Context;
import android.os.Build;
import android.os.Bundle;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.ByteBuffer;
import java.util.Locale;

abstract class DataTask extends StreamItemTask {
    static final String LOG_TAG = StreamLoader.LOG_TAG;
    static final String SUCCESS_KEY = "success";

    final Range byteRange, chunkRange;
    final ByteBuffer buffer;

    public DataTask(StreamItem item, Range chunkRange, Range byteRange, PublicCloudAPI api) {
        super(item, api);
        if (byteRange == null) throw new IllegalArgumentException("byterange cannot be null");
        if (chunkRange == null) throw new IllegalArgumentException("chunkRange cannot be null");
        if (item.getContentLength() > 0 &&
            byteRange.start > item.getContentLength()) {

            Log.w(LOG_TAG, String.format("requested range > contentlength (%d > %d)",
                    byteRange.start, item.getContentLength()));
        }
        this.byteRange = byteRange;
        this.chunkRange = chunkRange;
        buffer = ByteBuffer.allocate(byteRange.length);
    }

    protected abstract int getData(URL url, int start, int end, ByteBuffer dst) throws IOException;

    @Override
    public Bundle execute() throws IOException {
        Log.d(LOG_TAG, String.format("fetching chunk %d for item %s with range %s", chunkRange.start, item, byteRange));

        final Bundle b = new Bundle();
        final URL redirect = item.redirectUrl();
        if (redirect == null) {
            return b;
        }
        // need to rewind buffer - request might get retried later.
        buffer.rewind();

        final int status = getData(redirect, byteRange.start, byteRange.end() - 1, buffer);
        b.putInt("status", status);
        switch (status) {
            case HttpStatus.SC_OK:
            case HttpStatus.SC_PARTIAL_CONTENT:
                // already handled in getData()
                buffer.flip();
                b.putBoolean(SUCCESS_KEY, true);
                break;
            default:
                if (item.invalidateRedirectUrl(status)) {
                    Log.d(LOG_TAG, "invalidated redirect url");
                } else {
                    throw new IOException("unexpected status code received: " + status);
                }
        }
        return b;
    }

    @Override
    public String toString() {
        return "DataTask{" +
                "item=" + item +
                ", byteRange=" + byteRange +
                ", chunkRange=" + chunkRange +
                '}';
    }

    public static DataTask create(StreamItem item, Range chunkRange, Range range, Context context) {
        // google recommends using HttpURLConnection from Gingerbread on:
        // http://android-developers.blogspot.com/2011/09/androids-http-clients.html
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD) {
            return new HttpURLConnectionDataTask(item, chunkRange, range, new PublicApi(context));
        } else {
            return new HttpClientDataTask(item, chunkRange, range, new PublicApi(context));
        }
    }

    static class HttpClientDataTask extends DataTask {
        public HttpClientDataTask(StreamItem item, Range chunkRange, Range byteRange, PublicCloudAPI api) {
            super(item, chunkRange, byteRange, api);
        }

        @Override
        protected int getData(URL url, int start, int end, ByteBuffer dst) throws IOException {
            HttpGet get = new HttpGet(url.toString());
            get.setHeader("Range", Request.formatRange(start, end));
            HttpResponse resp = api.safeExecute(null, get);

            final int status = resp.getStatusLine().getStatusCode();
            switch (status) {
                case HttpStatus.SC_OK:
                case HttpStatus.SC_PARTIAL_CONTENT:
                    if (!BufferUtils.readBody(resp, buffer)) {
                        throw new IOException("error reading buffer");
                    }
            }
            return status;
        }
    }

    static class HttpURLConnectionDataTask extends DataTask {
        static final int READ_TIMEOUT = 10 * 1000;
        static final int CONNECTION_TIMEOUT = 10 * 1000;

        public HttpURLConnectionDataTask(StreamItem item, Range chunkRange, Range byteRange, PublicCloudAPI api) {
            super(item, chunkRange, byteRange, api);
        }

        @Override
        protected int getData(URL url, int start, int end, ByteBuffer dst) throws IOException {
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestProperty("Range",
                    String.format(Locale.ENGLISH, "bytes=%d-%d", start, end));

            connection.setReadTimeout(READ_TIMEOUT);
            connection.setConnectTimeout(CONNECTION_TIMEOUT);
            connection.setDoInput(true);
            connection.setDoOutput(false);
            connection.setRequestProperty("User-Agent", api.getUserAgent());
            connection.setUseCaches(false);
            InputStream is = null;
            try {
                connection.connect();
                final int status = connection.getResponseCode();
                switch (status) {
                    case HttpStatus.SC_OK:
                    case HttpStatus.SC_PARTIAL_CONTENT:
                        if (dst.remaining() < connection.getContentLength()) {
                            throw new IOException(String.format(Locale.ENGLISH, "allocated buffer is too small (%d < %d)",
                                        dst.remaining(), connection.getContentLength()));
                        }
                        is = new BufferedInputStream(connection.getInputStream());
                        final byte[] bytes = new byte[8192];
                        int n;
                        while ((n = is.read(bytes)) != -1) {
                            dst.put(bytes, 0, n);
                        }
                }
                return status;
            } finally {
                if (is != null) is.close();
                connection.disconnect();
            }
        }
    }
}
