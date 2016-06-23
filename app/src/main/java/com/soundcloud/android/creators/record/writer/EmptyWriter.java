package com.soundcloud.android.creators.record.writer;

import com.soundcloud.android.creators.record.AudioConfig;
import com.soundcloud.android.creators.record.AudioReader;
import com.soundcloud.android.creators.record.AudioWriter;

import java.io.IOException;
import java.nio.ByteBuffer;

public class EmptyWriter implements AudioWriter {
    private final AudioConfig config;

    public EmptyWriter(AudioConfig cfg) {
        this.config = cfg;
    }

    @Override
    public AudioConfig getConfig() {
        return config;
    }

    @Override
    public int write(ByteBuffer samples, int length) throws IOException {
        return -1;
    }

    @Override
    public void finalizeStream() throws IOException {
    }

    @Override
    public boolean setNewPosition(long pos) throws IOException {
        return false;
    }

    @Override
    public boolean isClosed() {
        return false;
    }

    @Override
    public long getDuration() {
        return -1;
    }

    @Override
    public AudioReader getAudioReader() throws IOException {
        return AudioReader.EMPTY;
    }

    @Override
    public void close() throws IOException {
    }
}
