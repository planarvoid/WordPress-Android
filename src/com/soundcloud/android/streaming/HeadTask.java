package com.soundcloud.android.streaming;

import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.client.methods.HttpUriRequest;

import android.util.Log;

class HeadTask extends DataTask {
    public HeadTask(StreamItem item) {
        super(item);
    }

    @Override
    protected HttpUriRequest buildRequest(){
       return new HttpHead(mItem.redirectedURL);
    }

    @Override
    public void run() {
        if (mResponse.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
            Log.i(getClass().getSimpleName(), "invalid status received: " + mResponse.getStatusLine().toString());
        } else {
            mItem.setContentLength(getContentLength(mResponse));
        }
        //mHeadTasks.remove(this);
    }

    private long getContentLength(HttpResponse resp) {
        Header h = resp.getFirstHeader("Content-Length");
        if (h != null) {
            try {
                return Long.parseLong(h.getValue());
            } catch (NumberFormatException e) {
                return -1;
            }
        } else {
            return -1;
        }
    }

    public StreamItem getItem() {
        return mItem;
    }
}
