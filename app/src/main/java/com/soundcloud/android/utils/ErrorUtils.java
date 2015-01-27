package com.soundcloud.android.utils;

import com.crashlytics.android.Crashlytics;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.common.annotations.VisibleForTesting;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.api.ApiRequestException;
import com.soundcloud.android.sync.SyncFailedException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import rx.exceptions.OnErrorFailedException;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Map;

public final class ErrorUtils {

    public static final String ERROR_CONTEXT_TAG = "error-context";
    private static final String OOM_TREND_LABEL = "OOM Trend";

    private ErrorUtils() {
        // not to be instantiated.
    }

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
     * onError method, and also logs them silently into Crashlytics (unless they're blacklisted.)
     * <p/>
     * see https://github.com/Netflix/RxJava/issues/969
     *
     * @param t       the Exception or Error that was raised
     * @param context an extra message that can be attached to clarify the error context
     */
    public static synchronized void handleThrowable(Throwable t, String context) {
        Log.e(ERROR_CONTEXT_TAG, context);

        if (Crashlytics.getInstance().isInitialized()) {
            Crashlytics.setString(ERROR_CONTEXT_TAG, context);
        }
        if (t instanceof RuntimeException) {
            throw (RuntimeException) t;
        } else if (includeInReports(t)) {
            // don't rethrow checked exceptions
            handleSilentException(t);
        } else {
            t.printStackTrace();
        }
    }

    /*
         * Call this AFTER initialising crash logger (e.g. Crashlytics) to aggregate OOM errors
         */
    public static void setupUncaughtExceptionHandler(final MemoryReporter memoryReporter) {
        final Thread.UncaughtExceptionHandler crashlyticsHandler = Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
            @Override
            public void uncaughtException(Thread thread, Throwable e) {
                if (isCausedByOutOfMemory(e)) {
                    memoryReporter.reportOomStats();
                    crashlyticsHandler.uncaughtException(thread, new OutOfMemoryError(OOM_TREND_LABEL));
                } else if (e.getCause() instanceof OnErrorFailedException) {
                    // This is to remove clutter from exceptions that are caught and redirected on RxJava worker threads.
                    // See ScheduledAction.java. It should give us cleaner stack traces containing just the root cause.
                    crashlyticsHandler.uncaughtException(thread, findRootCause(e));
                } else {
                    crashlyticsHandler.uncaughtException(thread, e);
                }
            }
        });
    }

    @VisibleForTesting
    static boolean includeInReports(Throwable t) {
        if (t instanceof SyncFailedException || isIOExceptionUnrelatedToParsing(t)) {
            return false;
        }
        if (t instanceof ApiRequestException) {
            return ((ApiRequestException) t).loggable();
        }
        return true;
    }

    private static boolean isIOExceptionUnrelatedToParsing(Throwable t) {
        return IOException.class.isAssignableFrom(t.getClass()) && !JsonProcessingException.class.isAssignableFrom(t.getClass());
    }

    public static void handleSilentException(String message, Throwable e) {
        handleSilentException(e, "message", message);
    }

    public static void handleSilentException(Throwable e) {
        handleSilentException(e, null, null);
    }

    public static void handleSilentException(Throwable e, @NotNull Map<String, String> customLogs) {
        if (Crashlytics.getInstance().isInitialized()) {
            Log.e(SoundCloudApplication.TAG, "Handling silent exception: " + e);
            for (Map.Entry<String, String> entry : customLogs.entrySet()) {
                Crashlytics.setString(entry.getKey(), entry.getValue());
            }
            Crashlytics.logException(e);
        }
    }

    private static synchronized void handleSilentException(
            Throwable e, @Nullable String contextKey, @Nullable String contextValue) {
        e.printStackTrace();
        if (Crashlytics.getInstance().isInitialized()) {
            Log.e(SoundCloudApplication.TAG, "Handling silent exception: " + e);
            if (contextKey != null && contextValue != null) {
                Crashlytics.setString(contextKey, contextValue);
            }
            Crashlytics.logException(e);
        }
    }

    @VisibleForTesting
    static boolean isCausedByOutOfMemory(Throwable uncaught) {
        Throwable throwable = uncaught;
        while (throwable != null) {
            if (throwable instanceof OutOfMemoryError) {
                return true;
            }
            throwable = throwable.getCause();
        }
        return false;
    }

    @Nullable
    static Throwable findRootCause(@Nullable Throwable throwable) {
        if (throwable == null) {
            return null;
        } else {
            Throwable rootCause = throwable;
            while (rootCause.getCause() != null) {
                rootCause = rootCause.getCause();
            }
            return rootCause;
        }
    }

}
