package com.soundcloud.android.task;

import android.net.Uri;
import android.util.Log;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.task.fetch.FetchTrackTask;
import com.soundcloud.api.Request;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;

import java.io.IOException;
import java.util.logging.Logger;

import static com.soundcloud.android.SoundCloudApplication.TAG;

public class PollUploadedTrackTask extends FetchTrackTask {

    private static long MAX_EXECUTION_TIME = 60000; // TODO, how long is too long?
    private static long MIN_TIME_BETWEEN_REQUESTS = 5000;

    private SoundCloudApplication mApp;
    private Uri mNotifyUri;

    public PollUploadedTrackTask(SoundCloudApplication app, long trackId, Uri notifyUri) {
        super(app, trackId);
        mApp = app;
        mNotifyUri = notifyUri;
    }

    @Override
    protected Track doInBackground(Request... request) {
        if (request == null || request.length == 0)
            throw new IllegalArgumentException("need path to executeAppendTask");


        Track track = null;
        final long start = System.currentTimeMillis();
        long lastRequest = 0;
        do {
            try {
                final long timeSince = System.currentTimeMillis() - lastRequest;
                if ( timeSince < MIN_TIME_BETWEEN_REQUESTS){
                    Thread.sleep(MIN_TIME_BETWEEN_REQUESTS - timeSince);
                }

                lastRequest = System.currentTimeMillis();
                HttpResponse resp = mApp.get(request[0]);
                if (isCancelled()) return null;

                if (resp.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
                    track = mApp.getMapper().readValue(resp.getEntity().getContent(), Track.class);
                } else {
                    Log.w(TAG, "unexpected response " + resp.getStatusLine());
                }
            } catch (IOException e) {
                Log.e(TAG, "error", e);
            } catch (InterruptedException e) {
                Log.e(TAG, "error", e);
            }
        } while ((track == null || track.state.isProcessing()) && System.currentTimeMillis() - start < MAX_EXECUTION_TIME);

        if (track != null && !track.state.isProcessing()){
            if (Log.isLoggable(SoundCloudApplication.TAG, Log.DEBUG)){
                Log.i(SoundCloudApplication.TAG, "Track succesfully prepared by the api: " + track);
            }
            updateLocally(mApp.getContentResolver(),track);
            // this will tell any observers to update their UIs to the new state
            if (mNotifyUri != null) mApp.getContentResolver().notifyChange(mNotifyUri,null,false);
        } else {
            Log.e(SoundCloudApplication.TAG, "Track failed to be prepared " + track + ", [state: " + track.state.value() + "]");
        }

        return track;
    }
}
