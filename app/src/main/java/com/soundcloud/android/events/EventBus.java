package com.soundcloud.android.events;

import com.soundcloud.android.rx.EventSubject;
import rx.Observer;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.subjects.BehaviorSubject;
import rx.subjects.Subject;

import android.util.SparseArray;

import javax.annotation.Nullable;
import javax.inject.Singleton;

@Singleton
public class EventBus {

    public static final class Queue<T> {
        public final String name;
        public final Class<T> eventType;
        @Nullable
        private T defaultEvent;

        private Queue(String name, Class<T> eventType) {
            this.name = name;
            this.eventType = eventType;
        }

        private Queue(String name, Class<T> eventType, @Nullable T defaultEvent) {
            this(name, eventType);
            this.defaultEvent = defaultEvent;
        }

        public static <T> Queue<T> create(String name, Class<T> eventType, T defaultEvent) {
            return new Queue<T>(name, eventType, defaultEvent);
        }

        public static <T> Queue<T> create(String name, Class<T> eventType) {
            return new Queue<T>(name, eventType);
        }

        public static <T> Queue<T> create(Class<T> eventType, T defaultEvent) {
            return new Queue<T>(eventType.getSimpleName(), eventType, defaultEvent);
        }

        public static <T> Queue<T> create(Class<T> eventType) {
            return new Queue<T>(eventType.getSimpleName(), eventType);
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
            return (that != null && that instanceof Queue && ((Queue) that).id() == this.id());
        }
    }

    private final SparseArray<Subject<?, ?>> queues = new SparseArray<Subject<?, ?>>();

    @SuppressWarnings("unchecked")
    public <T> Subject<T, T> queue(Queue<T> qd) {
        final int queueId = qd.id();
        Subject<T, T> queue = (Subject<T, T>) queues.get(queueId);
        if (queue == null) {
            queue = qd.defaultEvent == null ? EventSubject.<T>create() : BehaviorSubject.create(qd.defaultEvent);
            queues.put(queueId, queue);
        }
        return queue;
    }

    public <T> Subscription subscribe(Queue<T> qd, Observer<T> observer) {
        return this.queue(qd).observeOn(AndroidSchedulers.mainThread()).subscribe(observer);
    }

    public <T> void publish(Queue<T> qd, T event) {
        this.queue(qd).onNext(event);
    }
}
