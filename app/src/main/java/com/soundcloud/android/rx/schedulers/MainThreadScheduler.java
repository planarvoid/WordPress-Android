package com.soundcloud.android.rx.schedulers;

import rx.Scheduler;
import rx.Subscription;
import rx.subscriptions.Subscriptions;
import rx.util.functions.Action0;
import rx.util.functions.Func0;

import android.os.Handler;
import android.os.Looper;

import java.util.concurrent.TimeUnit;

/**
 * Schedules actions to run on Android's main UI thread.
 */
public class MainThreadScheduler implements Scheduler {

    @Override
    public Subscription schedule(final Func0<Subscription> action) {
        Handler handler = new Handler(Looper.getMainLooper());
        handler.post(new Runnable() {
            @Override
            public void run() {
                action.call();
            }
        });
        return Subscriptions.empty();
    }

    @Override
    public Subscription schedule(Action0 action) {
        return schedule(asFunc0(action));
    }

    @Override
    public Subscription schedule(Action0 action, long dueTime, TimeUnit unit) {
        return schedule(asFunc0(action), dueTime, unit);
    }

    @Override
    public Subscription schedule(Func0<Subscription> action, long dueTime, TimeUnit unit) {
        if (dueTime == 0) {
            return schedule(action);
        } else {
            Handler handler = new Handler(Looper.getMainLooper());
            handler.postAtTime(new Runnable() {
                @Override
                public void run() {
                }
            }, unit.toMillis(dueTime));
        }
        return Subscriptions.empty();
    }

    @Override
    public long now() {
        return System.nanoTime();
    }

    private static Func0<Subscription> asFunc0(final Action0 action) {
        return new Func0<Subscription>() {
            @Override
            public Subscription call() {
                action.call();
                return Subscriptions.empty();
            }
        };
    }
}
