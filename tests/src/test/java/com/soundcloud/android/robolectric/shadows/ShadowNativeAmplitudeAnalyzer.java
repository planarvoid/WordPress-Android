package com.soundcloud.android.robolectric.shadows;

import com.soundcloud.android.jni.NativeAmplitudeAnalyzer;
import com.xtremelabs.robolectric.internal.Implements;

@SuppressWarnings("UnusedDeclaration")
@Implements(NativeAmplitudeAnalyzer.class)
public class ShadowNativeAmplitudeAnalyzer {
    public static int lastValue;

    public int getLastValue() {
        return lastValue;
    }

    public static void reset() {
        lastValue = 0;
    }
}
