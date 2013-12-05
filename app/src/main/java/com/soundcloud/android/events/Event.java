package com.soundcloud.android.events;

import com.soundcloud.android.model.User;
import rx.Observer;
import rx.Subscription;
import rx.android.concurrency.AndroidSchedulers;
import rx.subjects.PublishSubject;

/**
 * A simple event bus based on RxJava. Add new events as enum constants. Use this event bus for non-critical events
 * that might be fired while the client component is active. For instance, an Activity can subscribe in onCreate and
 * unsubscribe in onDestroy. It's guaranteed that during the time of subscription events will be emitted, but missed
 * events during unsubscription will not be re-emitted.
 * <p/>
 * Event queues are not type safe, but an error will be raised immediately as soon as you try to emit event data that
 * does not match the type given when declaring the event.
 */
public enum Event {

    CURRENT_USER_UPDATED(User.class),
    SCREEN_ENTERED(String.class),
    PLAYBACK(PlaybackEventData.class),

    /**
     * Signals the playback service is destoryed. Used to flush events and stop handler in {@link com.soundcloud.android.tracking.eventlogger.EventLogger}
     */
    PLAYBACK_SERVICE_DESTROYED(Void.class),

    // I'd like to keep this to make unit testing simpler and not bound to our app specific events.
    TEST_EVENT(String.class),
    TEST_VOID_EVENT(Void.class);

    private final Class<?> eventDataType;
    private final PublishSubject eventQueue = PublishSubject.create();

    Event(Class<?> eventDataType) {
        this.eventDataType = eventDataType;
    }

    /**
     * Subscribes to this event and receives notifications on the main UI thread. For callers that wish to receive
     * notfications on a background, use {@link #subscribeBackground(rx.Observer)} instead.
     */
    @SuppressWarnings("unchecked")
    public <T> Subscription subscribe(Observer<T> observer) {
        return eventQueue.observeOn(AndroidSchedulers.mainThread()).subscribe(observer);
    }

    /**
     * Subscribes to this event and receives notifications on the current thread. This variant should be preferred
     * by components that want to consume events on a background thread.
     */
    @SuppressWarnings("unchecked")
    public <T> Subscription subscribeBackground(Observer<T> observer) {
        return eventQueue.subscribe(observer);
    }

    @SuppressWarnings("unchecked")
    public void publish(Object eventData) {
        if (!eventData.getClass().isAssignableFrom(eventDataType)) {
            throw new IllegalArgumentException("Cannot publish event data of type " +
                    eventData.getClass().getCanonicalName() +
                    "; expected " + eventDataType.getCanonicalName());
        }
        eventQueue.onNext(eventData);
    }

    @SuppressWarnings("unchecked")
    public void publish() {
        if (eventDataType != Void.class) {
            throw new IllegalArgumentException("Event Data required; expected " + eventDataType.getCanonicalName());
        }
        eventQueue.onNext(null);
    }

}
