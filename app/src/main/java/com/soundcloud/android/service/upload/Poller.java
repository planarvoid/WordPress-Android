package com.soundcloud.android.service.upload;


import static com.soundcloud.android.service.upload.UploadService.TAG;

import com.soundcloud.android.AndroidCloudAPI;
import com.soundcloud.android.model.Track;
import com.soundcloud.api.Endpoints;
import com.soundcloud.api.Request;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;

import android.content.ContentResolver;
import android.content.Intent;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import java.io.IOException;

public class Poller extends Handler {
    private static final long DEFAULT_MIN_TIME_BETWEEN_REQUESTS = 5000;
    private static final long DEFAULT_MAX_TRIES = 12; // timeout of ~10 minutes with exp. back-off

    private AndroidCloudAPI mApi;
    private Request mRequest;
    private Uri mNotifyUri;

    private long mMinDelayBetweenRequests;

    public Poller(Looper looper, AndroidCloudAPI app, long trackId, Uri notifyUri) {
        this(looper, app, trackId, notifyUri, DEFAULT_MIN_TIME_BETWEEN_REQUESTS);
    }

    public Poller(Looper looper, AndroidCloudAPI api,
                  long trackId,
                  Uri notifyUri,
                  long delayBetweenRequests) {
        super(looper);
        mApi = api;
        mRequest = Request.to(Endpoints.TRACK_DETAILS, trackId);
        mNotifyUri = notifyUri;
        mMinDelayBetweenRequests = delayBetweenRequests;
    }

    public void start() {
        sendEmptyMessage(0);
    }

    @Override
    public void handleMessage(Message msg) {
        Track track = null;
        final int attempt = msg.what;
        if (Log.isLoggable(TAG, Log.DEBUG)) Log.d(TAG, "poll attempt "+(attempt+1));
        try {
            HttpResponse resp = mApi.get(mRequest);
            if (resp.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
                track = mApi.getMapper().readValue(resp.getEntity().getContent(), Track.class);
            } else {
                Log.w(TAG, "unexpected response " + resp.getStatusLine());
            }
        } catch (IOException e) {
            Log.e(TAG, "error", e);
        }

        if ((track == null || track.isProcessing()) && attempt < DEFAULT_MAX_TRIES-1) {
            final long backoff = attempt * attempt * 1000;
            sendEmptyMessageDelayed(attempt + 1, Math.max(backoff, mMinDelayBetweenRequests));
        } else {

            if (track != null && track.isFinished()) {
                onTrackProcessed(track);
            } else {
                if (track != null && track.isFailed()) {
                    // track failed to transcode
                    LocalBroadcastManager
                            .getInstance(mApi.getContext())
                            .sendBroadcast(new Intent(UploadService.TRANSCODING_FAILED)
                                    .putExtra(Track.EXTRA, track));
                }

                Log.e(TAG, "Track failed to be prepared " + track +
                        (track != null && track.state != null ? ", [state: " + track.state + "]" : ""));
            }
            getLooper().quit();
        }
    }

    private void onTrackProcessed(Track track) {
        if (Log.isLoggable(TAG, Log.DEBUG)) {
            Log.d(TAG, "Track successfully prepared by the api: " + track);
        }

        // local storage should reflect full track info
        ContentResolver resolver = mApi.getContext().getContentResolver();

        track.setUpdated();

        // this will tell any observers to update their UIs to the up to date track
        if (mNotifyUri != null) resolver.notifyChange(mNotifyUri, null, false);

        LocalBroadcastManager
                .getInstance(mApi.getContext())
                .sendBroadcast(new Intent(UploadService.TRANSCODING_SUCCESS)
                        .putExtra(Track.EXTRA, track));
    }
}
