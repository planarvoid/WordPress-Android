package com.soundcloud.android.events;

import rx.Observable;
import rx.Observer;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.subjects.PublishSubject;
import rx.util.functions.Action1;

import android.util.SparseArray;

public class EventBus2 {

    @SuppressWarnings("unused") // we keep the type variable to enforce type checking
    public static final class QueueDescriptor<T> {
        public final String name;

        private QueueDescriptor(String name) {
            this.name = name;
        }

        public int id() {
            return name.hashCode();
        }

        public static <T> QueueDescriptor<T> create(String name) {
            return new QueueDescriptor<T>(name);
        }
    }

    public interface Queue<T> {
        public abstract Subscription subscribe(Observer<? super T> observer);
        public abstract Subscription subscribe(Action1<? super T> onNext);
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
        public Subscription subscribe(Action1<? super T> onNext) {
            return mSubject.observeOn(AndroidSchedulers.mainThread()).subscribe(onNext);
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
        return this.<T>queue(qd).subscribe(observer);
    }

    public <T> Subscription subscribe(QueueDescriptor<T> qd, Action1<T> onNext) {
        return this.<T>queue(qd).subscribe(onNext);
    }

    public <T> void publish(QueueDescriptor<T> qd, T event) {
        this.<T>queue(qd).publish(event);
    }
}
