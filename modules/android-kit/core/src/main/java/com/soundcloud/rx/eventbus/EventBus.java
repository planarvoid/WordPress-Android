package com.soundcloud.rx.eventbus;

import rx.Observer;
import rx.Subscription;
import rx.functions.Action0;
import rx.functions.Action1;
import rx.subjects.Subject;

@Deprecated // Prefer EventBusV2 for RxJava2 implementation
public interface EventBus {

    /**
     * Subscribes the given observer to the given queue. On which thread the observer
     * receives events is implementation defined (see {@link DefaultEventBus}.
     */
    <E> Subscription subscribe(Queue<E> queue, Observer<E> observer);

    /**
     * Subscribes the given observer to the given queue. The observer will receive
     * events on the same thread on which the event was published.
     */
    <E> Subscription subscribeImmediate(Queue<E> queue, Observer<E> observer);

    /**
     * Post the given event to this queue.
     */
    <E> void publish(Queue<E> queue, E event);

    /**
     * Constructs an action that can be used in Rx call chains to publish events.
     */
    <E> Action0 publishAction0(Queue<E> queue, E event);

    /**
     * Constructs an action that can be used in Rx call chains to publish events.
     */
    <E, T> Action1<T> publishAction1(Queue<E> queue, E event);

    /**
     * @return the Rx Subject associated to the given queue
     */
    <E> Subject<E, E> queue(Queue<E> queue);
}
