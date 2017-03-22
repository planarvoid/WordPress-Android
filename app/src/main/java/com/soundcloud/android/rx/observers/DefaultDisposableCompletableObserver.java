package com.soundcloud.android.rx.observers;

import com.soundcloud.android.utils.CallsiteToken;
import com.soundcloud.android.utils.ErrorUtils;
import io.reactivex.Completable;
import io.reactivex.Observable;
import io.reactivex.observers.DisposableCompletableObserver;

/**
 * Default {@link DisposableCompletableObserver} base class to be used whenever you want default error handling
 * (cf. {@link ErrorUtils#handleThrowable(Throwable, CallsiteToken)}
 */
public class DefaultDisposableCompletableObserver extends DisposableCompletableObserver {

    private final ErrorReporter errorReporter = new ErrorReporter();

    public static void fireAndForget(Observable observable) {
        Completable.fromObservable(observable)
                   .subscribe(new DefaultDisposableCompletableObserver());
    }

    @Override
    protected void onStart() {
        this.errorReporter.handleOnStart();
    }

    @Override
    public void onComplete() {
        errorReporter.handleOnComplete();
    }

    @Override
    public void onError(Throwable throwable) {
        errorReporter.handleOnError(throwable);
    }
}
