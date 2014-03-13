package com.soundcloud.android.analytics.eventlogger;

import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.events.PlaybackEvent;
import com.soundcloud.android.utils.IOUtils;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.util.Pair;

import java.io.UnsupportedEncodingException;
import java.util.List;

public class EventLoggerHandler extends Handler {

    static final int BATCH_SIZE    = 10;

    static final int INSERT_TOKEN = 0;
    static final int FLUSH_TOKEN = 1;
    static final int FINISH_TOKEN = 0xDEADBEEF;

    private final Context mContext;
    private final EventLoggerStorage mStorage;
    private final EventLoggerApi mApi;

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
            case INSERT_TOKEN:
                try {
                    if (mStorage.insertEvent((PlaybackEvent) msg.obj) < 0) {
                        Log.w(EventLogger.TAG, "error inserting tracking event");
                    }
                } catch (UnsupportedEncodingException e) {
                    Log.w(EventLogger.TAG, "error inserting tracking event", e);
                }
                break;

            case FLUSH_TOKEN:
                flushPlaybackTrackingEvents();
                break;

            case FINISH_TOKEN:
                removeMessages(FLUSH_TOKEN);
                flushPlaybackTrackingEvents();

                if (Log.isLoggable(EventLogger.TAG, Log.DEBUG)) Log.d(EventLogger.TAG, "Shutting down.");
                getLooper().quit();
                break;
        }
    }

    private void flushPlaybackTrackingEvents() {
        if (Log.isLoggable(EventLogger.TAG, Log.DEBUG)) Log.d(EventLogger.TAG, "flushPlaybackTrackingEvents");

        if (!IOUtils.isConnected(mContext)) {
            if (Log.isLoggable(EventLogger.TAG, Log.DEBUG)) Log.d(EventLogger.TAG, "not connected, skipping flush");
            return;
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
    }
}
