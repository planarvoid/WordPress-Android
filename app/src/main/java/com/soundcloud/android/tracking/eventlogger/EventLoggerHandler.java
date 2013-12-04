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
    private EventLoggerStorage mStorage;
    private EventLoggerApi mApi;

    public EventLoggerHandler(Looper looper, Context context, EventLoggerStorage storage, EventLoggerApi eventLoggerApi) {
        super(looper);
        mContext = context;
        mStorage = storage;
        mApi = eventLoggerApi;
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
                long id = mStorage.insertEvent(params);
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

        List<Pair<Long, String>> events = mStorage.getUnpushedEvents(mApi);

        if (!events.isEmpty()) {
            final String[] submitted = mApi.pushToRemote(events);
            if (submitted.length > 0) {
                int deleted = mStorage.deleteEventsById(submitted);
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
