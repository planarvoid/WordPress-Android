package com.soundcloud.android.analytics.eventlogger;

import static android.os.Process.THREAD_PRIORITY_LOWEST;

import com.soundcloud.android.utils.Log;

import android.os.HandlerThread;
import android.os.Message;

import javax.inject.Inject;

public class EventLogger {
    static final String TAG = EventLogger.class.getSimpleName();

    private final EventLoggerHandlerFactory handlerFactory;
    private EventLoggerHandler handler;

    @Inject
    public EventLogger(EventLoggerHandlerFactory handlerFactory) {
        this.handlerFactory = handlerFactory;
    }

    public void trackEvent(EventLoggerEvent event) {
        if (handler == null) {
            HandlerThread thread = new HandlerThread("EventLogger", THREAD_PRIORITY_LOWEST);
            thread.start();
            handler = handlerFactory.create(thread.getLooper());
        }

        Log.d(TAG, "new tracking event: " + event.toString());
        Message insert = handler.obtainMessage(EventLoggerHandler.INSERT_TOKEN, event);
        handler.removeMessages(EventLoggerHandler.FINISH_TOKEN);
        handler.sendMessage(insert);
    }

    void stop() {
        if (handler != null) {
            handler.obtainMessage(EventLoggerHandler.FINISH_TOKEN).sendToTarget();
            handler = null;
        }
    }

    void flush() {
        if (handler != null) {
            handler.obtainMessage(EventLoggerHandler.FLUSH_TOKEN).sendToTarget();
        }
    }
}
