package com.soundcloud.android.audio;

import java.io.Closeable;
import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * Interface for writing audio to a file, see {@link AudioReader}.
 */
public interface AudioWriter extends Closeable {

    /**
     * @return the audio config associated with this file, or null
     */
    AudioConfig getConfig();

    /**
     * @param samples the samples to write
     * @param length how many samples to write from the buffer (needed because direct buffers don't have length
     *               information)
     * @return the number of bytes actually written
     * @throws IOException
     */
    int write(ByteBuffer samples, int length) throws IOException;

    /**
     * @return recorded duration in milliseconds
     * @throws IOException
     */
    void finalizeStream() throws IOException;

    /**
     * @param pos the new recording position
     * @return success
     * @throws IOException
     */
    boolean setNewPosition(long pos) throws IOException;

    boolean isClosed();

    /**
     * @return the recorded duration, in ms, or -1 if not known
     */
    long getDuration();

    /**
     * @return audiofile which can be used to read this stream
     * @throws IOException
     */
    AudioReader getAudioFile() throws IOException;
}
