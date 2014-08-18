package com.soundcloud.android.utils;

import com.crashlytics.android.Crashlytics;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.api.APIRequestException;
import com.soundcloud.android.properties.ApplicationProperties;
import com.soundcloud.android.sync.SyncFailedException;
import org.jetbrains.annotations.Nullable;
import rx.exceptions.OnErrorNotImplementedException;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

public class ErrorUtils {

    public static final String ERROR_CONTEXT_TAG = "error-context";

    public static void handleThrowable(Throwable t, CallsiteToken callsiteToken) {
        final StringWriter callsiteTrace = new StringWriter();
        callsiteToken.printStackTrace(new PrintWriter(callsiteTrace));
        handleThrowable(t, callsiteTrace.toString());
    }

    public static void handleThrowable(Throwable t, Class<?> context) {
        handleThrowable(t, context.getCanonicalName());
    }

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
     * @param context an extra message that can be attached to clarify the error context
     */
    public static void handleThrowable(Throwable t, String context) {
        Log.e(ERROR_CONTEXT_TAG, context);

        if (ApplicationProperties.shouldReportCrashes()) {
            Crashlytics.setString(ERROR_CONTEXT_TAG, context);
        }
        if (t instanceof OnErrorNotImplementedException) {
            throw new FatalException(t.getCause());
        } else if (t instanceof RuntimeException || t instanceof Error) {
            throw new OnErrorNotImplementedException(t);
        } else if (!excludeFromReports(t)) {
            // don't rethrow checked exceptions
            handleSilentException(t);
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

    public static void handleSilentException(String message, Throwable e) {
        handleSilentException(e, "message", message);
    }

    public static void handleSilentException(Throwable e) {
        handleSilentException(e, null, null);
    }

    private static void handleSilentException(Throwable e, @Nullable String contextKey, @Nullable String contextValue) {
        if (ApplicationProperties.shouldReportCrashes()) {
            Log.e(SoundCloudApplication.TAG, "Handling silent exception: " + e);
            if (contextKey != null && contextValue != null) {
                Crashlytics.setString(contextKey, contextValue);
            }
            Crashlytics.logException(e);
        }
    }
}
