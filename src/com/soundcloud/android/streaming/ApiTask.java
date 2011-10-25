package com.soundcloud.android.streaming;

import com.soundcloud.android.AndroidCloudAPI;
import org.apache.http.HttpResponse;

import android.util.Log;

import java.io.IOException;

public abstract class ApiTask implements Runnable {
    final StreamItem item;
    final AndroidCloudAPI api;

    HttpResponse response;
    public boolean executed;

    public ApiTask(StreamItem item, AndroidCloudAPI api) {
        this.item = item;
        this.api = api;
    }

    @Override
    public void run() {
        try {
            handleResponse();
        } catch (IOException e) {
            Log.w(getClass().getSimpleName(), e);
        }
    }

    public boolean execute() {
        Log.d(getClass().getSimpleName(), "execute");

        try {
            response = performRequest();
            return true;
        } catch (IOException e) {
            Log.w(getClass().getSimpleName(), e);
        }
        executed = true;
        return false;
    }


    public abstract void handleResponse() throws IOException;
    protected abstract HttpResponse performRequest() throws IOException;


    @Override
    public String toString() {
        return getClass().getSimpleName()+"{" +
                "item=" + item +
                ", response=" + response +
                '}';
    }
}
