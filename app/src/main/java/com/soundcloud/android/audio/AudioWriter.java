package com.soundcloud.android.audio;

import java.io.Closeable;
import java.io.IOException;
import java.nio.ByteBuffer;

public interface AudioWriter extends Closeable {

    /**
     * @return the audio config associated with this file, or null
     */
    AudioConfig getConfig();

    int write(ByteBuffer buffer, int length) throws IOException;

    /**
     * @return recorded duration in milliseconds
     * @throws IOException
     */
    long finalizeStream() throws IOException;


    /**
     * @param pos the new recording position
     * @throws IOException
     */
    void setNewPosition(long pos) throws IOException;
}
