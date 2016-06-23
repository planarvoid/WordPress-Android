package com.soundcloud.android.analytics;

import com.soundcloud.android.utils.ErrorUtils;
import com.soundcloud.android.utils.Log;
import com.soundcloud.android.utils.NetworkConnectionHelper;
import com.soundcloud.propeller.ChangeResult;
import com.soundcloud.propeller.InsertResult;

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
    private final TrackingApiFactory apiFactory;

    TrackingHandler(Looper looper,
                    NetworkConnectionHelper networkConnectionHelper,
                    TrackingStorage storage,
                    TrackingApiFactory apiFactory) {
        super(looper);
        this.networkConnectionHelper = networkConnectionHelper;
        this.storage = storage;
        this.apiFactory = apiFactory;
    }

    @Override
    public void handleMessage(Message msg) {
        try {
            handleTrackingEvent(msg);
        } catch (Exception e) {
            ErrorUtils.handleSilentException(EventTracker.TAG, e);
        }
    }

    private void handleTrackingEvent(Message msg) {
        switch (msg.what) {
            case INSERT_TOKEN:
                try {
                    Log.d(EventTracker.TAG, "Inserting event: " + msg.obj + "\nthread=" + Thread.currentThread());
                    final InsertResult insertResult = storage.insertEvent((TrackingRecord) msg.obj);
                    if (!insertResult.success()) {
                        ErrorUtils.handleSilentException(
                                EventTracker.TAG,
                                new Exception("error inserting tracking event " + msg.obj, insertResult.getFailure()));
                    }
                } catch (UnsupportedEncodingException e) {
                    ErrorUtils.handleSilentException(EventTracker.TAG, e);
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

        if (networkConnectionHelper.isNetworkConnected()) {
            Log.d(EventTracker.TAG, "flushing tracking events (backend = " + backend + ")");
            List<TrackingRecord> events = backend == null ?
                                          storage.getPendingEvents() :
                                          storage.getPendingEventsForBackend(backend);

            if (!events.isEmpty()) {
                submitEvents(events, backend);
            }
        } else {
            Log.d(EventTracker.TAG, "not connected, skipping flush");
        }
    }

    private void submitEvents(List<TrackingRecord> events, String backend) {
        final List<TrackingRecord> submitted = apiFactory.create(backend).pushToRemote(events);
        if (!submitted.isEmpty()) {
            ChangeResult result = storage.deleteEvents(submitted);
            final int rowsDeleted = result.getNumRowsAffected();
            if (result.success() && submitted.size() == rowsDeleted) {
                Log.d(EventTracker.TAG, "submitted " + rowsDeleted + " events");
            } else {
                ErrorUtils.handleSilentException(
                        EventTracker.TAG, new Exception("Failed to delete some tracking events: failed = "
                                                                + (submitted.size() - rowsDeleted),
                                                        result.getFailure()));
            }
        }
    }
}
