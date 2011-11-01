package com.soundcloud.android.streaming;

import com.soundcloud.android.AndroidCloudAPI;
import com.soundcloud.api.CloudAPI;
import com.soundcloud.api.Stream;
import org.apache.http.HttpStatus;

import android.os.Bundle;

import java.io.IOException;

class HeadTask extends StreamItemTask implements HttpStatus {
    static final String LOG_TAG = HeadTask.class.getSimpleName();

    public HeadTask(StreamItem item, AndroidCloudAPI api) {
        super(item, api);
    }

    @Override
    public Bundle execute() throws IOException {
        try {
            Stream stream = api.resolveStreamUrl(item.url);
            Bundle b = new Bundle();
//            b.putSerializable("stream", stream);
            item.initializeFrom(stream);
            return b;
        } catch (CloudAPI.ResolverException e) {
            switch (e.getStatusCode()) {
                case SC_PAYMENT_REQUIRED:
                case SC_NOT_FOUND:
                case SC_GONE:
                    item.unavailable = true;
                    break;
            }
            return null;
        }
    }
}