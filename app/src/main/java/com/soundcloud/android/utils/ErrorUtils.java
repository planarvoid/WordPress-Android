package com.soundcloud.android.utils;

import static com.soundcloud.android.api.http.PublicApiWrapper.UnauthorizedException;

import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.api.Request;

import android.content.Context;

import java.io.IOException;

public class ErrorUtils {
    /**
     * Use this handler to provide default handling of Throwables in RxJava Observers.
     * <p/>
     * By default, RxJava wraps custom observers in a {@link rx.operators.SafeObserver}
     * which catches exceptions like NullPointerExceptions and forwards them to the
     * source observer. This is NOT what we want; we want a NPE to crash the app and be
     * reported as a crash into Crashlytics.
     * <p/>
     * This methods ensures that only checked exceptions make their way into an observer's
     * onError method, and also logs them silenty into Crashlytics (unless they're blacklisted.)
     *
     * @param t the Exception or Error that was raised
     */
    public static void handleThrowable(Throwable t) {
        if (t instanceof RuntimeException) {
            throw (RuntimeException) t;
        } else if (t instanceof Error) {
            throw new RuntimeException(t);
        } else if (!excludeFromReports(t)) {
            // don't rethrow checked exceptions
            SoundCloudApplication.handleSilentException(t.getMessage(), t);
        }
        t.printStackTrace();
    }

    private static boolean excludeFromReports(Throwable t) {
        return t instanceof IOException;
    }

    public static UnauthorizedException handleUnauthorized(Context context, Request failedRequest) {
        AccountOperations accountOperations = new AccountOperations(context);
        UnauthorizedException exception = new UnauthorizedException(failedRequest, accountOperations.getSoundCloudToken());
        Log.d("Received Unauthorized: " + exception.getMessage());
        SoundCloudApplication.handleSilentException("Received Unauthorized", exception);
        return exception;
    }
}
