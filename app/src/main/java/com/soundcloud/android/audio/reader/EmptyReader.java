package com.soundcloud.android.audio.reader;

import com.soundcloud.android.audio.AudioConfig;
import com.soundcloud.android.audio.AudioReader;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;

public class EmptyReader extends AudioReader {

    @Override
    public AudioConfig getConfig() {
        return AudioConfig.PCM16_44100_1;
    }

    @Override
    public void seek(long pos) throws IOException {
    }

    @Override
    public long getDuration() {
        return -1;
    }

    @Override
    public long getPosition() {
        return -1;
    }

    @Override
    public int read(ByteBuffer buffer, int length) throws IOException {
        return -1;
    }

    @Override
    public File getFile() {
        return null;
    }

    @Override
    public void reopen() throws IOException {
    }

    @Override
    public void close() throws IOException {
    }
}
