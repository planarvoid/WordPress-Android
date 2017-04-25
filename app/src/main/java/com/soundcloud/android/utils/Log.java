package com.soundcloud.android.utils;

import com.soundcloud.android.SoundCloudApplication;
import org.jetbrains.annotations.NotNull;

import android.support.compat.BuildConfig;

public final class Log {
    public static final String ADS_TAG = "ScAds";
    private static LogLevel logLevel = BuildConfig.DEBUG ? LogLevel.VERBOSE : LogLevel.WARN;

    public static void d(@NotNull final String tag, @NotNull final String message) {
        if (shouldLog(LogLevel.DEBUG)) {
            android.util.Log.d(tag, message);
        }
    }

    public static void d(@NotNull final Object obj, @NotNull final String message) {
        d(obj.getClass().getSimpleName(), message);
    }

    public static void d(@NotNull final String message) {
        d(SoundCloudApplication.TAG, message);
    }

    public static void i(@NotNull final String tag, @NotNull final String message) {
        if (shouldLog(LogLevel.WARN)) {
            android.util.Log.i(tag, message);
        }
    }

    public static void i(@NotNull final Object obj, @NotNull final String message) {
        i(obj.getClass().getSimpleName(), message);
    }

    public static void i(@NotNull final String tag, @NotNull final String message, @NotNull final Throwable t) {
        if (shouldLog(LogLevel.VERBOSE)) {
            android.util.Log.i(tag, message, t);
        }
    }

    public static void i(@NotNull final String message) {
        i(SoundCloudApplication.TAG, message);
    }

    public static void w(@NotNull final String tag, @NotNull final String message) {
        if (shouldLog(LogLevel.WARN)) {
            android.util.Log.w(tag, message);
        }
    }

    public static void w(@NotNull final String tag, @NotNull final String message, Throwable exception) {
        if (shouldLog(LogLevel.WARN)) {
            android.util.Log.w(tag, message, exception);
        }
    }

    public static void w(@NotNull final Object obj, @NotNull final String message) {
        w(obj.getClass().getSimpleName(), message);
    }

    public static void w(@NotNull final String message) {
        w(SoundCloudApplication.TAG, message);
    }

    public static void e(@NotNull final String tag, @NotNull final String message) {
        if (shouldLog(LogLevel.ERROR)) {
            android.util.Log.e(tag, message);
        }
    }

    public static void e(@NotNull final String tag, @NotNull final String message, @NotNull Throwable exception) {
        if (shouldLog(LogLevel.ERROR)) {
            android.util.Log.e(tag, message, exception);
        }
    }

    public static void e(@NotNull final Object obj, @NotNull final String message) {
        e(obj.getClass().getSimpleName(), message);
    }

    public static void e(@NotNull final String message, @NotNull final Throwable throwable) {
        e(SoundCloudApplication.TAG, message, throwable);
    }

    public static void e(@NotNull final String message) {
        e(SoundCloudApplication.TAG, message);
    }

    public static void setLogLevel(LogLevel logLevel) {
        Log.logLevel = logLevel;
    }

    private static boolean shouldLog(LogLevel target) {
        return logLevel.ordinal() <= target.ordinal();
    }

    private Log() {
    }

    public enum LogLevel {
        VERBOSE, DEBUG, WARN, ERROR, NONE
    }
}
