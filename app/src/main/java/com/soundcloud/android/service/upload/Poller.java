package com.soundcloud.android.service.upload;

import static com.soundcloud.android.SoundCloudApplication.TAG;

import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.model.Track;
import com.soundcloud.api.Endpoints;
import com.soundcloud.api.Request;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;

import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import java.io.IOException;

public class Poller extends Handler {
    private static final long DEFAULT_MAX_EXECUTION_TIME = 60 * 1000 * 5; // 5 minutes
    private static final long DEFAULT_MIN_TIME_BETWEEN_REQUESTS = 5000;
    private static final long DEFAULT_MAX_TRIES = 10;

    private SoundCloudApplication mApp;
    private Request mRequest;
    private Uri mNotifyUri;
    private long mFirstAttempt;

    private long mMinDelayBetweenRequests;
    private long mMaxExecutionTime;

    public Poller(Looper looper, SoundCloudApplication app, long trackId, Uri notifyUri) {
        this(looper, app, trackId, notifyUri, DEFAULT_MIN_TIME_BETWEEN_REQUESTS, DEFAULT_MAX_EXECUTION_TIME);
    }

    public Poller(Looper looper, SoundCloudApplication app,
                  long trackId,
                  Uri notifyUri,
                  long delayBetweenRequests,
                  long maxExecutionTime) {
        super(looper);
        mApp = app;
        mRequest = Request.to(Endpoints.TRACK_DETAILS, trackId);
        mNotifyUri = notifyUri;
        mMinDelayBetweenRequests = delayBetweenRequests;
        mMaxExecutionTime = maxExecutionTime;
    }

    public void start() {
        sendEmptyMessage(0);
    }

    @Override
    public void handleMessage(Message msg) {
        if (msg.what == 0) {
            mFirstAttempt = msg.getWhen();
        }

        Track track = null;
        final int attempt = msg.what;
        if (Log.isLoggable(TAG, Log.DEBUG)) Log.d(TAG, "poll attempt "+(attempt+1));
        try {
            HttpResponse resp = mApp.get(mRequest);
            if (resp.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
                track = mApp.getMapper().readValue(resp.getEntity().getContent(), Track.class);
            } else {
                Log.w(TAG, "unexpected response " + resp.getStatusLine());
            }
        } catch (IOException e) {
            Log.e(TAG, "error", e);
        }

        if ((track == null || track.state.isProcessing()) &&
                attempt < DEFAULT_MAX_TRIES-1 &&
                (msg.getWhen() - mFirstAttempt) < mMaxExecutionTime) {

            final long backoff = attempt * attempt * 1000;
            sendEmptyMessageDelayed(attempt + 1, Math.max(backoff, mMinDelayBetweenRequests));
        } else {

            if (track != null && !track.state.isProcessing()) {
                onTrackProcessed(track);
            } else {
                Log.e(TAG, "Track failed to be prepared " + track +
                        (track != null && track.state != null ? ", [state: " + track.state + "]" : ""));
            }
            getLooper().quit();
        }
    }

    private void onTrackProcessed(Track track) {
        // local storage should reflect full track info
        track.commitLocally(mApp.getContentResolver(), SoundCloudApplication.TRACK_CACHE);

        // this will tell any observers to update their UIs to the up to date track
        if (mNotifyUri != null) mApp.getContentResolver().notifyChange(mNotifyUri, null, false);

        if (Log.isLoggable(TAG, Log.DEBUG)) {
            Log.d(TAG, "Track succesfully prepared by the api: " + track);
        }
    }
}
