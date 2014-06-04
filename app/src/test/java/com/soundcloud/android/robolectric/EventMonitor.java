package com.soundcloud.android.robolectric;

import static com.soundcloud.android.Expect.expect;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.refEq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.collect.Iterables;
import com.soundcloud.android.events.EventBus;
import com.soundcloud.android.rx.EventSubject;
import org.hamcrest.Matchers;
import org.mockito.ArgumentCaptor;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.mockito.verification.VerificationMode;
import rx.Observer;
import rx.Subscription;
import rx.observers.TestObserver;
import rx.subjects.Subject;
import rx.util.functions.Action1;

import java.util.HashMap;
import java.util.Map;

// TODO: Fix this entire class
public class EventMonitor {

    private EventBus eventBus;
    private ArgumentCaptor captor;
    private Subscription subscription;

    private Map<EventBus.Queue, MonitoredQueue> monitoredQueues = new HashMap<EventBus.Queue, MonitoredQueue>();

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
        final Subject subject = EventSubject.create();
        final TestObserver testObserver = new TestObserver();
        subject.subscribe(testObserver);
        monitoredQueues.put(queue, new MonitoredQueue(subject, testObserver));
        when(eventBus.queue(queue)).thenReturn(subject);
        when(eventBus.subscribe(eq(queue), any(Observer.class))).thenAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                Observer observer = (Observer) invocation.getArguments()[1];
                subject.subscribe(observer);
                return null;
            }
        });
        when(eventBus.subscribeImmediate(eq(queue), any(Observer.class))).thenAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                Observer observer = (Observer) invocation.getArguments()[1];
                subject.subscribe(observer);
                return null;
            }
        });
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
        MonitoredQueue monitoredQueue = monitoredQueues.get(queue);
        if (monitoredQueue != null) {
            TestObserver testObserver = monitoredQueue.testObserver;
            assertThat(testObserver.getOnNextEvents().size(), Matchers.greaterThan(0));
            return (EventType) testObserver.getOnNextEvents().get(0);
        } else {
            ArgumentCaptor<EventType> eventObserver = ArgumentCaptor.forClass(queue.eventType);
            verify(eventBus).publish(refEq(queue), eventObserver.capture());
            return eventObserver.getValue();
        }
    }

    public <EventType> EventType verifyLastEventOn(EventBus.Queue<EventType> queue) {
        MonitoredQueue monitoredQueue = monitoredQueues.get(queue);
        if (monitoredQueue != null) {
            TestObserver testObserver = monitoredQueue.testObserver;
            assertThat(testObserver.getOnNextEvents().size(), Matchers.greaterThan(0));
            return (EventType) Iterables.getLast(testObserver.getOnNextEvents());
        } else {
            ArgumentCaptor<EventType> eventObserver = ArgumentCaptor.forClass(queue.eventType);
            verify(eventBus, atLeastOnce()).publish(refEq(queue), eventObserver.capture());
            return Iterables.getLast(eventObserver.getAllValues());
        }
    }

    public <EventType> EventMonitor verifyNoEventsOn(EventBus.Queue<EventType> queue) {
        MonitoredQueue monitoredQueue = monitoredQueues.get(queue);
        if (monitoredQueue != null) {
            assertThat(monitoredQueue.testObserver.getOnNextEvents().size(), Matchers.is(0));
        } else {
            verify(eventBus, never()).publish(refEq(queue), any(queue.eventType));
        }

        return this;
    }

    public <EventType> void publish(EventBus.Queue<EventType> queue, EventType event) {
        MonitoredQueue monitoredQueue = monitoredQueues.get(queue);
        if (monitoredQueue != null) {
            monitoredQueue.subject.onNext(event);
        } else {
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

    private static class MonitoredQueue {
        MonitoredQueue(Subject subject, TestObserver testObserver) {
            this.subject = subject;
            this.testObserver = testObserver;
        }

        Subject subject;
        TestObserver testObserver;
    }
}
