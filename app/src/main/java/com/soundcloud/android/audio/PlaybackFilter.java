package com.soundcloud.android.audio;

import java.nio.ByteBuffer;

public interface PlaybackFilter {
    ByteBuffer apply(ByteBuffer buffer, long position, long length);
}
