package com.soundcloud.android.streaming;

import com.soundcloud.android.AndroidCloudAPI;
import com.soundcloud.api.Stream;

import android.util.Log;

import java.io.IOException;

class HeadTask extends StreamItemTask {
    static final String LOG_TAG = HeadTask.class.getSimpleName();

    public HeadTask(StreamItem item, AndroidCloudAPI api) {
        super(item, api);
    }

    @Override
    public void execute() throws IOException {
        item.initializeFrom(api.resolveStreamUrl(item.url));
    }
}