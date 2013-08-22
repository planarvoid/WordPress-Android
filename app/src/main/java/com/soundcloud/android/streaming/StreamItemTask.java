package com.soundcloud.android.streaming;

import com.soundcloud.android.AndroidCloudAPI;

import android.os.Bundle;

import java.io.IOException;

public abstract class StreamItemTask {
    final StreamItem item;
    final AndroidCloudAPI api;

    public StreamItemTask(StreamItem item, AndroidCloudAPI api) {
        this.item = item;
        this.api = api;
    }

    public abstract Bundle execute() throws IOException;

    @Override
    public String toString() {
        return getClass().getSimpleName()+"{" +
                "item=" + item + '}';
    }
}
