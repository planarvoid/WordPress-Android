package com.soundcloud.android.streaming;

import com.soundcloud.android.AndroidCloudAPI;
import com.soundcloud.api.Request;
import org.apache.http.HttpResponse;

import android.text.TextUtils;
import android.util.Log;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;

class DataTask extends ApiTask {
    final Range byteRange;
    final byte[] buffer;

    public DataTask(StreamItem item, Range byteRange, AndroidCloudAPI api) {
        super(item, api);
        if (byteRange == null) throw new IllegalArgumentException("byterange cannot be null");
        this.byteRange = byteRange;
        buffer = new byte[byteRange.length];
    }

    @Override
    protected HttpResponse performRequest() throws IOException {
        return api.get(buildRequest());
    }

    @Override
    public void handleResponse() throws IOException {

        if (response.getStatusLine().getStatusCode() == 200 || response.getStatusLine().getStatusCode() == 206) {
            InputStream is = new BufferedInputStream(response.getEntity().getContent());
            int n = is.read(buffer);
            Log.d(getClass().getSimpleName(), "read "+n+" bytes");
        } else {
            Log.w(getClass().getSimpleName(), "invalid status code received:"+response.getStatusLine());
        }
    }

    private Request buildRequest() {
        boolean useRedirectedUrl = !TextUtils.isEmpty(item.redirectedURL) && byteRange.location > 0;
        final Request request = Request.to(useRedirectedUrl ? item.redirectedURL : item.url);
        if (byteRange != null) {
            request.range(byteRange.location, byteRange.end());
        }
        return request;
    }
}
