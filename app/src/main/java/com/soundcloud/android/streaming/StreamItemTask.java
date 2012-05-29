package com.soundcloud.android.streaming;

import com.soundcloud.android.AndroidCloudAPI;
import org.apache.http.HttpResponse;

import android.net.Uri;
import android.os.Bundle;
import android.util.Log;

import java.io.IOException;

public abstract class StreamItemTask implements Runnable {
    final StreamItem item;
    final AndroidCloudAPI api;
    private boolean executed;

    public StreamItemTask(StreamItem item, AndroidCloudAPI api) {
        this.item = item;
        this.api = api;
    }

    @Override
    public void run() {
        try {
            execute();
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            executed = true;
        }
    }

    public abstract Bundle execute() throws IOException;

    @Override
    public String toString() {
        return getClass().getSimpleName()+"{" +
                "item=" + item +
                ", executed=" + executed +
                '}';
    }
}
