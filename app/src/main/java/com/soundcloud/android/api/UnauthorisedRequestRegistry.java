package com.soundcloud.android.api;

import com.soundcloud.android.Consts;
import com.soundcloud.android.storage.StorageModule;
import com.soundcloud.android.utils.Log;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.support.annotation.VisibleForTesting;

import java.util.concurrent.TimeUnit;

public class UnauthorisedRequestRegistry {
    private static final String TAG = "RequestRegistry";
    private static final long NO_OBSERVED_TIME = 0L;
    private static final long TIME_LIMIT_IN_MILLISECONDS = TimeUnit.SECONDS.toMillis(5);
    private static final String LAST_OBSERVED_AUTH_ERROR_TIME = "LAST_OBSERVED_AUTH_ERROR_TIME";

    @SuppressLint("StaticFieldLeak")
    private static UnauthorisedRequestRegistry instance;

    private final Context context;
    private final SharedPreferences preferences;

    public static synchronized UnauthorisedRequestRegistry getInstance(Context context) {
        if (instance == null) {
            SharedPreferences preferences = StorageModule.getUnauthorizedErrorsSharedPreferences(context);
            instance = new UnauthorisedRequestRegistry(context, preferences);
        }
        return instance;
    }

    @VisibleForTesting
    protected UnauthorisedRequestRegistry(Context context, SharedPreferences preferences) {
        this.context = context.getApplicationContext();
        this.preferences = preferences;
    }

    public void updateObservedUnauthorisedRequestTimestamp() {
        boolean updated = updateLastObservedAuthErrorTimeIfNotAlreadySet();
        Log.d(TAG, "Observed Unauthorised request timestamp update result = " + updated);
        context.sendBroadcast(new Intent(Consts.GeneralIntents.UNAUTHORIZED));
    }

    public void clearObservedUnauthorisedRequestTimestamp() {
        Log.d(TAG, "Clearing Observed Unauthorised request timestamp");
        preferences.edit().clear().apply();
    }

    public Boolean timeSinceFirstUnauthorisedRequestIsBeyondLimit() {
        long lastObservedAuthErrorTime = getLastObservedAuthErrorTime();
        if (lastObservedAuthErrorTime == NO_OBSERVED_TIME) {
            return false;
        }

        long millisecondsSinceLastObservation = System.currentTimeMillis() - lastObservedAuthErrorTime;
        Log.d(TAG, "Milliseconds since last observed unauthorised request " + millisecondsSinceLastObservation);
        return millisecondsSinceLastObservation >= TIME_LIMIT_IN_MILLISECONDS;

    }

    private boolean updateLastObservedAuthErrorTimeIfNotAlreadySet() {
        if (getLastObservedAuthErrorTime() == NO_OBSERVED_TIME) {
            preferences.edit().putLong(LAST_OBSERVED_AUTH_ERROR_TIME, System.currentTimeMillis()).apply();
            return true;
        }
        return false;
    }

    private long getLastObservedAuthErrorTime() {
        return preferences.getLong(LAST_OBSERVED_AUTH_ERROR_TIME, NO_OBSERVED_TIME);
    }
}
