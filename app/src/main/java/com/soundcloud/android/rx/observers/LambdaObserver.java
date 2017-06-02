package com.soundcloud.android.rx.observers;

import io.reactivex.functions.Consumer;

public class LambdaObserver<T> extends DefaultObserver<T> {

    private final Consumer<T> onNextComsumer;

    public static <T> LambdaObserver<T> onNext(Consumer<T> onNextAction) {
        return new LambdaObserver<T>(onNextAction);
    }

    private LambdaObserver(Consumer<T> onNextComsumer) {
        this.onNextComsumer = onNextComsumer;
    }

    @Override
    public void onNext(T args) {
        try {
            onNextComsumer.accept(args);
        } catch (Exception e) {
            onError(e);
        }
    }
}
