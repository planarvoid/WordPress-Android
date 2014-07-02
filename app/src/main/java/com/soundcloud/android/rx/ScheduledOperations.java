package com.soundcloud.android.rx;

import com.soundcloud.android.utils.Log;
import org.jetbrains.annotations.Nullable;
import rx.Observable;
import rx.Scheduler;

public abstract class ScheduledOperations {

    private Scheduler subscribeOn;

    protected ScheduledOperations() {
    }

    protected ScheduledOperations(@Nullable Scheduler subscribeOn) {
        this.subscribeOn = subscribeOn;
    }

    @SuppressWarnings("unchecked")
    public <R extends ScheduledOperations> R subscribeOn(Scheduler scheduler) {
        subscribeOn = scheduler;
        return (R) this;
    }

    protected <R> Observable<R> schedule(final Observable<R> observable) {
        Observable<R> scheduledObservable = observable;
        if (subscribeOn != null) {
            scheduledObservable = scheduledObservable.subscribeOn(subscribeOn);
        }
        return scheduledObservable;
    }

    protected void log(String msg) {
        Log.d(this, msg + " (thread: " + Thread.currentThread().getName() + ")");
    }

}
