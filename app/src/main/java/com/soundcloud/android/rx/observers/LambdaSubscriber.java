package com.soundcloud.android.rx.observers;

import rx.functions.Action1;

/**
 * <p>This class has been deprecated in favor of {@link LambdaObserver<T>} and for the sake of migrating from RxJava 1 to
 * RxJava 2.</p>
 */
@Deprecated
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
