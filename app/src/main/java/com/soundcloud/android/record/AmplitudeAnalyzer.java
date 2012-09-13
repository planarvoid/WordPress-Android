package com.soundcloud.android.record;

import java.nio.ByteBuffer;

public interface AmplitudeAnalyzer {
    int getLastValue();
    float frameAmplitude(ByteBuffer buffer, int length);
}
