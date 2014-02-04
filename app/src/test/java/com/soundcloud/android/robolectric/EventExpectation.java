package com.soundcloud.android.robolectric;

import static com.soundcloud.android.Expect.expect;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.refEq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.events.EventBus2;
import org.mockito.ArgumentCaptor;
import rx.Observer;
import rx.subscriptions.Subscriptions;
import rx.util.functions.Action1;

public class EventExpectation {

    private EventBus2 eventBus;
    private EventBus2.QueueDescriptor queue;
    private ArgumentCaptor captor;

    public static EventExpectation on(EventBus2 eventBus) {
        return new EventExpectation(eventBus);
    }

    private EventExpectation(EventBus2 eventBus) {
        this.eventBus = eventBus;
        when(eventBus.subscribe(any(EventBus2.QueueDescriptor.class), any(Action1.class))).thenReturn(Subscriptions.empty());
        when(eventBus.subscribe(any(EventBus2.QueueDescriptor.class), any(Observer.class))).thenReturn(Subscriptions.empty());
    }

    public EventExpectation withQueue(EventBus2.QueueDescriptor queue) {
        this.queue = queue;
        return this;
    }

    public EventExpectation verifyActionSubscribed() {
        ArgumentCaptor<Action1> eventObserver = ArgumentCaptor.forClass(Action1.class);
        verify(eventBus).subscribe(refEq(queue), eventObserver.capture());
        this.captor = eventObserver;
        return this;
    }

    public EventExpectation verifyObserverSubscribed() {
        ArgumentCaptor<Observer> eventObserver = ArgumentCaptor.forClass(Observer.class);
        verify(eventBus).subscribe(refEq(queue), eventObserver.capture());
        this.captor = eventObserver;
        return this;
    }

    public void publish(Object event) {
        expect(captor).not.toBeNull();
        Object observer = captor.getValue();
        expect(observer).not.toBeNull();
        if (observer instanceof Action1) {
            ((Action1) observer).call(event);
        } else if (observer instanceof Observer) {
            ((Observer) observer).onNext(event);
        }
    }

}
