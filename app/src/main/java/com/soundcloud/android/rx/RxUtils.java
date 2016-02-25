package com.soundcloud.android.rx;

import com.soundcloud.android.Consts;
import rx.Observable;
import rx.Observer;
import rx.Subscription;
import rx.functions.Func1;
import rx.subscriptions.Subscriptions;

import java.util.List;

public final class RxUtils {

    public static final Func1<Boolean, Boolean> IS_TRUE = new Func1<Boolean, Boolean>() {
        @Override
        public Boolean call(Boolean isTrue) {
            return isTrue;
        }
    };

    public static final Func1<Boolean, Boolean> IS_FALSE = new Func1<Boolean, Boolean>() {
        @Override
        public Boolean call(Boolean isTrue) {
            return !isTrue;
        }
    };

    public static final Func1<Object, Void> TO_VOID = new Func1<Object, Void>() {
        @Override
        public Void call(Object ignore) {
            return null;
        }
    };

    public static final Func1<Long, Boolean> IS_VALID_TIMESTAMP = new Func1<Long, Boolean>() {
        @Override
        public Boolean call(Long ts) {
            return ts != Consts.NOT_SET;
        }
    };

    public static final Object EMPTY_VALUE = new Object();

    public static <T> void emitIterable(Observer<? super T> observer, Iterable<T> iterable) {
        for (T item : iterable) {
            observer.onNext(item);
        }
    }

    /**
     * @return A Subscription that is always unsubscribed. Can use as a Null object; reference equality
     * checks are safe to perform.
     */
    public static Subscription invalidSubscription() {
        return Subscriptions.unsubscribed();
    }

    public static <T> Func1<Iterable<T>, Observable<T>> iterableToObservable() {
        return new Func1<Iterable<T>, Observable<T>>() {
            @Override
            public Observable<T> call(Iterable<T> items) {
                return Observable.from(items);
            }
        };
    }

    public static <T> Func1<Object, T> returning(final T obj) {
        return new Func1<Object, T>() {
            @Override
            public T call(Object o) {
                return obj;
            }
        };
    }

    public static <T> Func1<Object, Observable<T>> continueWith(final Observable<T> continuation) {
        return new Func1<Object, Observable<T>>() {
            @Override
            public Observable<T> call(Object o) {
                return continuation;
            }
        };
    }

    public static <T> Func1<List<T>, Boolean> filterEmptyLists() {
        return new Func1<List<T>, Boolean>() {
            @Override
            public Boolean call(List<T> list) {
                return !list.isEmpty();
            }
        };
    }

    private RxUtils() {
    }
}
