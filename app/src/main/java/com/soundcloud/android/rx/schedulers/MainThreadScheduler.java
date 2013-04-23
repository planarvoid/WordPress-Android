package com.soundcloud.android.rx.schedulers;

import rx.Scheduler;
import rx.Subscription;
import rx.subscriptions.Subscriptions;
import rx.util.AtomicObservableSubscription;
import rx.util.functions.Action0;
import rx.util.functions.Func0;
import rx.util.functions.Func2;

import android.os.Handler;
import android.os.Looper;

import java.util.concurrent.TimeUnit;

/**
 * Schedules actions to run on Android's main UI thread.
 */
public class MainThreadScheduler extends Scheduler {

    @Override
    public <T> Subscription schedule(final T state, final Func2<Scheduler, T, Subscription> action) {
        final Handler handler = new Handler(Looper.getMainLooper());
        final AtomicObservableSubscription subscription = new AtomicObservableSubscription();
        final Scheduler _scheduler = this;

        handler.post(new Runnable() {
            @Override
            public void run() {
                subscription.wrap(action.call(_scheduler, state));
            }
        });
        return subscription;
    }

    @Override
    public <T> Subscription schedule(final T state, final Func2<Scheduler, T, Subscription> action, long delayTime, TimeUnit unit) {
        if (delayTime == 0) {
            return schedule(state, action);
        } else {
            final AtomicObservableSubscription subscription = new AtomicObservableSubscription();
            final Scheduler _scheduler = this;
            final Handler handler = new Handler(Looper.getMainLooper());
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    subscription.wrap(action.call(_scheduler, state));
                }
            }, unit.toMillis(delayTime));
            return subscription;
        }
    }

}
