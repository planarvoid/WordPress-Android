package com.soundcloud.android.utils;

import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.api.http.APIRequestException;
import com.soundcloud.android.sync.SyncFailedException;
import rx.exceptions.OnErrorNotImplementedException;

import java.io.IOException;

public class ErrorUtils {
    /**
     * Use this handler to provide default handling of Throwables in RxJava Observers.
     * <p/>
     * By default, RxJava wraps custom observers in a {@link rx.observers.SafeSubscriber}
     * which catches exceptions like NullPointerExceptions and forwards them to the
     * source observer. This is NOT what we want; we want a NPE to crash the app and be
     * reported as a crash into Crashlytics.
     * <p/>
     * This methods ensures that only checked exceptions make their way into an observer's
     * onError method, and also logs them silenty into Crashlytics (unless they're blacklisted.)
     * <p/>
     * see https://github.com/Netflix/RxJava/issues/969
     * @param t the Exception or Error that was raised
     */
    public static void handleThrowable(Throwable t) {
        if (t instanceof OnErrorNotImplementedException) {
            throw new FatalException(t.getCause());
        } else if (t instanceof RuntimeException || t instanceof Error) {
            throw new OnErrorNotImplementedException(t);
        } else if (!excludeFromReports(t)) {
            // don't rethrow checked exceptions
            SoundCloudApplication.handleSilentException(t.getMessage(), t);
        }
        t.printStackTrace();
    }

    private static boolean excludeFromReports(Throwable t) {
        return t instanceof IOException || t instanceof APIRequestException || t instanceof SyncFailedException;
    }

    // we use this exception to signal fatal conditions that should crash the app
    public static class FatalException extends RuntimeException {
        public FatalException(Throwable throwable) {
            super(throwable);
        }
    }
}
