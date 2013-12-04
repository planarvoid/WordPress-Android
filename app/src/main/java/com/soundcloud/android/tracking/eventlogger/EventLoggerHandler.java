package com.soundcloud.android.tracking.eventlogger;

import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.events.PlaybackEventData;
import com.soundcloud.android.utils.IOUtils;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.util.Pair;

import java.util.List;

public class EventLoggerHandler extends Handler {

    private Context mContext;
    private EventLoggerDbHelper mTrackingDbHelper;
    private EventLoggerApi mTrackingApi;

    public EventLoggerHandler(Looper looper, Context context,
                              EventLoggerDbHelper eventLoggerDbHelper, EventLoggerApi trackingApi) {
        super(looper);
        mContext = context;
        mTrackingDbHelper = eventLoggerDbHelper;
        mTrackingApi = trackingApi;
    }

    @Override
    public void handleMessage(Message msg) {
        try {
            handleTrackingEvent(msg);
        } catch (Exception e) {
            SoundCloudApplication.handleSilentException("Error in tracking handler", e);
        }
    }

    private void handleTrackingEvent(Message msg) {
        switch (msg.what) {
            case EventLogger.INSERT_TOKEN:
                final PlaybackEventData params = (PlaybackEventData) msg.obj;
                long id = mTrackingDbHelper.insertEvent(params);
                if (id < 0) {
                    Log.w(EventLogger.TAG, "error inserting tracking event");
                }
                removeMessages(EventLogger.FLUSH_TOKEN);
                sendMessageDelayed(obtainMessage(EventLogger.FLUSH_TOKEN), EventLogger.FLUSH_DELAY);
                break;

            case EventLogger.FLUSH_TOKEN:
                flushPlaybackTrackingEvents();
                break;

            case EventLogger.FINISH_TOKEN:
                removeMessages(EventLogger.FLUSH_TOKEN);
                flushPlaybackTrackingEvents();

                if (Log.isLoggable(EventLogger.TAG, Log.DEBUG)) Log.d(EventLogger.TAG, "Shutting down.");
                getLooper().quit();
                break;
        }
    }

    private boolean flushPlaybackTrackingEvents() {
        if (Log.isLoggable(EventLogger.TAG, Log.DEBUG)) Log.d(EventLogger.TAG, "flushPlaybackTrackingEvents");

        if (!IOUtils.isConnected(mContext)) {
            if (Log.isLoggable(EventLogger.TAG, Log.DEBUG)) Log.d(EventLogger.TAG, "not connected, skipping flush");
            return true;
        }

        List<Pair<Long, String>> events = mTrackingDbHelper.getUnpushedEvents(mTrackingApi);

        if (!events.isEmpty()) {
            final String[] submitted = mTrackingApi.pushToRemote(events);
            if (submitted.length > 0) {
                int deleted = mTrackingDbHelper.deleteEventsById(submitted);
                if (deleted != submitted.length) {
                    Log.w(EventLogger.TAG, "error deleting events (deleted=" + deleted + ")");
                } else {
                    if (Log.isLoggable(EventLogger.TAG, Log.DEBUG)) Log.d(EventLogger.TAG, "submitted " + deleted + " events");
                }
            }
        }

        return events.size() < EventLogger.BATCH_SIZE;
    }
}
