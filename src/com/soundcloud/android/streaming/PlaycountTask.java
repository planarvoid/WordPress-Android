package com.soundcloud.android.streaming;

import com.soundcloud.android.AndroidCloudAPI;
import com.soundcloud.api.Request;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;

import android.net.Uri;
import android.os.Bundle;
import android.util.Log;

import java.io.IOException;

public class PlaycountTask extends StreamItemTask {
    static final String LOG_TAG = StreamLoader.class.getSimpleName();

    public PlaycountTask(StreamItem item, AndroidCloudAPI api) {
        super(item, api);
    }

    @Override
    public Bundle execute() throws IOException {
        if (Log.isLoggable(StreamLoader.LOG_TAG, Log.DEBUG))
            Log.d(LOG_TAG, "Logging playcount for item "+item);

        // request 1st byte to get counted as play
        HttpResponse resp = api.get(Request.to(Uri.parse(item.url).getPath()).range(0, 1));

        final int status = resp.getStatusLine().getStatusCode();
        switch (status) {
            case HttpStatus.SC_MOVED_TEMPORARILY:
                if (Log.isLoggable(StreamLoader.LOG_TAG, Log.DEBUG))
                    Log.d(LOG_TAG, "logged playcount for "+item);
                return null;
            default:
                throw new IOException("invalid status code received:" + resp.getStatusLine());
        }
    }
}
