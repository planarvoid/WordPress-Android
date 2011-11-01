package com.soundcloud.android.streaming;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

public class BufferUtils {
    private BufferUtils() {
    }

    static ByteBuffer readToByteBuffer(File f) throws IOException {
        return readToByteBuffer(f, (int) f.length());
    }

    static ByteBuffer readToByteBuffer(File f, int toRead) throws IOException {
        ByteBuffer b = ByteBuffer.allocate(toRead);
        FileChannel fc = new FileInputStream(f).getChannel();
        fc.read(b);
        b.flip();
        return b;
    }

    public static ByteBuffer clone(ByteBuffer original) {
        ByteBuffer clone = ByteBuffer.allocate(original.capacity());
        original.rewind();
        clone.put(original);
        original.rewind();
        clone.flip();
        return clone;
    }
}
