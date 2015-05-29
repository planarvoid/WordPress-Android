package com.soundcloud.android.rx.eventbus;

import com.soundcloud.android.utils.ErrorUtils;
import rx.Subscriber;
import rx.subjects.PublishSubject;
import rx.subjects.Subject;

class DefaultEventSubject<T> extends Subject<T, T> {

    private final Subject<T, T> wrappedSubject;

    private static final class OnSubscribeFunc<T> implements OnSubscribe<T> {

        private final PublishSubject<T> subject = PublishSubject.create();

        @Override
        public void call(Subscriber<? super T> subscriber) {
            subject.subscribe(subscriber);
        }
    }

    public static <T> DefaultEventSubject<T> create() {
        return new DefaultEventSubject<>(new OnSubscribeFunc<T>());
    }

    private DefaultEventSubject(OnSubscribeFunc<T> onSubscribeFunc) {
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
