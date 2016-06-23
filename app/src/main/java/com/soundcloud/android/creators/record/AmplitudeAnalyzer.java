package com.soundcloud.android.creators.record;

import java.nio.ByteBuffer;

public interface AmplitudeAnalyzer {
    int getLastValue();

    float frameAmplitude(ByteBuffer buffer, int length);
}
