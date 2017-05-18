package com.soundcloud.rx.eventbus;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import rx.Subscriber;
import rx.functions.Action1;
import rx.subjects.BehaviorSubject;
import rx.subjects.PublishSubject;
import rx.subjects.Subject;

/**
 * An Rx Subject that never terminates.
 */
final class EventSubject<T> extends Subject<T, T> {

    private final Subject<T, T> wrappedSubject;
    @NotNull private final Action1<Throwable> onError;

    public static <T> EventSubject<T> create() {
        final PublishSubjectOnSubscribe<T> onSubscribe = new PublishSubjectOnSubscribe<>();
        return new EventSubject<>(onSubscribe, onSubscribe.subject, null);
    }

    public static <T> EventSubject<T> create(@Nullable Action1<Throwable> onError) {
        final PublishSubjectOnSubscribe<T> onSubscribe = new PublishSubjectOnSubscribe<>();
        return new EventSubject<>(onSubscribe, onSubscribe.subject, onError);
    }

    public static <T> EventSubject<T> replaying() {
        final BehaviorSubjectOnSubscribe<T> onSubscribe = new BehaviorSubjectOnSubscribe<>(null);
        return new EventSubject<>(onSubscribe, onSubscribe.subject, null);
    }

    public static <T> EventSubject<T> replaying(@Nullable T defaultEvent, @Nullable Action1<Throwable> onError) {
        final BehaviorSubjectOnSubscribe<T> onSubscribe = new BehaviorSubjectOnSubscribe<>(defaultEvent);
        return new EventSubject<>(onSubscribe, onSubscribe.subject, onError);
    }

    EventSubject(OnSubscribe<T> onSubscribe, Subject<T, T> wrappedSubject,
                 @Nullable Action1<Throwable> onError) {
        super(onSubscribe);
        this.wrappedSubject = wrappedSubject;
        if (onError == null) {
            this.onError = new DefaultErrorHandler();
        } else {
            this.onError = onError;
        }
    }

    @Override
    public void onCompleted() {
        // never process onCompleted
    }

    @Override
    public void onError(Throwable e) {
        onError.call(e);
    }

    @Override
    public void onNext(T t) {
        wrappedSubject.onNext(t);
    }

    @Override
    public boolean hasObservers() {
        return wrappedSubject.hasObservers();
    }

    private static final class PublishSubjectOnSubscribe<T> implements OnSubscribe<T> {

        private final PublishSubject<T> subject = PublishSubject.create();

        @Override
        public void call(Subscriber<? super T> subscriber) {
            subject.subscribe(subscriber);
        }
    }

    private static final class BehaviorSubjectOnSubscribe<T> implements OnSubscribe<T> {

        private final BehaviorSubject<T> subject;

        BehaviorSubjectOnSubscribe(@Nullable T defaultEvent) {
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

    private static final class DefaultErrorHandler implements Action1<Throwable> {
        @Override
        public void call(Throwable throwable) {
            throwable.printStackTrace();
        }
    }
}
