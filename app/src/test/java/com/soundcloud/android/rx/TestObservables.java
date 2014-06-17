package com.soundcloud.android.rx;

import rx.Observable;
import rx.Subscriber;
import rx.Subscription;
import rx.functions.Action1;
import rx.observables.ConnectableObservable;
import rx.subscriptions.Subscriptions;

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

    public static <T> MockObservable<T> just(T result) {
        return new MockObservable<T>(new OnSubscribeCapture(Observable.just(result)));
    }

    public static <T> MockObservable<T> emptyObservable() {
        return new MockObservable<T>(new OnSubscribeCapture(Observable.empty()));
    }

    public static <T> MockObservable<T> emptyObservable(Subscription subscription) {
        return new MockObservable<T>(new OnSubscribeCapture(fromSubscription(subscription)));
    }

    public static <T> MockConnectableObservable<T> connectableObservable(T result) {
        return new MockConnectableObservable<T>(new OnSubscribeCapture(Observable.just(result)));
    }

    public static <T> MockConnectableObservable<T> endlessConnectableObservable() {
        return endlessConnectableObservable(Subscriptions.empty());
    }

    public static <T> MockObservable<T> endlessMockObservableFromSubscription(Subscription subscription) {
        return new MockObservable<T>(new OnSubscribeCapture(endlessObservablefromSubscription(subscription)));
    }

    public static <T> MockConnectableObservable<T> endlessConnectableObservable(Subscription subscription) {
        return new MockConnectableObservable<T>(new OnSubscribeCapture(endlessObservablefromSubscription(subscription)));
    }

    public static <T> MockConnectableObservable<T> emptyConnectableObservable(Subscription subscription) {
        return new MockConnectableObservable<T>(new OnSubscribeCapture(fromSubscription(subscription)));
    }

    public static <T> MockConnectableObservable<T> errorConnectableObservable() {
        return new MockConnectableObservable<T>(new OnSubscribeCapture(errorObservable()));
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

    public static class MockConnectableObservable<T> extends ConnectableObservable<T> {

        private final OnSubscribeCapture capture;
        private boolean connected;

        protected MockConnectableObservable(OnSubscribeCapture capture) {
            super(capture);
            this.capture = capture;
        }

        public boolean subscribedTo() {
            return !capture.subscribers.isEmpty();
        }

        public boolean connected() {
            return this.connected;
        }

        public List<Subscriber<? super T>> subscribers() {
            return capture.subscribers;
        }

        @Override
        public void connect(Action1<? super Subscription> connection) {
            connected = true;
            for (Subscriber s : subscribers()) {
                capture.source.subscribe(s);
            }
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
