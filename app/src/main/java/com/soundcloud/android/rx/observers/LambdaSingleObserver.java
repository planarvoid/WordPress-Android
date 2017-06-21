package com.soundcloud.android.rx.observers;

import io.reactivex.functions.Consumer;

public final class LambdaSingleObserver<T> extends DefaultSingleObserver<T> {

    private final Consumer<T> onNextConsumer;

    public static <T> LambdaSingleObserver<T> onNext(Consumer<T> onNextAction) {
        return new LambdaSingleObserver<T>(onNextAction);
    }

    private LambdaSingleObserver(Consumer<T> onNextConsumer) {
        this.onNextConsumer = onNextConsumer;
    }

    @Override
    public void onSuccess(T args) {
        try {
            onNextConsumer.accept(args);
            super.onSuccess(args);
        } catch (Exception e) {
            onError(e);
        }
    }
}
