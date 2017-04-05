package com.soundcloud.android;

import com.soundcloud.android.utils.CrashlyticsMemoryReporter;
import com.soundcloud.android.utils.ErrorUtils;
import com.soundcloud.android.utils.MemoryReporter;
import rx.exceptions.OnErrorFailedException;

import android.app.ActivityManager;
import android.content.Context;

// Note : don't use injection in this class.
// It is used before Dagger is setup - otherwise we would never report potential Dagger crashes.
class UncaughtExceptionHandlerController {
    private static final String OOM_TREND_LABEL = "OOM Trend";

    private final MemoryReporter memoryReporter;
    private Thread.UncaughtExceptionHandler handler;

    public UncaughtExceptionHandlerController(Context context, boolean isReportingCrashes) {
        final ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        if (isReportingCrashes) {
            memoryReporter = new CrashlyticsMemoryReporter(activityManager);
        } else {
            memoryReporter = new MemoryReporter(activityManager);
        }
    }

    UncaughtExceptionHandlerController(MemoryReporter memoryReporter) {
        this.memoryReporter = memoryReporter;
    }

    void reportSystemMemoryStats() {
        memoryReporter.reportSystemMemoryStats();
    }

    void reportMemoryTrim(int level) {
        memoryReporter.reportMemoryTrim(level);
    }

    /*
     * Call this AFTER initialising crash logger (e.g. Crashlytics) to aggregate OOM errors
     */
    void setHandler() {
        final Thread.UncaughtExceptionHandler crashlyticsHandler = Thread.getDefaultUncaughtExceptionHandler();
        handler = (thread, e) -> {
            //TODO evaluate this on alpha/beta and enable it on release later
            final Throwable purged = BuildConfig.BUILD_TYPE.equals("release") ? e : ErrorUtils.purgeOnNextValueCause(e);
            if (ErrorUtils.isCausedByOutOfMemory(e)) {
                memoryReporter.reportOomStats();
                crashlyticsHandler.uncaughtException(thread, new OutOfMemoryError(OOM_TREND_LABEL));
            } else if (e.getCause() instanceof OnErrorFailedException) {
                // This is to remove clutter from exceptions that are caught and redirected on RxJava worker threads.
                // See ScheduledAction.java. It should give us cleaner stack traces containing just the root cause.
                final Throwable rootCause = ErrorUtils.findRootCause(purged.getCause());
                crashlyticsHandler.uncaughtException(thread, rootCause);
            } else {
                crashlyticsHandler.uncaughtException(thread, purged);
            }
        };
        Thread.setDefaultUncaughtExceptionHandler(handler);
    }

    void assertHandlerIsSet() {
        if (handler != Thread.getDefaultUncaughtExceptionHandler()) {
            final String detailMessage = "Illegal handler: " + Thread.getDefaultUncaughtExceptionHandler();
            setHandler();
            ErrorUtils.handleSilentException(detailMessage,
                                             new IllegalUncaughtExceptionHandlerException(detailMessage));
        }
    }

    class IllegalUncaughtExceptionHandlerException extends RuntimeException {
        public IllegalUncaughtExceptionHandlerException(String detailMessage) {
            super(detailMessage);
        }
    }
}
