package com.soundcloud.android.commands;

import io.reactivex.Single;
import io.reactivex.functions.Consumer;
import rx.Observable;
import rx.Subscriber;
import rx.functions.Action0;
import rx.functions.Action1;
import rx.functions.Func1;

public abstract class Command<I, O> {

    public abstract O call(I input);

    public O call() {
        return call(null);
    }

    public Observable<O> toObservable(final I input) {
        return Observable.create(new Observable.OnSubscribe<O>() {
            @Override
            public void call(Subscriber<? super O> subscriber) {
                try {
                    subscriber.onNext(Command.this.call(input));
                    subscriber.onCompleted();
                } catch (Throwable t) {
                    subscriber.onError(t);
                }
            }
        });
    }

    public Single<O> toSingle(final I input) {
        return Single.fromCallable(() -> call(input));
    }

    public final Consumer<I> toConsumer() {
        return Command.this::call;
    }

    @Deprecated
    /** Use {@link Command#toConsumer()} */
    public final Action1<I> toAction1() {
        return Command.this::call;
    }

    final Action0 toAction0() {
        return Command.this::call;
    }

    public final Func1<I, Observable<O>> toContinuation() {
        return this::toObservable;
    }
}
