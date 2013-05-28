package com.soundcloud.android.rx.schedulers;

import com.soundcloud.android.rx.ScSchedulers;
import com.soundcloud.android.utils.Log;
import org.jetbrains.annotations.Nullable;
import rx.Observable;
import rx.Scheduler;

public abstract class ScheduledOperations {

    private Scheduler mSubscribeOn;
    private Scheduler mObserveOn;

    protected ScheduledOperations() {
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

    /**
     * Convenience method to have all observables created by this class execute in the background, and call back on
     * the UI thread. This is the safest options when calling from an Activity or Fragment.
     */
    @SuppressWarnings("unchecked")
    public <R extends ScheduledOperations> R scheduleFromActivity() {
        mSubscribeOn = ScSchedulers.BACKGROUND_SCHEDULER;
        mObserveOn = ScSchedulers.UI_SCHEDULER;
        return (R) this;
    }

    /**
     * Convenience method to have all observables created by this class execute in the background.
     */
    @SuppressWarnings("unchecked")
    public <R extends ScheduledOperations> R subscribeInBackground() {
        mSubscribeOn = ScSchedulers.BACKGROUND_SCHEDULER;
        return (R) this;
    }

    /**
     * Convenience method to have all observables created by this class call back on the UI thread.
     */
    @SuppressWarnings("unchecked")
    public <R extends ScheduledOperations> R observeInForeground() {
        mObserveOn = ScSchedulers.UI_SCHEDULER;
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
