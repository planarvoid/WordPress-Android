package com.soundcloud.android.rx.observers;

import io.reactivex.functions.Consumer;

public final class LambdaSingleObserver<T> extends DefaultSingleObserver<T> {

    private final Consumer<T> onSuccessConsumer;

    public static <T> LambdaSingleObserver<T> onSuccess(Consumer<T> onSuccessConsumer) {
        return new LambdaSingleObserver<>(onSuccessConsumer);
    }

    private LambdaSingleObserver(Consumer<T> onSuccessConsumer) {
        this.onSuccessConsumer = onSuccessConsumer;
    }

    @Override
    public void onSuccess(T args) {
        try {
            onSuccessConsumer.accept(args);
            super.onSuccess(args);
        } catch (Exception e) {
            onError(e);
        }
    }
}
