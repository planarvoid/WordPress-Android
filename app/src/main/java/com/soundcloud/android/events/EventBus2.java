package com.soundcloud.android.events;

import rx.Observable;
import rx.Observer;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.subjects.PublishSubject;

import android.util.SparseArray;

public class EventBus2 {

    public interface Queue<T> {
        String name();
        Subscription subscribe(Observer<? super T> observer);
        void publish(T event);
        Observable<T> transform();
    }

    private class SubjectQueue<T> implements Queue<T> {

        private final String mName;
        private final PublishSubject<T> mSubject = PublishSubject.create();

        private SubjectQueue(String name) {
            mName = name;
        }

        @Override
        public String name() {
            return mName;
        }

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

    public <T> Queue<T> queue(String name) {
        return (Queue<T>) mQueues.get(name.hashCode());
    }

    public <T> void registerQueue(String name) {
        mQueues.put(name.hashCode(), new SubjectQueue<T>(name));
    }

    public <T> Subscription subscribe(String queueName, Observer<T> observer) {
        return this.<T>queue(queueName).subscribe(observer);
    }

    public <T> void publish(String queueName, T event) {
        this.<T>queue(queueName).publish(event);
    }
}
