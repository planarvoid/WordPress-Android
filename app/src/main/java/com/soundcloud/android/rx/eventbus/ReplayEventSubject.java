package com.soundcloud.android.rx.eventbus;

import com.soundcloud.android.utils.ErrorUtils;
import org.jetbrains.annotations.Nullable;
import rx.Subscriber;
import rx.subjects.BehaviorSubject;
import rx.subjects.Subject;

class ReplayEventSubject<T> extends Subject<T, T> {

    private final Subject<T, T> wrappedSubject;

    private static final class OnSubscribeFunc<T> implements OnSubscribe<T> {

        private final BehaviorSubject<T> subject;

        private OnSubscribeFunc(@Nullable T defaultEvent) {
            if (defaultEvent == null) {
                subject = BehaviorSubject.create();
            } else {
                subject = BehaviorSubject.create(defaultEvent);
            }
        }

        @Override
        public void call(Subscriber<? super T> subscriber) {
            subject.subscribe(subscriber);
        }
    }

    public static <T> ReplayEventSubject<T> create() {
        return new ReplayEventSubject<>(new OnSubscribeFunc<T>(null));
    }

    public static <T> ReplayEventSubject<T> create(T defaultEvent) {
        return new ReplayEventSubject<>(new OnSubscribeFunc<>(defaultEvent));
    }

    private ReplayEventSubject(OnSubscribeFunc<T> onSubscribeFunc) {
        super(onSubscribeFunc);
        wrappedSubject = onSubscribeFunc.subject;
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

    @Override
    public boolean hasObservers() {
        return wrappedSubject.hasObservers();
    }
}
