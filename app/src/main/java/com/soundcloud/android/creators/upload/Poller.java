package com.soundcloud.android.creators.upload;


import static com.soundcloud.android.creators.upload.UploadService.TAG;

import com.soundcloud.android.api.legacy.PublicCloudAPI;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.api.legacy.model.PublicApiTrack;
import com.soundcloud.android.storage.TrackStorage;
import com.soundcloud.api.Endpoints;
import com.soundcloud.api.Request;

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

    private final PublicCloudAPI api;
    private final Request request;
    private final Uri notifyUri;

    private final long minDelayBetweenRequests;

    public Poller(Looper looper, PublicCloudAPI app, long trackId, Uri notifyUri) {
        this(looper, app, trackId, notifyUri, DEFAULT_MIN_TIME_BETWEEN_REQUESTS);
    }

    public Poller(Looper looper, PublicCloudAPI api,
                  long trackId,
                  Uri notifyUri,
                  long delayBetweenRequests) {
        super(looper);
        this.api = api;
        request = Request.to(Endpoints.TRACK_DETAILS, trackId);
        this.notifyUri = notifyUri;
        minDelayBetweenRequests = delayBetweenRequests;
    }

    public void start() {
        sendEmptyMessage(0);
    }

    @Override
    public void handleMessage(Message msg) {
        PublicApiTrack track = null;
        final int attempt = msg.what;
        if (Log.isLoggable(TAG, Log.DEBUG)) Log.d(TAG, "poll attempt "+(attempt+1));
        try {
            track = api.read(request);
        } catch (IOException e) {
            Log.e(TAG, "error", e);
        }

        if ((track == null || track.isProcessing()) && attempt < DEFAULT_MAX_TRIES-1) {
            final long backoff = attempt * attempt * 1000;
            sendEmptyMessageDelayed(attempt + 1, Math.max(backoff, minDelayBetweenRequests));
        } else {

            if (track != null && track.isFinished()) {
                onTrackProcessed(track);
            } else {
                if (track != null && track.isFailed()) {
                    // track failed to transcode
                    persistTrack(track);

                    LocalBroadcastManager
                            .getInstance(SoundCloudApplication.instance)
                            .sendBroadcast(new Intent(UploadService.TRANSCODING_FAILED)
                                    .putExtra(PublicApiTrack.EXTRA, track));
                }
                Log.e(TAG, "Track failed to be prepared " + track +
                        (track != null && track.state != null ? ", [state: " + track.state + "]" : ""));
            }
            getLooper().quit();
        }
    }

    private void onTrackProcessed(PublicApiTrack track) {
        if (Log.isLoggable(TAG, Log.DEBUG)) {
            Log.d(TAG, "Track successfully prepared by the api: " + track);
        }

        // local storage should reflect full track info
        ContentResolver resolver = SoundCloudApplication.instance.getContentResolver();
        persistTrack(track);

        // this will tell any observers to update their UIs to the up to date track
        if (notifyUri != null) resolver.notifyChange(notifyUri, null, false);

        LocalBroadcastManager
                .getInstance(SoundCloudApplication.instance)
                .sendBroadcast(new Intent(UploadService.TRANSCODING_SUCCESS)
                        .putExtra(PublicApiTrack.EXTRA, track));
    }


    private void persistTrack(PublicApiTrack track) {
        track.setUpdated();
        new TrackStorage().createOrUpdate(track);
    }

}
