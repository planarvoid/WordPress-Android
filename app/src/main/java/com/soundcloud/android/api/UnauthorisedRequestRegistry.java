package com.soundcloud.android.api;

import com.google.common.annotations.VisibleForTesting;
import com.soundcloud.android.Consts;
import com.soundcloud.android.utils.Log;

import android.content.Context;
import android.content.Intent;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

public class UnauthorisedRequestRegistry {
    private static final String TAG = "RequestRegistry";
    private static final long NO_OBSERVED_TIME = 0L;
    private static final long TIME_LIMIT_IN_MINUTES = 2;
    private static UnauthorisedRequestRegistry instance;
    private Context mContext;

    public static synchronized  UnauthorisedRequestRegistry getInstance(Context context){
        if(instance == null){
            instance = new UnauthorisedRequestRegistry(context, new AtomicLong(NO_OBSERVED_TIME));
        }
        return instance;
    }

    @VisibleForTesting
    protected UnauthorisedRequestRegistry(Context context, AtomicLong lastObservedTime){
        mLastObservedTime = lastObservedTime;
        mContext = context.getApplicationContext();
    }

    private final AtomicLong mLastObservedTime;

    public void updateObservedUnauthorisedRequestTimestamp() {
        boolean updated = mLastObservedTime.compareAndSet(NO_OBSERVED_TIME, System.currentTimeMillis());
        Log.d(TAG, "Observed Unauthorised request timestamp update result = " + updated);
        mContext.sendBroadcast(new Intent(Consts.GeneralIntents.UNAUTHORIZED));
    }

    public void clearObservedUnauthorisedRequestTimestamp() {
        Log.d(TAG, "Clearing Observed Unauthorised request timestamp");
        mLastObservedTime.set(NO_OBSERVED_TIME);
    }

    public Boolean timeSinceFirstUnauthorisedRequestIsBeyondLimit() {
        long lastObservedTime = mLastObservedTime.get();
        if(lastObservedTime == NO_OBSERVED_TIME){
            return false;
        }

        long minutesSinceFirstObservation = TimeUnit.MINUTES.convert(System.currentTimeMillis() - lastObservedTime, TimeUnit.MILLISECONDS);
        Log.d(TAG, "Minutes since last observed unauthorised request " + minutesSinceFirstObservation);
        return minutesSinceFirstObservation >= TIME_LIMIT_IN_MINUTES;

    }

    @VisibleForTesting
    public long getLastObservedTime(){
        return mLastObservedTime.get();
    }

    @VisibleForTesting
    public void setLastObservedTime(long value){
        mLastObservedTime.set(value);
    }

}
