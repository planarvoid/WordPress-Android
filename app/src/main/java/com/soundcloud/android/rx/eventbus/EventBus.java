package com.soundcloud.android.rx.eventbus;

import rx.Observer;
import rx.Subscription;
import rx.functions.Action1;
import rx.subjects.Subject;

public interface EventBus {

    <T> Subscription subscribe(Queue<T> queue, Observer<T> observer);

    <T> Subscription subscribeImmediate(Queue<T> queue, Observer<T> observer);

    <T> void publish(Queue<T> queue, T event);

    <T, E> Action1<E> publishAction(Queue<T> queue, T event);

    <T> Subject<T, T> queue(Queue<T> queue);
}
