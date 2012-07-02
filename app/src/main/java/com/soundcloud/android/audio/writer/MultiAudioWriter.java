package com.soundcloud.android.audio.writer;

import com.soundcloud.android.audio.AudioConfig;
import com.soundcloud.android.audio.AudioReader;
import com.soundcloud.android.audio.AudioWriter;

import java.io.IOException;
import java.nio.ByteBuffer;

public class MultiAudioWriter implements AudioWriter {
    private final AudioWriter[] writers;
    private AudioConfig config;

    public MultiAudioWriter(AudioWriter... writers) {
        this.writers = writers;
        for (AudioWriter w : writers) {
            if (config == null) {
                config = w.getConfig();
            } else if (config != w.getConfig()) {
                throw new IllegalArgumentException("mismatch in configurations:" + config + "/" + w.getConfig());
            }
        }
    }

    @Override
    public AudioConfig getConfig() {
        return config;
    }

    @Override
    public int write(ByteBuffer samples, int length) throws IOException {
        int[] written = new int[writers.length];
        for (int i = 0; i < writers.length; i++) {
            written[i] = writers[i].write(samples, length);
        }
        return written[0];
    }

    @Override
    public void finalizeStream() throws IOException {
        for (AudioWriter w : writers) {
            w.finalizeStream();
        }
    }

    @Override
    public boolean setNewPosition(long pos) throws IOException {
        for (AudioWriter w : writers) {
            boolean success = w.setNewPosition(pos);
            if (!success) return false;
        }
        return true;
    }

    @Override
    public boolean isClosed() {
        boolean closed = false;
        for (AudioWriter w : writers) {
            closed = w.isClosed();
        }
        return closed;
    }

    @Override
    public long getDuration() {
        for (AudioWriter w : writers) {
            long d = w.getDuration();
            if (d != -1) return d;
        }
        return -1;
    }

    @Override
    public AudioReader getAudioFile() throws IOException {
        return writers[0].getAudioFile();
    }

    @Override
    public void close() throws IOException {
        for (AudioWriter w : writers) w.close();
    }

}
