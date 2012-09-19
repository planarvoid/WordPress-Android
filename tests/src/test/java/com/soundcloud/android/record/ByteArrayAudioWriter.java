package com.soundcloud.android.record;

import com.soundcloud.android.audio.AudioConfig;
import com.soundcloud.android.audio.AudioReader;
import com.soundcloud.android.audio.AudioWriter;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

public class ByteArrayAudioWriter implements AudioWriter {
    private ByteArrayOutputStream bos = new ByteArrayOutputStream();

    @Override
    public AudioConfig getConfig() {
        return null;
    }

    @Override
    public int write(ByteBuffer samples, int length) throws IOException {
        byte[] b = new byte[length];
        samples.rewind();
        samples.get(b, 0, length);
        bos.write(b);
        return length;
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
        return 0;
    }

    @Override
    public AudioReader getAudioReader() throws IOException {
        return null;
    }

    @Override
    public void close() throws IOException {
    }

    public byte[] getBytes() {
        return bos.toByteArray();
    }
}
