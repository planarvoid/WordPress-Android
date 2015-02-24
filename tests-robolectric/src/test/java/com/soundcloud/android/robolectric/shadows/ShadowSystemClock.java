package com.soundcloud.android.robolectric.shadows;

import com.xtremelabs.robolectric.internal.Implementation;
import com.xtremelabs.robolectric.internal.Implements;

import android.os.SystemClock;

@Implements(SystemClock.class)
public class ShadowSystemClock {
    private static long uptimeMillis = 0;

    public static void setUptimeMillis(long uptimeMillis) {
        ShadowSystemClock.uptimeMillis = uptimeMillis;
    }

    @Implementation
    public static long uptimeMillis() {
        return uptimeMillis == 0 ? SystemClock.elapsedRealtime() : uptimeMillis;
    }

    public static void reset() {
        uptimeMillis = 0;
    }
}