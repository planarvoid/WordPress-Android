package com.soundcloud.android.rx;

import com.soundcloud.android.utils.Log;
import org.jetbrains.annotations.Nullable;
import rx.Observable;
import rx.Scheduler;

public abstract class ScheduledOperations {

    private Scheduler mSubscribeOn;
    private Scheduler mObserveOn;

    protected ScheduledOperations() {
    }

    protected ScheduledOperations(@Nullable Scheduler subscribeOn) {
        mSubscribeOn = subscribeOn;
    }

    protected ScheduledOperations(@Nullable Scheduler subscribeOn, @Nullable Scheduler observeOn) {
        mSubscribeOn = subscribeOn;
        mObserveOn = observeOn;
    }

    @SuppressWarnings("unchecked")
    public <R extends ScheduledOperations> R scheduleDefault() {
        mSubscribeOn = mObserveOn = null;
        return (R) this;
    }

    @SuppressWarnings("unchecked")
    public <R extends ScheduledOperations> R subscribeOn(Scheduler scheduler) {
        mSubscribeOn = scheduler;
        return (R) this;
    }

    @SuppressWarnings("unchecked")
    public <R extends ScheduledOperations> R observeOn(Scheduler scheduler) {
        mObserveOn = scheduler;
        return (R) this;
    }

    protected <R> Observable<R> schedule(final Observable<R> observable) {
        Observable<R> scheduledObservable = observable;
        if (mSubscribeOn != null) {
            scheduledObservable = scheduledObservable.subscribeOn(mSubscribeOn);
        }
        if (mObserveOn != null) {
            scheduledObservable = scheduledObservable.observeOn(mObserveOn);
        }
        return scheduledObservable;
    }

    protected void log(String msg) {
        Log.d(this, msg + " (thread: " + Thread.currentThread().getName() + ")");
    }

}
