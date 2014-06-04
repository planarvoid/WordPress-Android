package com.soundcloud.android.rx;

import rx.Subscriber;
import rx.subjects.PublishSubject;
import rx.subjects.Subject;

public class EventSubject<T> extends Subject<T, T> {

    private final PublishSubject<T> wrappedSubject;

    private static final class OnSubscribeFunc<T> implements OnSubscribe<T> {

        private final PublishSubject<T> subject = PublishSubject.create();

        @Override
        public void call(Subscriber<? super T> subscriber) {
            subject.subscribe(subscriber);
        }
    }

    public static <T> EventSubject<T> create() {
        return new EventSubject<T>(new OnSubscribeFunc<T>());
    }

    private EventSubject(OnSubscribeFunc<T> onSubscribeFunc) {
        super(onSubscribeFunc);
        wrappedSubject = onSubscribeFunc.subject;
    }

    @Override
    public void onCompleted() {
        // never process onCompleted
    }

    @Override
    public void onError(Throwable e) {
        wrappedSubject.onError(e);
    }

    @Override
    public void onNext(T t) {
        wrappedSubject.onNext(t);
    }
}
