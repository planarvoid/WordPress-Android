package com.soundcloud.android.analytics.eventlogger;

import static android.os.Process.THREAD_PRIORITY_LOWEST;

import com.soundcloud.android.events.EventBus;
import com.soundcloud.android.events.PlaybackEvent;
import com.soundcloud.android.events.PlayerLifeCycleEvent;
import com.soundcloud.android.rx.observers.DefaultObserver;
import rx.Subscription;
import rx.subscriptions.Subscriptions;

import android.os.HandlerThread;
import android.os.Message;
import android.util.Log;

import javax.inject.Inject;

public class EventLogger {
    static final String TAG = EventLogger.class.getSimpleName();

    private final EventLoggerHandlerFactory mEventLoggerHandlerFactory;
    private Subscription mShutdownSubscription = Subscriptions.empty();
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

            mShutdownSubscription = EventBus.PLAYER_LIFECYCLE.subscribe(new PlaybackServiceDestroyedObserver());
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
        mShutdownSubscription.unsubscribe();
    }

    void flush() {
        if (mHandler != null) {
            mHandler.obtainMessage(EventLoggerHandler.FLUSH_TOKEN).sendToTarget();
        }
    }

    private class PlaybackServiceDestroyedObserver extends DefaultObserver<PlayerLifeCycleEvent> {
        @Override
        public void onNext(PlayerLifeCycleEvent event) {
            if (event.getKind() == PlayerLifeCycleEvent.STATE_DESTROYED) {
                stop();
            }
        }
    }
}
