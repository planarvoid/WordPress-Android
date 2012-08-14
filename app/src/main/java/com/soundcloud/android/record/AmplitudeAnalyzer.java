package com.soundcloud.android.record;

import java.nio.ByteBuffer;

public interface AmplitudeAnalyzer {
    float frameAmplitude(ByteBuffer buffer, int length);
}
