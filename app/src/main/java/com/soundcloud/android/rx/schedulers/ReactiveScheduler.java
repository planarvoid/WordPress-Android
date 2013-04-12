package com.soundcloud.android.rx.schedulers;

import com.soundcloud.android.rx.ScActions;
import com.soundcloud.android.rx.ScObservables;
import org.jetbrains.annotations.Nullable;
import rx.Observable;
import rx.Observer;
import rx.Subscription;

import android.content.Context;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ReactiveScheduler<T> {

    private static final ExecutorService sExecutor;

    static {
        sExecutor = Executors.newSingleThreadExecutor();
    }

    protected Context mContext;

    private List<Observable<Observable<T>>> mPendingObservables;

    public ReactiveScheduler(Context context) {
        mContext = context.getApplicationContext();
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

    public Subscription scheduleFirstPendingObservable(Observer<T> observer) {
        if (hasPendingObservables()) {
            Observable<Observable<T>> observable = mPendingObservables.get(0);
            Subscription subscription = observable.subscribe(ScActions.pendingAction(observer));
            mPendingObservables.clear();
            return subscription;
        }
        return Subscriptions.empty();
    }

    public static <T> BackgroundJob<T> newBackgroundJob(final ObservedRunnable<T> runnable) {
        return new BackgroundJob<T>(runnable, sExecutor);
    }

    public static <T> BackgroundJob<T> newBackgroundJob(final ObservedRunnable<T> runnable, final Subscription subscription) {
        return new BackgroundJob<T>(runnable, sExecutor, subscription);
    }

}
