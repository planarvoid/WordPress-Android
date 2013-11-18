package com.soundcloud.android.api;

import static android.content.SharedPreferences.Editor;

import com.google.common.annotations.VisibleForTesting;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.concurrent.TimeUnit;

public class UnauthorisedRequestRegistry {
    private static final String SHARED_PREFERENCE_NAME = "UnauthorisedRequestRegister";
    private static final long NO_OBSERVED_TIME = 0L;
    private static final String OBSERVED_TIMESTAMP_KEY = "first_observed_timestamp";
    private static final long TIME_LIMIT_IN_MINUTES = 10;
    private static UnauthorisedRequestRegistry instance;
    private final SharedPreferences mSharedPreference;

    public static synchronized UnauthorisedRequestRegistry getInstance(Context context){
        if(instance == null){
            instance = new UnauthorisedRequestRegistry(context);
        }
        return instance;
    }

    @VisibleForTesting
    protected UnauthorisedRequestRegistry(Context context) {
        mSharedPreference = context.getSharedPreferences(SHARED_PREFERENCE_NAME, Context.MODE_PRIVATE);
    }

    public boolean updateObservedUnauthorisedRequestTimestamp() {
        synchronized (instance) {
            long firstObservedTime = mSharedPreference.getLong(OBSERVED_TIMESTAMP_KEY, NO_OBSERVED_TIME);
            if (firstObservationTimeExists(firstObservedTime)) {
                return false;
            }
            Editor editor = mSharedPreference.edit();
            editor.putLong(OBSERVED_TIMESTAMP_KEY, System.currentTimeMillis());
            editor.commit();
            return true;
        }

    }

    public void clearObservedUnauthorisedRequestTimestamp() {
        synchronized (instance) {
            Editor editor = mSharedPreference.edit();
            editor.clear();
            editor.commit();
        }
    }

    public boolean timeSinceFirstUnauthorisedRequestIsBeyondLimit() {
        synchronized (instance) {
            long firstObservedTime = mSharedPreference.getLong(OBSERVED_TIMESTAMP_KEY, NO_OBSERVED_TIME);
            if (firstObservationTimeDoesNotExist(firstObservedTime)) {
                return false;
            }
            long minutesSinceFirstObservation = TimeUnit.MINUTES.convert(System.currentTimeMillis() - firstObservedTime, TimeUnit.MILLISECONDS);
            return minutesSinceFirstObservation >= TIME_LIMIT_IN_MINUTES;
        }

    }

    private boolean firstObservationTimeDoesNotExist(long firstObservedTime) {
        return firstObservedTime == NO_OBSERVED_TIME;
    }

    private boolean firstObservationTimeExists(long firstObservedTime) {
        return firstObservedTime != NO_OBSERVED_TIME;
    }

}
