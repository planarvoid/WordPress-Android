package com.soundcloud.android.rx.eventbus;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import rx.Observer;
import rx.Subscription;
import rx.observers.TestObserver;
import rx.subjects.Subject;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class TestEventBus implements EventBus {

    private final EventBus eventBus = new DefaultEventBus();
    private final Map<Queue, Set<TestObserver>> observedQueues = new HashMap<>();
    private final Map<Queue, Set<Subscription>> subscriptions = new HashMap<>();

    private <T> List<T> internalEventsOn(Queue<T> queue) {
        final List<T> events = new LinkedList<>();
        if (observedQueues.containsKey(queue)) {
            for (TestObserver observer : observedQueues.get(queue)) {
                for (Object event : observer.getOnNextEvents()) {
                    events.add((T) event);
                }
            }
        }
        return events;
    }

    public <T> List<T> eventsOn(Queue<T> queue) {
        return internalEventsOn(queue);
    }

    public <T> T firstEventOn(Queue<T> queue) {
        final List<T> events = this.eventsOn(queue);
        assertFalse("Attempted to access first event on queue " + queue + ", but no events fired", events.isEmpty());
        return events.get(0);
    }

    public <T> T lastEventOn(Queue<T> queue) {
        final List<T> events = this.eventsOn(queue);
        assertFalse("Attempted to access last event on queue " + queue + ", but no events fired", events.isEmpty());
        return events.get(events.size() - 1);
    }

    public <T> void verifyNoEventsOn(Queue<T> queue) {
        final List<T> events = eventsOn(queue);
        assertTrue("Expected no events on queue " + queue + ", but found these events:\n" + events, events.isEmpty());
    }

    public <T> void verifyUnsubscribed(Queue<T> queue) {
        final Set<Subscription> seenSubscriptions = subscriptions.get(queue);
        assertFalse("Expected to be unsubscribed from queue " + queue + ", but was never subscribed",
                seenSubscriptions == null || seenSubscriptions.isEmpty());
        assertAllUnsubscribed(seenSubscriptions);
    }

    public void verifyUnsubscribed() {
        final Collection<Set<Subscription>> seenSubscriptions = subscriptions.values();
        assertFalse("Expected to be unsubscribed from all queues, but was never subscribed to any",
                seenSubscriptions.isEmpty());
        for (Collection<Subscription> subscriptions : seenSubscriptions) {
            assertAllUnsubscribed(subscriptions);
        }
    }

    private void assertAllUnsubscribed(Collection<Subscription> subscriptions) {
        for (Subscription subscription : subscriptions) {
            assertTrue("Expected to be unsubscribed from all queues, but found " + subscription,
                    subscription.isUnsubscribed());
        }
    }

    @Override
    public <T> Subscription subscribe(Queue<T> queue, Observer<T> observer) {
        final Subscription subscription = eventBus.subscribe(queue, observer);
        Set<Subscription> queueSubs = subscriptions.get(queue);
        if (queueSubs == null) {
            queueSubs = new HashSet<>();
            subscriptions.put(queue, queueSubs);
        }
        queueSubs.add(subscription);
        return subscription;
    }

    @Override
    public <T> Subscription subscribeImmediate(Queue<T> queue, Observer<T> observer) {
        return subscribe(queue, observer);
    }

    @Override
    public <T> void publish(Queue<T> queue, T event) {
        monitorQueue(queue);
        eventBus.publish(queue, event);
    }

    @Override
    public <T> Subject<T, T> queue(Queue<T> queue) {
        monitorQueue(queue);
        return eventBus.queue(queue);
    }

    private <T> void monitorQueue(Queue<T> queue) {
        if (!observedQueues.containsKey(queue)) {
            final TestObserver<T> testObserver = new TestObserver<>();
            eventBus.subscribe(queue, testObserver);
            Set<TestObserver> queueObservers = observedQueues.get(queue);
            if (queueObservers == null) {
                queueObservers = new HashSet<>();
                observedQueues.put(queue, queueObservers);
            }
            queueObservers.add(testObserver);
        }
    }
}
