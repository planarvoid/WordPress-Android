package com.soundcloud.android.analytics;

import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.utils.Log;
import com.soundcloud.android.utils.NetworkConnectionHelper;
import com.soundcloud.propeller.ChangeResult;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;

import java.io.UnsupportedEncodingException;
import java.util.List;

/**
 * Processes messages received from {@link com.soundcloud.android.analytics.EventTracker}, such as inserting an event
 * into the tracking database or flushing existing events to the respective tracking backend.
 */
class TrackingHandler extends Handler {

    static final int INSERT_TOKEN = 0;
    static final int FLUSH_TOKEN = 1;
    static final int FINISH_TOKEN = 0xDEADBEEF;

    private final NetworkConnectionHelper networkConnectionHelper;
    private final TrackingStorage storage;
    private final TrackingApi api;

    TrackingHandler(Looper looper, NetworkConnectionHelper networkConnectionHelper, TrackingStorage storage, TrackingApi api) {
        super(looper);
        this.networkConnectionHelper = networkConnectionHelper;
        this.storage = storage;
        this.api = api;
    }

    @Override
    public void handleMessage(Message msg) {
        try {
            handleTrackingEvent(msg);
        } catch (Exception e) {
            SoundCloudApplication.handleSilentException(EventTracker.TAG, e);
        }
    }

    private void handleTrackingEvent(Message msg) {
        switch (msg.what) {
            case INSERT_TOKEN:
                try {
                    if (!storage.insertEvent((TrackingEvent) msg.obj).success()) {
                        SoundCloudApplication.handleSilentException(
                                EventTracker.TAG, new Exception("error inserting tracking event " + msg.obj));
                    }
                } catch (UnsupportedEncodingException e) {
                    SoundCloudApplication.handleSilentException(EventTracker.TAG, e);
                }
                break;

            case FLUSH_TOKEN:
                flushTrackingEvents(msg);
                break;

            case FINISH_TOKEN:
                Log.d(EventTracker.TAG, "Shutting down.");
                removeCallbacksAndMessages(null);
                getLooper().quit();
                break;

            default:
                break;
        }
    }

    private void flushTrackingEvents(Message flushMessage) {
        final String backend = (String) flushMessage.obj;

        if (networkConnectionHelper.networkIsConnected()) {
            Log.d(EventTracker.TAG, "flushing tracking events (backend = " + backend + ")");
            List<TrackingEvent> events = backend == null ? storage.getPendingEvents() : storage.getPendingEventsForBackend(backend);

            if (!events.isEmpty()) {
                submitEvents(events);
            }
        } else {
            Log.d(EventTracker.TAG, "not connected, skipping flush");
        }
    }

    private void submitEvents(List<TrackingEvent> events) {
        final List<TrackingEvent> submitted = api.pushToRemote(events);
        if (!submitted.isEmpty()) {
            ChangeResult result = storage.deleteEvents(submitted);
            final int rowsDeleted = result.getNumRowsAffected();
            if (result.success() && submitted.size() == rowsDeleted) {
                Log.d(EventTracker.TAG, "submitted " + rowsDeleted + " events");
            } else {
                SoundCloudApplication.handleSilentException(
                        EventTracker.TAG, new Exception("Failed to delete some tracking events: failed = "
                                + (submitted.size() - rowsDeleted)));
            }
        }
    }
}
