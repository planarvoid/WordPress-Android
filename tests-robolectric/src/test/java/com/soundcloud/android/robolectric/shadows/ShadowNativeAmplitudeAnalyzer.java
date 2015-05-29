package com.soundcloud.android.robolectric.shadows;

import com.soundcloud.android.creators.record.jni.NativeAmplitudeAnalyzer;
import com.xtremelabs.robolectric.internal.Implements;

@SuppressWarnings("UnusedDeclaration")
@Implements(NativeAmplitudeAnalyzer.class)
// WARNING: This shadows a class we own and so depends on
// WARNING: a 'addClassOrPackageToInstrument' line in the TestRunner's constructor
public class ShadowNativeAmplitudeAnalyzer {
    public static int lastValue;

    public int getLastValue() {
        return lastValue;
    }

    public static void reset() {
        lastValue = 0;
    }
}
