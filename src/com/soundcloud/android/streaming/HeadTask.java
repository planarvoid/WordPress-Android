package com.soundcloud.android.streaming;

import com.soundcloud.android.AndroidCloudAPI;
import com.soundcloud.api.CloudAPI;
import com.soundcloud.api.Stream;
import org.apache.http.HttpStatus;

import android.os.Bundle;
import android.util.Log;

import java.io.IOException;

class HeadTask extends StreamItemTask implements HttpStatus {
    static final String LOG_TAG = StreamLoader.LOG_TAG;

    public HeadTask(StreamItem item, AndroidCloudAPI api) {
        super(item, api);
    }

    @Override
    public Bundle execute() throws IOException {
        Bundle b = new Bundle();
        try {
            Stream stream = api.resolveStreamUrl(item.url.toString());
//            b.putSerializable("stream", stream);
            item.initializeFromStream(stream);
            b.putBoolean("success", true);
            return b;
        } catch (CloudAPI.ResolverException e) {
            Log.w(LOG_TAG, "error resolving " + item, e);
            b.putInt("status", e.getStatusCode());
            switch (e.getStatusCode()) {
                case SC_PAYMENT_REQUIRED:
                case SC_NOT_FOUND:
                case SC_GONE:
                    item.markUnavailable();
                    return b;

                default:
                    throw e;
            }
        }
    }
}