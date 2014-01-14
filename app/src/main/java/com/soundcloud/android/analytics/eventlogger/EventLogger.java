package com.soundcloud.android.analytics.eventlogger;

import static android.os.Process.THREAD_PRIORITY_LOWEST;

import com.soundcloud.android.events.PlaybackEvent;

import android.os.HandlerThread;
import android.os.Message;
import android.util.Log;

import javax.inject.Inject;

public class EventLogger {
    static final String TAG = EventLogger.class.getSimpleName();

    private final EventLoggerHandlerFactory mEventLoggerHandlerFactory;
    private EventLoggerHandler mHandler;

    @Inject
    public EventLogger(EventLoggerHandlerFactory eventLoggerHandlerFactory) {
        mEventLoggerHandlerFactory = eventLoggerHandlerFactory;
    }

    public void trackEvent(PlaybackEvent playbackEvent) {
        if (mHandler == null) {
            HandlerThread thread = new HandlerThread("EventLogger", THREAD_PRIORITY_LOWEST);
            thread.start();
            mHandler = mEventLoggerHandlerFactory.create(thread.getLooper());
        }

        if (Log.isLoggable(TAG, Log.DEBUG)) Log.d(TAG, "new tracking event: " + playbackEvent.toString());
        Message insert = mHandler.obtainMessage(EventLoggerHandler.INSERT_TOKEN, playbackEvent);
        mHandler.removeMessages(EventLoggerHandler.FINISH_TOKEN);
        mHandler.sendMessage(insert);
    }

    void stop() {
        if (mHandler != null) {
            mHandler.obtainMessage(EventLoggerHandler.FINISH_TOKEN).sendToTarget();
            mHandler = null;
        }
    }

    void flush() {
        if (mHandler != null) {
            mHandler.obtainMessage(EventLoggerHandler.FLUSH_TOKEN).sendToTarget();
        }
    }
}
