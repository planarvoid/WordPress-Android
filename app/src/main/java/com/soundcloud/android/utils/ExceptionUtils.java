package com.soundcloud.android.utils;

public class ExceptionUtils {

    private static final String OOM_TREND_LABEL = "OOM Trend";

    /*
     * Call this AFTER initialising crash logger (e.g. Crashlytics) to aggregate OOM errors
     */
    public static void setupOOMInterception() {
        final Thread.UncaughtExceptionHandler crashlyticsHandler = Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
            @Override
            public void uncaughtException(Thread thread, Throwable e) {
                if (isCausedByOutOfMemory(e)) {
                    crashlyticsHandler.uncaughtException(thread, new OutOfMemoryError(OOM_TREND_LABEL));
                } else {
                    crashlyticsHandler.uncaughtException(thread, e);
                }
            }
        });
    }

    public static boolean isCausedByOutOfMemory(Throwable uncaught) {
        Throwable crash = uncaught;
        while (crash != null) {
            if (crash instanceof OutOfMemoryError) {
                return true;
            }
            crash = crash.getCause();
        }
        return false;
    }

}
