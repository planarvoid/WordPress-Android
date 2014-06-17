package com.soundcloud.android.rx.eventbus;

import rx.Observer;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.subjects.Subject;

import android.util.SparseArray;

import javax.inject.Singleton;

@Singleton
public class DefaultEventBus implements EventBus {

    private final SparseArray<Subject<?, ?>> queues = new SparseArray<Subject<?, ?>>();

    @SuppressWarnings("unchecked")
    @Override
    public <T> Subject<T, T> queue(Queue<T> queue) {
        Subject<T, T> subject = (Subject<T, T>) queues.get(queue.id);
        if (subject == null) {
            if (queue.defaultEvent != null) {
                subject = ReplayEventSubject.create(queue.defaultEvent);
            } else if (queue.replayLast) {
                subject = ReplayEventSubject.create();
            } else {
                subject = DefaultEventSubject.create();
            }
            queues.put(queue.id, subject);
        }
        return subject;
    }

    @Override
    public <T> Subscription subscribe(Queue<T> queue, Observer<T> observer) {
        return this.queue(queue).observeOn(AndroidSchedulers.mainThread()).subscribe(observer);
    }

    @Override
    public <T> Subscription subscribeImmediate(Queue<T> queue, Observer<T> observer) {
        return this.queue(queue).subscribe(observer);
    }

    @Override
    public <T> void publish(Queue<T> queue, T event) {
        this.queue(queue).onNext(event);
    }
}
