package com.soundcloud.android.events;

import com.soundcloud.android.utils.Log;
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
public enum EventBus {

    ACTIVITY_LIFECYCLE(ActivityLifeCycleEvent.class),
    PLAYER_LIFECYCLE(PlayerLifeCycleEvent.class),
    CURRENT_USER_CHANGED(CurrentUserChangedEvent.class),
    SCREEN_ENTERED(String.class),
    PLAYBACK(PlaybackEvent.class),
    UI(UIEvent.class),
    ONBOARDING(OnboardingEvent.class);

    public final PublishSubject QUEUE = PublishSubject.create();

    private final Class<?> eventDataType;

    EventBus(Class<?> eventDataType) {
        this.eventDataType = eventDataType;
    }

    /**
     * Subscribes to this event and receives notifications on the main UI thread. For callers that wish to receive
     * notfications on the current thread, use {@link #subscribeHere(rx.Observer)} instead.
     */
    @SuppressWarnings("unchecked")
    public <T> Subscription subscribe(Observer<T> observer) {
        Log.d(this, "subscribing to queue " + name());
        return QUEUE.observeOn(AndroidSchedulers.mainThread()).subscribe(observer);
    }

    /**
     * Subscribes to this event and receives notifications on the current thread. This variant should be preferred
     * by components that want to consume events on a background thread.
     */
    @SuppressWarnings("unchecked")
    public <T> Subscription subscribeHere(Observer<T> observer) {
        return QUEUE.subscribe(observer);
    }

    @SuppressWarnings("unchecked")
    public void publish(Object eventData) {
        Log.d(this, "publishing event: queue = " + name() + "; data = " + eventData);
        if (!eventData.getClass().isAssignableFrom(eventDataType)) {
            throw new IllegalArgumentException("Cannot publish event data of type " +
                    eventData.getClass().getCanonicalName() +
                    "; expected " + eventDataType.getCanonicalName());
        }
        QUEUE.onNext(eventData);
    }

    @SuppressWarnings("unchecked")
    public void publish() {
        Log.d(this, "publishing event: queue = " + name() + "; <no data>");
        if (eventDataType != Void.class) {
            throw new IllegalArgumentException("Event Data required; expected " + eventDataType.getCanonicalName());
        }
        QUEUE.onNext(null);
    }

}
