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
    private final boolean skipLogging;

    public HeadTask(StreamItem item, AndroidCloudAPI api, boolean skipPlayLogging) {
        super(item, api);
        this.skipLogging = skipPlayLogging;
    }

    @Override
    public Bundle execute() throws IOException {
        Bundle b = new Bundle();
        try {
            Stream stream = api.resolveStreamUrl(item.url.toString(), skipLogging);
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
                    item.markUnavailable(e.getStatusCode());
                    return b;

                default:
                    item.setHttpError(e.getStatusCode());
                    throw e;
            }
        }
    }
}