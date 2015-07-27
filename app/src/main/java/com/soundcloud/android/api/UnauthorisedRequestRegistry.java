package com.soundcloud.android.api;

import com.soundcloud.android.Consts;
import com.soundcloud.android.utils.Log;

import android.content.Context;
import android.content.Intent;
import android.support.annotation.VisibleForTesting;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

public class UnauthorisedRequestRegistry {
    private static final String TAG = "RequestRegistry";
    private static final long NO_OBSERVED_TIME = 0L;
    private static final long TIME_LIMIT_IN_MILLISECONDS = TimeUnit.SECONDS.toMillis(5);
    private static UnauthorisedRequestRegistry instance;
    private final Context context;
    private final AtomicLong lastObservedTime;

    public static synchronized UnauthorisedRequestRegistry getInstance(Context context) {
        if (instance == null) {
            instance = new UnauthorisedRequestRegistry(context, new AtomicLong(NO_OBSERVED_TIME));
        }
        return instance;
    }

    @VisibleForTesting
    protected UnauthorisedRequestRegistry(Context context, AtomicLong lastObservedTime) {
        this.context = context.getApplicationContext();
        this.lastObservedTime = lastObservedTime;
    }

    public void updateObservedUnauthorisedRequestTimestamp() {
        boolean updated = lastObservedTime.compareAndSet(NO_OBSERVED_TIME, System.currentTimeMillis());
        Log.d(TAG, "Observed Unauthorised request timestamp update result = " + updated);
        context.sendBroadcast(new Intent(Consts.GeneralIntents.UNAUTHORIZED));
    }

    public void clearObservedUnauthorisedRequestTimestamp() {
        Log.d(TAG, "Clearing Observed Unauthorised request timestamp");
        lastObservedTime.set(NO_OBSERVED_TIME);
    }

    public Boolean timeSinceFirstUnauthorisedRequestIsBeyondLimit() {
        long lastObservedTime = this.lastObservedTime.get();
        if (lastObservedTime == NO_OBSERVED_TIME) {
            return false;
        }

        long millisecondsSinceLastObservation = System.currentTimeMillis() - lastObservedTime;
        Log.d(TAG, "Milliseconds since last observed unauthorised request " + millisecondsSinceLastObservation);
        return millisecondsSinceLastObservation >= TIME_LIMIT_IN_MILLISECONDS;

    }

    @VisibleForTesting
    long getLastObservedTime() {
        return lastObservedTime.get();
    }
}
