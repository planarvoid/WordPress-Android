package com.soundcloud.rx.eventbus;

import io.reactivex.Observer;
import io.reactivex.functions.Action;
import io.reactivex.functions.Consumer;

public interface EventBusV2 {

    /**
     * Subscribes the given observer to the given queue. On which thread the observer
     * receives events is implementation defined (see {@link DefaultEventBus}.
     */
    <E> Observer<E> subscribe(Queue<E> queue, io.reactivex.Observer<E> observer);

    /**
     * Subscribes the given observer to the given queue. The observer will receive
     * events on the same thread on which the event was published.
     */
    <E> Observer<E> subscribeImmediate(Queue<E> queue, io.reactivex.Observer<E> observer);

    /**
     * Post the given event to this queue.
     */
    <E> void publish(Queue<E> queue, E event);

    /**
     * Constructs an action that can be used in Rx call chains to publish events.
     */
    <E> Action publishAction0(Queue<E> queue, E event);

    /**
     * Constructs an action that can be used in Rx call chains to publish events.
     */
    <E, T> Consumer<T> publishAction1(Queue<E> queue, E event);

    /**
     * @return the Rx Subject associated to the given queue
     */
    <E> io.reactivex.subjects.Subject<E> queue(Queue<E> queue);
}
