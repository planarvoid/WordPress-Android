package com.soundcloud.android.rx.observers;

import rx.Observer;
import rx.functions.Action0;
import rx.functions.Action1;

public class DefaultObserver<T> implements Observer<T> {

    private Action1<? super T> onNextAction;
    private Action1<Throwable> onErrorAction;
    private Action0 onCompletedAction;

    public DefaultObserver(Action1<? super T> onNextAction) {
        this(onNextAction, null, null);
    }

    public DefaultObserver(Action1<? super T> onNextAction, Action1<Throwable> onErrorAction) {
        this(onNextAction, onErrorAction, null);
    }

    public DefaultObserver(Action1<? super T> onNextAction, Action1<Throwable> onErrorAction, Action0 onCompletedAction) {
        this.onNextAction = onNextAction;
        this.onErrorAction = onErrorAction;
        this.onCompletedAction = onCompletedAction;
    }

    public static <T> DefaultObserver<T> onNext(Action1<? super T> onNextAction) {
        return new DefaultObserver<>(onNextAction);
    }

    @Override
    public void onCompleted() {
        if (onCompletedAction != null) {
            onCompletedAction.call();
        }
    }

    @Override
    public void onError(Throwable e) {
        if (onErrorAction != null) {
            onErrorAction.call(e);
        }
    }

    @Override
    public void onNext(T t) {
        if (onNextAction != null) {
            onNextAction.call(t);
        }
    }
}
