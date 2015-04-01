package com.soundcloud.android.utils;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class BufferUtils {
    private BufferUtils() {
    }

    public static ByteBuffer clone(ByteBuffer original) {
        ByteBuffer clone = ByteBuffer.allocate(original.capacity());
        original.rewind();
        clone.put(original);
        original.rewind();
        clone.flip();
        return clone;
    }

    public static ByteBuffer allocateAudioBuffer(int size) {
        ByteBuffer bb = ByteBuffer.allocateDirect(size);
        bb.order(ByteOrder.LITTLE_ENDIAN);
        return bb;
    }
}
