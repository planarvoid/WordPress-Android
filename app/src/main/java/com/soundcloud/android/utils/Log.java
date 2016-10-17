package com.soundcloud.android.utils;

import com.soundcloud.android.SoundCloudApplication;
import org.jetbrains.annotations.NotNull;

public final class Log {
    public static final String ADS_TAG = "ScAds";

    public static void d(@NotNull final String tag, @NotNull final String message) {
        android.util.Log.d(tag, message);
    }

    public static void d(@NotNull final Object obj, @NotNull final String message) {
        d(obj.getClass().getSimpleName(), message);
    }

    public static void d(@NotNull final String message) {
        d(SoundCloudApplication.TAG, message);
    }

    public static void i(@NotNull final String tag, @NotNull final String message) {
        android.util.Log.i(tag, message);
    }

    public static void i(@NotNull final Object obj, @NotNull final String message) {
        i(obj.getClass().getSimpleName(), message);
    }

    public static void i(@NotNull final String tag, @NotNull final String message, @NotNull final Throwable t) {
        android.util.Log.i(tag, message, t);
    }

    public static void i(@NotNull final String message) {
        i(SoundCloudApplication.TAG, message);
    }

    public static void w(@NotNull final String tag, @NotNull final String message) {
        android.util.Log.w(tag, message);
    }

    public static void w(@NotNull final String tag, @NotNull final String message, Throwable exception) {
        android.util.Log.w(tag, message, exception);
    }

    public static void w(@NotNull final Object obj, @NotNull final String message) {
        w(obj.getClass().getSimpleName(), message);
    }

    public static void w(@NotNull final String message) {
        w(SoundCloudApplication.TAG, message);
    }

    public static void e(@NotNull final String tag, @NotNull final String message) {
        android.util.Log.e(tag, message);
    }

    public static void e(@NotNull final String tag, @NotNull final String message, @NotNull Throwable exception) {
        android.util.Log.e(tag, message, exception);
    }

    public static void e(@NotNull final Object obj, @NotNull final String message) {
        e(obj.getClass().getSimpleName(), message);
    }

    public static void e(@NotNull final String message) {
        e(SoundCloudApplication.TAG, message);
    }

    private Log() {
    }
}
