package com.soundcloud.rx.eventbus;

import hu.akarnokd.rxjava.interop.RxJavaInterop;
import io.reactivex.Observer;
import io.reactivex.Scheduler;
import io.reactivex.functions.Action;
import io.reactivex.functions.Consumer;
import io.reactivex.subjects.Subject;

public class DefaultEventBusV2 implements EventBusV2 {

    private final Scheduler defaultScheduler;
    private final DefaultEventBus eventBus;

    public DefaultEventBusV2(Scheduler defaultScheduler) {
        this.defaultScheduler = defaultScheduler;
        this.eventBus = new DefaultEventBus(null);
    }

    @Override
    public <E> Observer<E> subscribe(Queue<E> queue, Observer<E> observer) {
        return queue(queue).subscribeOn(defaultScheduler).subscribeWith(observer);
    }

    @Override
    public <E> Observer<E> subscribeImmediate(Queue<E> queue, Observer<E> observer) {
        return queue(queue).subscribeWith(observer);
    }

    @Override
    public <E> void publish(Queue<E> queue, E event) {
        eventBus.publish(queue, event);
    }

    @Override
    public <E> Action publishAction0(final Queue<E> queue, final E event) {
        return new Action() {
            @Override
            public void run() throws Exception {
                eventBus.publishAction0(queue, event).call();
            }
        };
    }

    @Override
    public <E, T> Consumer<T> publishAction1(final Queue<E> queue, final E event) {
        return new Consumer<T>() {
            @Override
            public void accept(T t) throws Exception {
                eventBus.publishAction1(queue, event).call(t);
            }
        };
    }

    @Override
    public <E> Subject<E> queue(Queue<E> queue) {
        return RxJavaInterop.toV2Subject(eventBus.queue(queue));
    }
}
