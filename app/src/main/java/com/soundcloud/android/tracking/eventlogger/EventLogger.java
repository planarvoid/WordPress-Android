package com.soundcloud.android.tracking.eventlogger;

import static android.os.Process.THREAD_PRIORITY_LOWEST;

import com.soundcloud.android.events.Event;
import com.soundcloud.android.events.PlaybackEventData;
import com.soundcloud.android.rx.observers.DefaultObserver;
import rx.Subscription;
import rx.subscriptions.Subscriptions;

import android.os.HandlerThread;
import android.os.Message;
import android.util.Log;

import javax.inject.Inject;

public class EventLogger {
    static final String TAG = EventLogger.class.getSimpleName();

    static final int INSERT_TOKEN = 0;
    static final int FLUSH_TOKEN = 1;
    static final int FINISH_TOKEN = 0xDEADBEEF;

    // service stop delay is 60s, this is bigger to avoid simultaneous flushes
    public static final int FLUSH_DELAY   = 90 * 1000;
    public static final int BATCH_SIZE    = 10;

    private final EventLoggerHandlerFactory mEventLoggerHandlerFactory;
    private final Object lock = new Object();
    private Subscription mShutdownSubscrioption = Subscriptions.empty();

    private EventLoggerHandler mHandler;

    @Inject
    public EventLogger(EventLoggerHandlerFactory eventLoggerHandlerFactory) {
        mEventLoggerHandlerFactory = eventLoggerHandlerFactory;
    }

    public void trackEvent(PlaybackEventData playbackEventData) {
        synchronized (lock) {
            if (mHandler == null) {
                HandlerThread thread = new HandlerThread("PlayEvent-tracking", THREAD_PRIORITY_LOWEST);
                thread.start();
                mHandler = mEventLoggerHandlerFactory.create(thread.getLooper());

                mShutdownSubscrioption = Event.PLAYBACK_SERVICE_DESTROYED.subscribe(new PlaybackServiceDestroyedObserver());
            }

            if (Log.isLoggable(TAG, Log.DEBUG)) Log.d(TAG, "new tracking event: " + playbackEventData.toString());
            Message insert = mHandler.obtainMessage(INSERT_TOKEN, playbackEventData);
            mHandler.removeMessages(FINISH_TOKEN);
            mHandler.sendMessage(insert);
        }
    }

    void stop() {
        if (mHandler != null) {
            mHandler.obtainMessage(FINISH_TOKEN).sendToTarget();
        }
        mShutdownSubscrioption.unsubscribe();
    }

    private class PlaybackServiceDestroyedObserver extends DefaultObserver<Integer> {
        @Override
        public void onNext(Integer args) {
            synchronized (lock) {
                stop();
            }
        }
    }
}
