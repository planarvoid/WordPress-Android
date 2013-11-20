package com.soundcloud.android.api;

import static android.content.SharedPreferences.Editor;

import com.google.common.annotations.VisibleForTesting;
import com.soundcloud.android.rx.ScSchedulers;
import com.soundcloud.android.rx.ScheduledOperations;
import com.soundcloud.android.utils.Log;
import rx.Observable;
import rx.Observer;
import rx.Scheduler;
import rx.Subscription;
import rx.concurrency.Schedulers;
import rx.subscriptions.Subscriptions;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.concurrent.TimeUnit;

public class UnauthorisedRequestRegistry extends ScheduledOperations {
    private static final String TAG = "RequestRegistry";
    private static final String SHARED_PREFERENCE_NAME = "UnauthorisedRequestRegister";
    private static final long NO_OBSERVED_TIME = 0L;
    private static final String OBSERVED_TIMESTAMP_KEY = "first_observed_timestamp";
    private static final long TIME_LIMIT_IN_MINUTES = 2;
    private static UnauthorisedRequestRegistry sInstance;
    private final SharedPreferences mSharedPreferences;
    private final UnauthorisedRequestObserver mUnauthorisedRequestObserver;

    public static synchronized UnauthorisedRequestRegistry getInstance(Context context) {
        if (sInstance == null) {
            sInstance = new UnauthorisedRequestRegistry(context, ScSchedulers.STORAGE_SCHEDULER, new UnauthorisedRequestObserver(context));
        }
        return sInstance;
    }

    @VisibleForTesting
    protected UnauthorisedRequestRegistry(Context context, UnauthorisedRequestRegistry instance,
                                          UnauthorisedRequestObserver unauthorisedRequestObserver) {
        this(context, Schedulers.currentThread(), unauthorisedRequestObserver);
        sInstance = instance;
    }

    private UnauthorisedRequestRegistry(Context context, Scheduler scheduler,
                                        UnauthorisedRequestObserver unauthorisedRequestObserver) {
        super(scheduler);
        mSharedPreferences = context.getSharedPreferences(SHARED_PREFERENCE_NAME, Context.MODE_PRIVATE);
        mUnauthorisedRequestObserver = unauthorisedRequestObserver;
    }

    public void updateObservedUnauthorisedRequestTimestamp() {
        schedule(Observable.create(new Observable.OnSubscribeFunc() {

            @Override
            public Subscription onSubscribe(Observer observer) {
                synchronized (sInstance) {
                    long firstObservedTime = mSharedPreferences.getLong(OBSERVED_TIMESTAMP_KEY, NO_OBSERVED_TIME);
                    if (firstObservationTimeDoesNotExist(firstObservedTime)) {
                        long now = System.currentTimeMillis();
                        Log.d(TAG, "Updating the first observed unauthorised request timestamp to " + now);
                        Editor editor = mSharedPreferences.edit();
                        editor.putLong(OBSERVED_TIMESTAMP_KEY, now);
                        editor.commit();
                    }
                }
                observer.onCompleted();
                return Subscriptions.empty();
            }
        })).subscribe(mUnauthorisedRequestObserver);

    }

    public Observable clearObservedUnauthorisedRequestTimestampAsync() {
        return schedule(Observable.create(new Observable.OnSubscribeFunc<Void>() {

            @Override
            public Subscription onSubscribe(Observer observer) {
                clearObservedUnauthorisedRequestTimestamp();
                observer.onCompleted();
                return Subscriptions.empty();
            }
        }));

    }

    public void clearObservedUnauthorisedRequestTimestamp() {
        synchronized (sInstance) {
            Log.d(TAG, "Clearing the observed timestamp");
            Editor editor = mSharedPreferences.edit();
            editor.clear();
            editor.commit();
        }

    }

    public Observable<Boolean> timeSinceFirstUnauthorisedRequestIsBeyondLimit() {
        return schedule(Observable.create(new Observable.OnSubscribeFunc<Boolean>() {
            @Override
            public Subscription onSubscribe(Observer<? super Boolean> observer) {
                long firstObservedTime;

                synchronized (sInstance) {
                    firstObservedTime = mSharedPreferences.getLong(OBSERVED_TIMESTAMP_KEY, NO_OBSERVED_TIME);
                }

                if (firstObservationTimeDoesNotExist(firstObservedTime)) {
                    observer.onNext(false);
                } else {
                    long minutesSinceFirstObservation = TimeUnit.MINUTES.convert(System.currentTimeMillis() - firstObservedTime, TimeUnit.MILLISECONDS);
                    Log.d(TAG, "Minutes since last observed unauthorised request " + minutesSinceFirstObservation);
                    observer.onNext(minutesSinceFirstObservation >= TIME_LIMIT_IN_MINUTES);
                }
                observer.onCompleted();
                return Subscriptions.empty();
            }

        }));

    }

    private boolean firstObservationTimeDoesNotExist(long firstObservedTime) {
        return firstObservedTime == NO_OBSERVED_TIME;
    }

}
