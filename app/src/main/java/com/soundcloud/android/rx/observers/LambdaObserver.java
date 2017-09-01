package com.soundcloud.android.rx.observers;

import io.reactivex.functions.Consumer;

public final class LambdaObserver<T> extends DefaultObserver<T> {

    private final Consumer<T> onNextConsumer;

    public static <T> LambdaObserver<T> onNext(Consumer<T> onNextAction) {
        return new LambdaObserver<T>(onNextAction);
    }

    private LambdaObserver(Consumer<T> onNextConsumer) {
        this.onNextConsumer = onNextConsumer;
    }

    @Override
    public void onNext(T args) {
        try {
            onNextConsumer.accept(args);
        } catch (Exception e) {
            onError(e);
        }
    }
}