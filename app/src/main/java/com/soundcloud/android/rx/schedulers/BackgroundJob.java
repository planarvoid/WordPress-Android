package com.soundcloud.android.rx.schedulers;

import com.soundcloud.android.rx.observers.DetachableObserver;
import com.soundcloud.android.utils.Log;
import rx.Observer;
import rx.Subscription;
import rx.subscriptions.Subscriptions;
import rx.util.functions.Func1;

import java.util.concurrent.ExecutorService;

class BackgroundJob<T> implements Func1<Observer<T>, Subscription> {

    private final ObservedRunnable<T> mRunnable;
    private final ExecutorService mExecutor;
    private final Subscription mSubscription;

    BackgroundJob(ObservedRunnable<T> runnable, ExecutorService executor, Subscription subscription) {
        mRunnable = runnable;
        mExecutor = executor;
        mSubscription = subscription;
    }

    BackgroundJob(ObservedRunnable<T> runnable, ExecutorService executor) {
        this(runnable, executor, Subscriptions.empty());
    }

    @Override
    public Subscription call(Observer<T> observer) {
        mRunnable.attachObserver(new DetachableObserver<T>(observer));
        mExecutor.execute(mRunnable);
        return new Subscription() {
            @Override
            public void unsubscribe() {
                Log.d(BackgroundJob.this, "unsubscribe from " + BackgroundJob.class.getSimpleName());
                mRunnable.detachObserver();
                mSubscription.unsubscribe();
            }
        };
    }


    @Override
    protected void finalize() throws Throwable {
        super.finalize();
        Log.d(this, "FINALIZE");
    }
}
