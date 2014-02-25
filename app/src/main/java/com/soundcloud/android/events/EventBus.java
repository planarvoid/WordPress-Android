package com.soundcloud.android.events;

import rx.Observable;
import rx.Observer;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.subjects.PublishSubject;

import android.util.SparseArray;

import javax.inject.Singleton;

@Singleton
public class EventBus {

    public static final class QueueDescriptor<T> {
        public final String name;
        public final Class<T> eventType;

        private QueueDescriptor(String name, Class<T> eventType) {
            this.name = name;
            this.eventType = eventType;
        }

        public static <T> QueueDescriptor<T> create(String name, Class<T> eventType) {
            return new QueueDescriptor<T>(name, eventType);
        }

        public static <T extends Event> QueueDescriptor<T> create(Class<T> eventType) {
            return new QueueDescriptor<T>(eventType.getSimpleName(), eventType);
        }

        int id() {
            return name.hashCode();
        }

        @Override
        public int hashCode() {
            return id();
        }

        @Override
        public boolean equals(Object that) {
            return (that != null && that instanceof QueueDescriptor && ((QueueDescriptor) that).id() == this.id());
        }
    }

    public interface Queue<T> {
        public abstract Subscription subscribe(Observer<? super T> observer);
        public abstract void publish(T event);
        public abstract Observable<T> transform();
    }

    private static class SubjectQueue<T> implements Queue<T> {

        private final PublishSubject<T> mSubject = PublishSubject.create();

        @Override
        public Subscription subscribe(Observer<? super T> observer) {
            return mSubject.observeOn(AndroidSchedulers.mainThread()).subscribe(observer);
        }

        @Override
        public void publish(T event) {
            mSubject.onNext(event);
        }

        @Override
        public Observable<T> transform() {
            return mSubject;
        }
    }

    private SparseArray<Queue<?>> mQueues = new SparseArray<Queue<?>>();

    @SuppressWarnings("unchecked")
    public <T> Queue<T> queue(QueueDescriptor<T> qd) {
        final int queueId = qd.id();
        Queue<T> queue = (Queue<T>) mQueues.get(queueId);
        if (queue == null) {
            queue = new SubjectQueue<T>();
            mQueues.put(queueId, queue);
        }
        return queue;
    }

    public <T> Subscription subscribe(QueueDescriptor<T> qd, Observer<T> observer) {
        return this.queue(qd).subscribe(observer);
    }

    public <T> void publish(QueueDescriptor<T> qd, T event) {
        this.queue(qd).publish(event);
    }
}
