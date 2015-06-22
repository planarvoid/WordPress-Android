package com.soundcloud.android.rx;

import rx.Observable;
import rx.Subscriber;
import rx.Subscription;
import rx.functions.Action1;
import rx.observables.ConnectableObservable;

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

    public static <T> Observable<T> withSubscription(final Subscription subscription, Observable<T> source) {
        return source.lift(TestObservables.<T>withSubscription(subscription));
    }

    public static <T> Observable.Operator<T, T> withSubscription(final Subscription subscription) {
        return new Observable.Operator<T, T>() {
            @Override
            public Subscriber<? super T> call(final Subscriber<? super T> subscriber) {
                subscriber.add(subscription);
                return new Subscriber<T>() {
                    @Override
                    public void onCompleted() {
                        subscriber.onCompleted();
                    }

                    @Override
                    public void onError(Throwable e) {
                        subscriber.onError(e);
                    }

                    @Override
                    public void onNext(T t) {
                        subscriber.onNext(t);
                    }
                };
            }
        };
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
        return new MockObservable<>(new OnSubscribeCapture(Observable.just(result)));
    }

    public static <T> MockObservable<T> emptyObservable() {
        return new MockObservable<>(new OnSubscribeCapture(Observable.empty()));
    }

    public static <T> MockObservable<T> emptyObservable(Subscription subscription) {
        return new MockObservable<>(new OnSubscribeCapture(fromSubscription(subscription)));
    }

    public static <T> MockConnectableObservable<T> emptyConnectableObservable(Subscription subscription) {
        return new MockConnectableObservable<>(new OnSubscribeCapture(fromSubscription(subscription)));
    }

    public static <T> MockConnectableObservable<T> errorConnectableObservable() {
        return new MockConnectableObservable<>(new OnSubscribeCapture(errorObservable()));
    }

    public static <T> MockObservable<T> errorObservable(Throwable error) {
        return new MockObservable<>(new OnSubscribeCapture(Observable.error(error)));
    }

    public static <T> MockObservable<T> errorObservable() {
        return errorObservable(new Exception());
    }

    @Deprecated
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

    @Deprecated
    public static class MockConnectableObservable<T> extends ConnectableObservable<T> {

        private final OnSubscribeCapture capture;

        protected MockConnectableObservable(OnSubscribeCapture capture) {
            super(capture);
            this.capture = capture;
        }

        public boolean subscribedTo() {
            return !capture.subscribers.isEmpty();
        }

        public List<Subscriber<? super T>> subscribers() {
            return capture.subscribers;
        }

        @Override
        public void connect(Action1<? super Subscription> connection) {
            for (Subscriber s : subscribers()) {
                capture.source.subscribe(s);
            }
        }
    }

    private static final class OnSubscribeCapture<T> implements Observable.OnSubscribe<T> {

        List<Subscriber<? super T>> subscribers = new LinkedList<>();
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
