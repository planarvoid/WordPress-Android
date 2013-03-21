package com.soundcloud.android.rx.schedulers;

import com.soundcloud.android.rx.ScActions;
import com.soundcloud.android.rx.ScObservables;
import com.soundcloud.android.utils.Log;
import org.jetbrains.annotations.Nullable;
import rx.Observable;
import rx.Observer;
import rx.Subscription;

import android.content.Context;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public abstract class ReactiveScheduler<T> {

    protected Context mContext;
    protected ExecutorService mExecutor;

    private List<Observable<Observable<T>>> mPendingObservables;

    public ReactiveScheduler(Context context) {
        mContext = context.getApplicationContext();
        mExecutor = Executors.newSingleThreadExecutor();
        mPendingObservables = new ArrayList<Observable<Observable<T>>>();
    }

    public Context getContext() {
        return mContext;
    }

    public void addPendingObservable(Observable<Observable<T>> observable) {
        mPendingObservables.add(observable);
    }

    public boolean hasPendingObservables() {
        return !mPendingObservables.isEmpty();
    }

    public Subscription schedulePendingObservables(Observer<T> observer, @Nullable Integer limit) {
        Observable<Observable<T>> observable = ScObservables.pendingObservables(mPendingObservables);
        if (limit != null) {
            observable = observable.take(limit);
        }
        Subscription subscription = observable.subscribe(ScActions.pendingAction(observer));
        mPendingObservables.clear();
        return subscription;
    }

    public Subscription schedulePendingObservables(Observer<T> observer) {
        return schedulePendingObservables(observer, null);
    }

    public Subscription scheduleFirstPendingObservable(Observer<T> observer) {
        return schedulePendingObservables(observer, 1);
    }

    protected <T> BackgroundJob<T> newBackgroundJob(final ObservedRunnable<T> runnable) {
        return new BackgroundJob<T>(runnable, mExecutor);
    }

    protected <T> BackgroundJob<T> newBackgroundJob(final ObservedRunnable<T> runnable, final Subscription subscription) {
        return new BackgroundJob<T>(runnable, mExecutor, subscription);
    }

    public abstract Observable<T> loadFromLocalStorage(final long id);

    protected void log(String msg) {
        Log.d(this, msg + " (thread: " + Thread.currentThread().getName() + ")");
    }

}
