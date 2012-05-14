package com.soundcloud.android.audio;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;

public interface AudioFile extends Closeable {
    int EOF = -1;

    /**
     * @return the audio config associated with this file, or null
     */
    AudioConfig getConfig();

    /**
     * @param pos the position in milliseconds
     * @throws IOException if seek failed
     */
    void seek(long pos) throws IOException;

    /**
     * @return the duration in milliseconds
     */
    long getDuration();


    /**
     * @return the current position in milliseconds
     */
    long getPosition();

    /**
     * Reads up to length bytes audiodata into the buffer, starting from the current position
     * @param buffer the bytebuffer to read bytes into
     * @param length the number of bytes to read (should be buffer.capacity())
     * @return the number of read bytes, or -1 for EOF
     * @throws IOException
     */
    int read(ByteBuffer buffer, int length) throws IOException;


    /**
     * @return the underlying file or null
     */
    File getFile();
}
