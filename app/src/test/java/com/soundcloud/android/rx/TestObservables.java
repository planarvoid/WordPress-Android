package com.soundcloud.android.rx;

import rx.Observable;
import rx.Subscriber;
import rx.Subscription;

import java.util.LinkedList;
import java.util.List;

public class TestObservables {

    public static <T> Observable<T> fromSubscription(final Subscription subscription) {
        return Observable.create(new Observable.OnSubscribe<T>() {
            @Override
            public void call(Subscriber<? super T> subscriber) {
                subscriber.add(subscription);
                subscriber.onCompleted();
            }
        });
    }

    public static <T> Observable<T> endlessObservablefromSubscription(final Subscription subscription) {
        return Observable.create(new Observable.OnSubscribe<T>() {
            @Override
            public void call(Subscriber<? super T> subscriber) {
                subscriber.add(subscription);
            }
        });
    }

    public static <T> MockObservable<T> emptyObservable() {
        return new MockObservable<T>(new OnSubscribeCapture(Observable.empty()));
    }

    public static <T> MockObservable<T> errorObservable(Throwable error) {
        return new MockObservable<T>(new OnSubscribeCapture(Observable.error(error)));
    }

    public static <T> MockObservable<T> errorObservable() {
        return errorObservable(new Exception());
    }

    public static class MockObservable<T> extends Observable<T> {

        private final OnSubscribeCapture capture;

        protected MockObservable(OnSubscribeCapture capture) {
            super(capture);
            this.capture = capture;
        }

        public boolean subscribedTo() {
            return !capture.subscribers.isEmpty();
        }

        public List<Subscriber<? super T>> subscribers() {
            return capture.subscribers;
        }
    }

    private static final class OnSubscribeCapture<T> implements Observable.OnSubscribe<T> {

        List<Subscriber<? super T>> subscribers = new LinkedList<Subscriber<? super T>>();
        Observable<T> source;

        private OnSubscribeCapture(Observable<T> source) {
            this.source = source;
        }

        @Override
        public void call(Subscriber<? super T> subscriber) {
            this.subscribers.add(subscriber);
            source.subscribe(subscriber);
        }
    }
}
