package com.soundcloud.android.robolectric;

import static com.soundcloud.android.Expect.expect;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.refEq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.events.EventBus;
import org.mockito.ArgumentCaptor;
import org.mockito.verification.VerificationMode;
import rx.Observer;
import rx.Subscription;
import rx.subjects.Subject;
import rx.util.functions.Action1;

public class EventMonitor {

    private EventBus eventBus;
    private ArgumentCaptor captor;
    private Subscription subscription;
    private Subject monitoredQueue;

    public static EventMonitor on(EventBus eventBus) {
        return new EventMonitor(eventBus);
    }

    private EventMonitor(EventBus eventBus) {
        this.eventBus = eventBus;
        withSubscription(mock(Subscription.class));
    }

    public EventMonitor withSubscription(Subscription subscription) {
        this.subscription = subscription;
        when(eventBus.subscribe(any(EventBus.Queue.class), any(Observer.class))).thenReturn(subscription);
        return this;
    }

    public EventMonitor monitorQueue(EventBus.Queue queue) {
        this.monitoredQueue = mock(Subject.class);
        when(eventBus.queue(queue)).thenReturn(monitoredQueue);
        return this;
    }

    public EventMonitor verifySubscribedTo(EventBus.Queue queue) {
        verifySubscribedTo(queue, times(1));
        return this;
    }

    public EventMonitor verifySubscribedTo(EventBus.Queue queue, VerificationMode verificationMode) {
        ArgumentCaptor<Observer> eventObserver = ArgumentCaptor.forClass(Observer.class);
        verify(eventBus, verificationMode).subscribe(refEq(queue), eventObserver.capture());
        this.captor = eventObserver;
        return this;
    }

    public EventMonitor verifyNotSubscribedTo(EventBus.Queue queue) {
        verify(eventBus, never()).subscribe(refEq(queue), any(Observer.class));
        return this;
    }

    public EventMonitor verifyUnsubscribed() {
        verify(subscription, atLeastOnce()).unsubscribe();
        return this;
    }

    public <EventType> EventType verifyEventOn(EventBus.Queue<EventType> queue) {
        ArgumentCaptor<EventType> eventObserver = ArgumentCaptor.forClass(queue.eventType);
        if (monitoredQueue != null) {
            verify(monitoredQueue).onNext(eventObserver.capture());
        } else {
            verify(eventBus).publish(refEq(queue), eventObserver.capture());
        }
        return eventObserver.getValue();
    }

    public <EventType> EventType verifyLastEventOn(EventBus.Queue<EventType> queue) {
        ArgumentCaptor<EventType> eventObserver = ArgumentCaptor.forClass(queue.eventType);

        if (monitoredQueue != null) {
            verify(monitoredQueue, atLeastOnce()).onNext(eventObserver.capture());
        } else {
            verify(eventBus, atLeastOnce()).publish(refEq(queue), eventObserver.capture());
        }
        return eventObserver.getValue();
    }

    public <EventType> EventMonitor verifyNoEventsOn(EventBus.Queue<EventType> queue) {
        if (monitoredQueue != null) {
            verify(monitoredQueue, never()).onNext(any(queue.eventType));
        } else {
            verify(eventBus, never()).publish(refEq(queue), any(queue.eventType));
        }

        return this;
    }

    public <EventType> void publish(EventBus.Queue<EventType> queue, EventType event) {
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
