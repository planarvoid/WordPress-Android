package com.soundcloud.android.rx.observers;

import rx.functions.Action1;

public class LambdaSubscriber<T> extends DefaultSubscriber<T> {

    private final Action1<T> onNextAction;

    public static <T> LambdaSubscriber<T> onNext(Action1<T> onNextAction) {
        return new LambdaSubscriber<T>(onNextAction);
    }

    private LambdaSubscriber(Action1<T> onNextAction) {
        this.onNextAction = onNextAction;
    }

    @Override
    public void onNext(T args) {
        onNextAction.call(args);
    }
}
