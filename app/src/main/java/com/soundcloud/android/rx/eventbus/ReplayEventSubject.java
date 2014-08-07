package com.soundcloud.android.rx.eventbus;

import com.soundcloud.android.utils.ErrorUtils;
import org.jetbrains.annotations.Nullable;
import rx.Subscriber;
import rx.subjects.ReplaySubject;
import rx.subjects.Subject;

class ReplayEventSubject<T> extends Subject<T, T> {

    private final ReplaySubject<T> wrappedSubject;

    private static final class OnSubscribeFunc<T> implements OnSubscribe<T> {

        private final ReplaySubject<T> subject = ReplaySubject.createWithSize(1);

        @Override
        public void call(Subscriber<? super T> subscriber) {
            subject.subscribe(subscriber);
        }
    }

    public static <T> ReplayEventSubject<T> create() {
        return new ReplayEventSubject<T>(new OnSubscribeFunc<T>(), null);
    }

    public static <T> ReplayEventSubject<T> create(T defaultEvent) {
        return new ReplayEventSubject<T>(new OnSubscribeFunc<T>(), defaultEvent);
    }

    private ReplayEventSubject(OnSubscribeFunc<T> onSubscribeFunc, @Nullable T defaultEvent) {
        super(onSubscribeFunc);
        wrappedSubject = onSubscribeFunc.subject;
        if (defaultEvent != null) {
            onNext(defaultEvent);
        }
    }

    @Override
    public void onCompleted() {
        // never process onCompleted
    }

    @Override
    public void onError(Throwable e) {
        ErrorUtils.handleThrowable(e, getClass());
    }

    @Override
    public void onNext(T t) {
        wrappedSubject.onNext(t);
    }
}
