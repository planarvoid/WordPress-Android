package com.soundcloud.android.framework;

import android.util.Log;

import java.util.Date;

public class DebugHelper {
    static public void log(String msg) {
        Log.d("SC_ANDROID_TEST", debugMessage(msg));
    }

    static public String debugMessage(String msg) {
        return String.format("[TEST_DEBUG %s] %s", new Date().toString(), msg);
    }
}
