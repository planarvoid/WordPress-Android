package com.soundcloud.android.rx.observers;

import io.reactivex.functions.Consumer;

public final class LambdaMaybeObserver<T> extends DefaultMaybeObserver<T> {

    private final Consumer<T> onNextConsumer;

    public static <T> LambdaMaybeObserver<T> onNext(Consumer<T> onNextAction) {
        return new LambdaMaybeObserver<T>(onNextAction);
    }

    private LambdaMaybeObserver(Consumer<T> onNextConsumer) {
        this.onNextConsumer = onNextConsumer;
    }

    @Override
    public void onSuccess(T args) {
        try {
            onNextConsumer.accept(args);
        } catch (Exception e) {
            onError(e);
        }
    }
}
