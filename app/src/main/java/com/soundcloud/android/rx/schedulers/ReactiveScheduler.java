package com.soundcloud.android.rx.schedulers;

import com.soundcloud.android.rx.ScActions;
import rx.Observable;
import rx.Observer;
import rx.Scheduler;
import rx.Subscription;
import rx.concurrency.Schedulers;
import rx.subscriptions.Subscriptions;

import android.content.Context;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ReactiveScheduler<T> {

    private static final ExecutorService sExecutor;
    public static final Scheduler BACKGROUND_SCHEDULER;
    public static final Scheduler UI_SCHEDULER;

    static {
        sExecutor = Executors.newSingleThreadExecutor();
        BACKGROUND_SCHEDULER = Schedulers.executor(sExecutor);
        UI_SCHEDULER = new MainThreadScheduler();
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

    //TODO: currently we lose the subscription to the underlying pending observable and merely return the one returned
    //from the decision function (which is fast to execute). We need a way to hold on to the subscription of the
    //actual long running task so that we can disconnect the observer at any point in time
    public Subscription scheduleFirstPendingObservable(Observer<T> observer) {
        if (hasPendingObservables()) {
            Observable<Observable<T>> observable = mPendingObservables.get(0);
            Subscription subscription = observable.subscribe(ScActions.pendingAction(observer));
            mPendingObservables.clear();
            return subscription;
        }
        return Subscriptions.empty();
    }
}
