package com.soundcloud.android.rx;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import rx.Subscription;
import rx.android.concurrency.AndroidSchedulers;
import rx.observables.ConnectableObservable;

public class RxTestHelper {

    public static <T> ConnectableObservable<T> connectableObservableReturning(Subscription subscription) {
        ConnectableObservable mockObservable = mock(ConnectableObservable.class);
        when(mockObservable.connect()).thenReturn(subscription);
        when(mockObservable.observeOn(AndroidSchedulers.mainThread())).thenReturn(mockObservable);
        return mockObservable;
    }

}
