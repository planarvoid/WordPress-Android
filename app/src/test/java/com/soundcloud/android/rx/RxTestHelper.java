package com.soundcloud.android.rx;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import rx.Observable;
import rx.Observer;
import rx.Scheduler;
import rx.Subscription;

public class RxTestHelper {

    public static <T> MockObservableBuilder<T> mockObservable() {
        return new MockObservableBuilder<T>(mock(Observable.class));
    }

    public static final class MockObservableBuilder<T> {

        private final Observable<T> observable;

        MockObservableBuilder(Observable<T> observable) {
            this.observable = observable;
        }

        public Observable<T> get() {
            return observable;
        }

        public MockObservableBuilder<T> returning(Subscription subscription) {
            when(observable.subscribe(any(Observer.class))).thenReturn(subscription);
            return this;
        }

        public MockObservableBuilder<T> howeverScheduled() {
            when(observable.subscribeOn(any(Scheduler.class))).thenReturn(observable);
            when(observable.observeOn(any(Scheduler.class))).thenReturn(observable);
            return this;
        }
    }
}
