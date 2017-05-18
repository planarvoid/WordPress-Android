package com.soundcloud.rx.eventbus;

import rx.Observer;
import rx.Scheduler;
import rx.Subscription;
import rx.functions.Action0;
import rx.functions.Action1;
import rx.subjects.Subject;

import java.util.HashMap;
import java.util.Map;

@Deprecated // Prefer DefaultEventBusV2 for RxJava2 implementation
public class DefaultEventBus implements EventBus {

    private final Map<Integer, Subject<?, ?>> queues = new HashMap<>();

    private final Scheduler defaultScheduler;

    /**
     * Constructs a new EventBus instance managing a set of {@link Queue}s.
     *
     * @param defaultScheduler The scheduler through which to dispatch events.
     *                         This will be used by {@link #subscribe(Queue, Observer)};
     *                         to dispatch events on the publishing thread, use
     *                         {@link #subscribeImmediate(Queue, Observer)}.
     */
    public DefaultEventBus(Scheduler defaultScheduler) {
        this.defaultScheduler = defaultScheduler;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <E> Subject<E, E> queue(Queue<E> queue) {
        Subject<E, E> subject = (Subject<E, E>) queues.get(queue.id);
        if (subject == null) {
            if (queue.replayLast) {
                subject = EventSubject.replaying(queue.defaultEvent, queue.onError);
            } else {
                subject = EventSubject.create(queue.onError);
            }
            queues.put(queue.id, subject);
        }
        return subject;
    }

    @Override
    public <E> Subscription subscribe(Queue<E> queue, Observer<E> observer) {
        return this.queue(queue).observeOn(defaultScheduler).subscribe(observer);
    }

    @Override
    public <E> Subscription subscribeImmediate(Queue<E> queue, Observer<E> observer) {
        return this.queue(queue).subscribe(observer);
    }

    @Override
    public <E> void publish(Queue<E> queue, E event) {
        this.queue(queue).onNext(event);
    }

    @Override
    public <E> Action0 publishAction0(final Queue<E> queue, final E event) {
        return new Action0() {
            @Override
            public void call() {
                publish(queue, event);
            }
        };
    }

    @Override
    public <E, T> Action1<T> publishAction1(final Queue<E> queue, final E event) {
        return new Action1<T>() {
            @Override
            public void call(T t) {
                publish(queue, event);
            }
        };
    }
}
