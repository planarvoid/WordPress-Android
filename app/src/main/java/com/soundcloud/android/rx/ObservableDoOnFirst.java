package com.soundcloud.android.rx;

import io.reactivex.ObservableOperator;
import io.reactivex.Observer;
import io.reactivex.annotations.NonNull;
import io.reactivex.disposables.Disposable;
import io.reactivex.exceptions.Exceptions;
import io.reactivex.functions.Consumer;

public final class ObservableDoOnFirst<T> implements ObservableOperator<T, T> {

    private final Consumer<T> onFirst;

    public ObservableDoOnFirst(Consumer<T> onFirst) {
        this.onFirst = onFirst;
    }

    @Override
    public Observer<? super T> apply(@NonNull Observer<? super T> observer) throws Exception {
        return new DoOnEmptySubscriber<T>(observer, onFirst);
    }

    @SuppressWarnings("PMD.AvoidCatchingThrowable")
    private static final class DoOnEmptySubscriber<T> implements Observer<T> {

        private final Observer<? super T> child;
        private final Consumer<T> onFirst;
        private boolean alreadyEmitted;

        DoOnEmptySubscriber(Observer<? super T> child, Consumer<T> onFirst) {
            this.child = child;
            this.onFirst = onFirst;
        }

        @Override
        public void onSubscribe(@NonNull Disposable d) {
            child.onSubscribe(d);
        }

        @Override
        public void onNext(@NonNull T t) {
            if (!alreadyEmitted) {
                alreadyEmitted = true;
                try {
                    onFirst.accept(t);
                } catch (Throwable e) {
                    Exceptions.throwIfFatal(e);
                    onError(e);
                    return;
                }
            }
            child.onNext(t);
        }

        @Override
        public void onError(@NonNull Throwable e) {
            child.onError(e);
        }

        @Override
        public void onComplete() {
            child.onComplete();
        }
    }
}
