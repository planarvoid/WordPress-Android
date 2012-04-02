package com.soundcloud.android.record;

public interface RecordListener {
    void onFrameUpdate(float maxAmplitude, long elapsed);
}
