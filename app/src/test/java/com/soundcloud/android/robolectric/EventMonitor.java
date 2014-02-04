package com.soundcloud.android.robolectric;

import static com.soundcloud.android.Expect.expect;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.refEq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.events.EventBus2;
import org.mockito.ArgumentCaptor;
import rx.Observer;
import rx.Subscription;
import rx.util.functions.Action1;

public class EventMonitor {

    private EventBus2 eventBus;
    private ArgumentCaptor captor;
    private Subscription subscription;

    public static EventMonitor on(EventBus2 eventBus) {
        return new EventMonitor(eventBus);
    }

    private EventMonitor(EventBus2 eventBus) {
        this.eventBus = eventBus;
        withSubscription(mock(Subscription.class));
    }

    public EventMonitor withSubscription(Subscription subscription) {
        this.subscription = subscription;
        when(eventBus.subscribe(any(EventBus2.QueueDescriptor.class), any(Observer.class))).thenReturn(subscription);
        return this;
    }

    public EventMonitor verifySubscribedTo(EventBus2.QueueDescriptor queue) {
        ArgumentCaptor<Observer> eventObserver = ArgumentCaptor.forClass(Observer.class);
        verify(eventBus).subscribe(refEq(queue), eventObserver.capture());
        this.captor = eventObserver;
        return this;
    }

    public EventMonitor verifyNotSubscribedTo(EventBus2.QueueDescriptor queue) {
        verify(eventBus, never()).subscribe(refEq(queue), any(Observer.class));
        return this;
    }

    public EventMonitor verifyUnsubscribed() {
        verify(subscription, atLeastOnce()).unsubscribe();
        return this;
    }

    public <EventType> EventType verifyEventOn(EventBus2.QueueDescriptor<EventType> queue) {
        ArgumentCaptor<EventType> eventObserver = ArgumentCaptor.forClass(queue.eventType);
        verify(eventBus).publish(refEq(queue), eventObserver.capture());
        return eventObserver.getValue();
    }

    public <EventType> EventMonitor verifyNoEventsOn(EventBus2.QueueDescriptor<EventType> queue) {
        verify(eventBus, never()).publish(refEq(queue), any(queue.eventType));
        return this;
    }

    public <EventType> void publish(EventBus2.QueueDescriptor<EventType> queue, EventType event) {
        verifySubscribedTo(queue);

        Object observer = captor.getValue();
        expect(observer).not.toBeNull();

        if (observer instanceof Action1) {
            ((Action1) observer).call(event);
        } else if (observer instanceof Observer) {
            ((Observer) observer).onNext(event);
        }
    }

}
