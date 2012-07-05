package com.soundcloud.android.audio.writer;

import com.soundcloud.android.audio.AudioConfig;
import com.soundcloud.android.audio.AudioReader;
import com.soundcloud.android.audio.AudioWriter;

import java.io.IOException;
import java.nio.ByteBuffer;

public class EmptyWriter implements AudioWriter {
    private final AudioConfig config;

    public EmptyWriter(AudioConfig cfg) {
        this.config =  cfg;
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
    public AudioReader getAudioFile() throws IOException {
        return null;
    }

    @Override
    public void close() throws IOException {
    }
}
