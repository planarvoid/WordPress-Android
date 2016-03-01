package com.soundcloud.android.commands;

import rx.Observable;
import rx.Subscriber;
import rx.functions.Action0;
import rx.functions.Action1;
import rx.functions.Func1;

public abstract class Command<I, O> {

    public abstract O call(I input);

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

    public final Action1<I> toAction1() {
        return new Action1<I>() {
            @Override
            public void call(I i) {
                Command.this.call(i);
            }
        };
    }

    public final Action0 toAction0() {
        return new Action0() {
            @Override
            public void call() {
                Command.this.call(null);
            }
        };
    }

    public final Func1<I, Observable<O>> toContinuation() {
        return new Func1<I, Observable<O>>() {
            @Override
            public Observable<O> call(I i) {
                return toObservable(i);
            }
        };
    }

}
