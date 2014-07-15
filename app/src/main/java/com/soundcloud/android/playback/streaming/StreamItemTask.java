package com.soundcloud.android.playback.streaming;

import com.soundcloud.android.api.legacy.PublicCloudAPI;

import android.os.Bundle;

import java.io.IOException;

public abstract class StreamItemTask {
    final StreamItem item;
    final PublicCloudAPI api;

    public StreamItemTask(StreamItem item, PublicCloudAPI api) {
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
