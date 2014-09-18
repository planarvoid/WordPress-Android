package com.soundcloud.android.creators.record;

import com.soundcloud.android.creators.record.reader.EmptyReader;
import com.soundcloud.android.creators.record.reader.VorbisReader;
import com.soundcloud.android.creators.record.reader.WavReader;
import com.soundcloud.android.utils.IOUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * Abstraction of audio reading, counterpart of {@link AudioWriter}.
 */
public abstract class AudioReader implements Closeable {
    public static final AudioReader EMPTY = new EmptyReader();

    public static final int EOF = -1;

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
     *
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

    public abstract void reopen() throws IOException;


    public static
    @NotNull
    AudioReader guessMultiple(@Nullable File... files) throws IOException {
        if (files == null) {
            return AudioReader.EMPTY;
        }
        for (File f : files) {
            AudioReader reader = guess(f);
            if (!(reader instanceof EmptyReader)) {
                return reader;
            }
        }
        return AudioReader.EMPTY;
    }

    public static AudioReader guess(@Nullable File file) throws IOException {
        if (file == null || !file.exists()) {
            return AudioReader.EMPTY;
        }

        final String ext = IOUtils.extension(file);
        if (ext == null) {
            return AudioReader.EMPTY;
        } else if (ext.equals(WavReader.EXTENSION)) {
            return new WavReader(file);
        } else if (ext.equals(VorbisReader.EXTENSION)) {
            return new VorbisReader(file);
        } else {
            return AudioReader.EMPTY;
        }
    }
}
