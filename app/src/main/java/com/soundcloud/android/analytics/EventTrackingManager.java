package com.soundcloud.android.analytics;

import static android.os.Process.THREAD_PRIORITY_LOWEST;

import com.soundcloud.android.utils.Log;

import android.os.HandlerThread;
import android.os.Message;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.concurrent.TimeUnit;

/**
 * Used by an {@link com.soundcloud.android.analytics.AnalyticsProvider} to schedule tracking of events.
 * Tracked events are queued up in the database and can be flushed in batches at an appropriate time.
 */
@Singleton
public class EventTrackingManager {
    static final String TAG = EventTrackingManager.class.getSimpleName();

    private static final long FINISH_DELAY_MILLIS = TimeUnit.SECONDS.toMillis(30);

    private final TrackingHandlerFactory handlerFactory;
    private TrackingHandler handler;

    @Inject
    public EventTrackingManager(TrackingHandlerFactory handlerFactory) {
        this.handlerFactory = handlerFactory;
    }

    public void trackEvent(TrackingRecord event) {
        Log.d(TAG, "New tracking event: " + event.toString());
        createHandlerIfNecessary();

        // cancel any pending finish requests
        handler.removeMessages(TrackingHandler.FINISH_TOKEN);

        final Message insert = handler.obtainMessage(TrackingHandler.INSERT_TOKEN, event);
        handler.sendMessage(insert);

        // schedule a finish (next incoming event will cancel this again)
        final Message finish = handler.obtainMessage(TrackingHandler.FINISH_TOKEN);
        handler.sendMessageDelayed(finish, FINISH_DELAY_MILLIS);
    }

    public void flush(String backend) {
        Log.d(TAG, "Requesting FLUSH for " + backend);
        createHandlerIfNecessary();
        handler.obtainMessage(TrackingHandler.FLUSH_TOKEN, backend).sendToTarget();
    }

    private void createHandlerIfNecessary() {
        if (isHandlerDead()) {
            Log.d(TAG, "Handler was dead, recreating");
            HandlerThread thread = new HandlerThread(TAG, THREAD_PRIORITY_LOWEST);
            thread.start();
            handler = handlerFactory.create(thread.getLooper());
        }
    }

    private boolean isHandlerDead() {
        return handler == null || handler.getLooper().getThread().getState() == Thread.State.TERMINATED;
    }

}
