package com.soundcloud.android.audio;

import com.soundcloud.android.utils.IOUtils;
import org.jetbrains.annotations.Nullable;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;

public abstract class AudioFile implements Closeable {
    int EOF = -1;

    /**
     * @return the audio config associated with this file, or null
     */
    public abstract AudioConfig getConfig();

    /**
     * @param pos the position in milliseconds
     * @throws IOException if seek failed
     */
    public abstract void seek(long pos) throws IOException;

    /**
     * @return the duration in milliseconds
     */
    public abstract long getDuration();


    /**
     * @return the current position in milliseconds
     */
    public abstract long getPosition();

    /**
     * Reads up to length bytes audiodata into the buffer, starting from the current position
     * @param buffer the bytebuffer to read bytes into
     * @param length the number of bytes to read (should be buffer.capacity())
     * @return the number of read bytes, or -1 for EOF
     * @throws IOException
     */
    public abstract int read(ByteBuffer buffer, int length) throws IOException;


    /**
     * @return the underlying file or null
     */
    public abstract File getFile();

    public abstract void reopen();

    public @Nullable static AudioFile guess(File file) throws IOException {
        if (IOUtils.extension(file).equals(WavFile.EXTENSION)) {
            return new WavFile(file);
        } else if (IOUtils.extension(file).equals(VorbisFile.EXTENSION)) {
            return new VorbisFile(file);
        } else {
            return null;
        }
    }
}
