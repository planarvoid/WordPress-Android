package com.soundcloud.android.coreutils.log;


import android.util.Log;

public final class Logger {

    private Logger(){}

    public static void error(final String tag, final String message, Object... args) {
        Log.e(tag, String.format(message, args));
    }

    public static void error(final String tag, final Throwable throwable, final String message, Object... args) {
        Log.e(tag, String.format(message, args), throwable);
    }

    public static void info(final String tag, final String message, Object... args) {
        Log.i(tag, String.format(message, args));
    }


}

