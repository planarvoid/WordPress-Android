package com.soundcloud.rx.eventbus;

import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Action;
import io.reactivex.functions.Consumer;
import io.reactivex.observers.ResourceObserver;
import io.reactivex.observers.TestObserver;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.subjects.Subject;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class TestEventBusV2 implements EventBusV2 {


    private final DefaultEventBus legacyEventBus = new DefaultEventBus(rx.schedulers.Schedulers.immediate());
    private final DefaultEventBusV2 eventBus = new DefaultEventBusV2(Schedulers.trampoline(), legacyEventBus);
    private final Map<Queue, Set<TestObserver>> observedQueues = new HashMap<>();
    private final Map<Queue, Set<Disposable>> subscriptions = new HashMap<>();

    private <T> List<T> internalEventsOn(Queue<T> queue) {
        final List<T> events = new LinkedList<>();
        if (observedQueues.containsKey(queue)) {
            for (TestObserver observer : observedQueues.get(queue)) {
                for (Object event : observer.values()) {
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

    public <T, S extends T> S firstEventOn(Queue<T> queue, Class<S> eventType) {
        final List<T> events = this.eventsOn(queue);
        assertFalse("Attempted to access first event on queue " + queue + ", but no events fired", events.isEmpty());
        final T event = events.get(0);
        assertTrue("Expect event of " + eventType, eventType.isAssignableFrom(event.getClass()));
        return (S) event;
    }

    public <T> T lastEventOn(Queue<T> queue) {
        final List<T> events = this.eventsOn(queue);
        assertFalse("Attempted to access last event on queue " + queue + ", but no events fired", events.isEmpty());
        return events.get(events.size() - 1);
    }

    public <T, S extends T> S lastEventOn(Queue<T> queue, Class<S> eventType) {
        final List<T> events = this.eventsOn(queue);
        assertFalse("Attempted to access last event on queue " + queue + ", but no events fired", events.isEmpty());
        final T event = events.get(events.size() - 1);
        assertTrue("Expect event of " + eventType, eventType.isAssignableFrom(event.getClass()));
        return (S) event;
    }

    public <T> void verifyNoEventsOn(Queue<T> queue) {
        final List<T> events = eventsOn(queue);
        assertTrue("Expected no events on queue " + queue + ", but found these events:\n" + events, events.isEmpty());
    }

    public <T> void verifyUnsubscribed(Queue<T> queue) {
        final Set<Disposable> seenSubscriptions = subscriptions.get(queue);
        assertFalse("Expected to be unsubscribed from queue " + queue + ", but was never subscribed",
                seenSubscriptions == null || seenSubscriptions.isEmpty());
        assertAllUnsubscribed(seenSubscriptions);
    }

    public void verifyUnsubscribed() {
        final Collection<Set<Disposable>> seenSubscriptions = subscriptions.values();
        assertFalse("Expected to be unsubscribed from all queues, but was never subscribed to any",
                seenSubscriptions.isEmpty());
        for (Collection<Disposable> subscriptions : seenSubscriptions) {
            assertAllUnsubscribed(subscriptions);
        }
    }

    private void assertAllUnsubscribed(Collection<Disposable> subscriptions) {
        for (Disposable subscription : subscriptions) {
            assertTrue("Expected to be unsubscribed from all queues, but found " + subscription,
                    subscription.isDisposed());
        }
    }

    @Override
    public <E> Disposable subscribe(Queue<E> queue, ResourceObserver<E> observer) {
        final Disposable disposable = eventBus.subscribe(queue, observer);
        Set<Disposable> queueSubs = subscriptions.get(queue);
        if (queueSubs == null) {
            queueSubs = new HashSet<>();
            subscriptions.put(queue, queueSubs);
        }
        queueSubs.add(disposable);
        return observer;
    }

    @Override
    public <E> Disposable subscribeImmediate(Queue<E> queue, ResourceObserver<E> observer) {
        return subscribe(queue, observer);
    }

    @Override
    public <T> void publish(Queue<T> queue, T event) {
        monitorQueue(queue);
        eventBus.publish(queue, event);
    }

    @Override
    public <E> Action publishAction0(final Queue<E> queue, final E event) {
        return new Action() {
            @Override
            public void run() {
                publish(queue, event);
            }
        };
    }

    @Override
    public <E, T> Consumer<T> publishAction1(final Queue<E> queue, final E event) {
        return new Consumer<T>() {
            @Override
            public void accept(T ignored) {
                publish(queue, event);
            }
        };
    }

    @Override
    public <T> Subject<T> queue(Queue<T> queue) {
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

    private void assertTrue(String message, boolean condition) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    private void assertFalse(String message, boolean condition) {
        if (condition) {
            throw new AssertionError(message);
        }
    }
}
