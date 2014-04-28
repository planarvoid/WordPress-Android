package com.soundcloud.android.rx;

import com.soundcloud.android.utils.Log;
import org.jetbrains.annotations.Nullable;
import rx.Observable;
import rx.Scheduler;

public abstract class ScheduledOperations {

    private Scheduler subscribeOn;
    private Scheduler observeOn;

    protected ScheduledOperations() {
    }

    protected ScheduledOperations(@Nullable Scheduler subscribeOn) {
        this.subscribeOn = subscribeOn;
    }

    protected ScheduledOperations(@Nullable Scheduler subscribeOn, @Nullable Scheduler observeOn) {
        this.subscribeOn = subscribeOn;
        this.observeOn = observeOn;
    }

    @SuppressWarnings("unchecked")
    public <R extends ScheduledOperations> R scheduleDefault() {
        subscribeOn = observeOn = null;
        return (R) this;
    }

    @SuppressWarnings("unchecked")
    public <R extends ScheduledOperations> R subscribeOn(Scheduler scheduler) {
        subscribeOn = scheduler;
        return (R) this;
    }

    @SuppressWarnings("unchecked")
    public <R extends ScheduledOperations> R observeOn(Scheduler scheduler) {
        observeOn = scheduler;
        return (R) this;
    }

    protected <R> Observable<R> schedule(final Observable<R> observable) {
        Observable<R> scheduledObservable = observable;
        if (subscribeOn != null) {
            scheduledObservable = scheduledObservable.subscribeOn(subscribeOn);
        }
        if (observeOn != null) {
            scheduledObservable = scheduledObservable.observeOn(observeOn);
        }
        return scheduledObservable;
    }

    protected void log(String msg) {
        Log.d(this, msg + " (thread: " + Thread.currentThread().getName() + ")");
    }

}
