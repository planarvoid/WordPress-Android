package com.soundcloud.android.commands;

import rx.Observable;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.util.async.operators.OperatorFromFunctionals;

import java.util.concurrent.Callable;

public abstract class Command<I, O, This extends Command<I, O, This>> implements Callable<O>, Func1<I, Observable<O>> {

    protected I input;

    public final This with(I input) {
        this.input = input;
        return (This) this;
    }

    public final I getInput() {
        return input;
    }

    public Observable<O> toObservable() {
        return Observable.create(OperatorFromFunctionals.fromCallable(this));
    }

    public final Action1<I> toAction() {
        return new Action1<I>() {
            @Override
            public void call(I i) {
                try {
                    with(i).call();
                } catch (Exception e) {
                    throw new CommandFailedException(e);
                }
            }
        };
    }

    @Override
    public final Observable<O> call(I i) {
        return with(i).toObservable();
    }

    public final <R> Observable<R> flatMap(Command<O, R, ?> command) {
        return toObservable().flatMap(command);
    }

    public final <R, CmdT extends Command<? super O, R, ?>> CmdT andThen(CmdT command) throws Exception {
        final O thisResult = call();
        return (CmdT) command.with(thisResult);
    }

    public static class CommandFailedException extends RuntimeException {
        public CommandFailedException(Throwable throwable) {
            super(throwable);
        }
    }
}
