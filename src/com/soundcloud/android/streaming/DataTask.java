package com.soundcloud.android.streaming;

import com.soundcloud.api.CloudAPI;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;

import android.net.http.AndroidHttpClient;
import android.text.TextUtils;

import java.io.IOException;

class DataTask implements Runnable {
    StreamItem mItem;
    Range mByteRange;

    AndroidHttpClient mClient;
    HttpResponse mResponse;

    public boolean executed = false;

    public DataTask(StreamItem item) {
        mItem = item;
        mClient = AndroidHttpClient.newInstance(CloudAPI.USER_AGENT);
    }

    public DataTask(StreamItem item, Range byteRange) {
        this(item);
        mByteRange = byteRange;
    }

    protected HttpUriRequest buildRequest() {

        boolean useRedirectedUrl = false;
        if (!TextUtils.isEmpty(mItem.redirectedURL) && !(mByteRange.location == 0)) {
            useRedirectedUrl = true;
        }

        final HttpGet method = new HttpGet(useRedirectedUrl ? mItem.redirectedURL :
                mItem.url);

        // method.setHeader("Range", "bytes=" + get + "-");

        return method;
    }

    public boolean execute() {
        try {
            mResponse = mClient.execute(buildRequest());
            return true;
        } catch (IOException e) {
            e.printStackTrace();
        }
        executed = true;
        return false;
    }

    @Override
    public void run() {

    }
}
