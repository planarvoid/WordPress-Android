package com.soundcloud.android.record;

import com.soundcloud.android.audio.AudioConfig;
import com.soundcloud.android.audio.AudioReader;
import com.soundcloud.android.audio.AudioWriter;
import com.soundcloud.android.audio.writer.MultiAudioWriter;
import com.soundcloud.android.audio.writer.VorbisWriter;
import com.soundcloud.android.audio.writer.WavWriter;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;

public class RecordStream implements AudioWriter {

    private AudioWriter delegate;

    /**
     * @param raw the file to hold raw data
     * @param encoded the file to be encoded (pass in null to skip encoding)
     * @param cfg the audio config to use
     */
    public RecordStream(File raw, File encoded, AudioConfig cfg)  {
        if (raw == null && encoded == null) throw new IllegalArgumentException("raw + encoded is null");
        if (cfg == null) throw new IllegalArgumentException("config is null");

        if (encoded != null && raw == null) delegate = new VorbisWriter(encoded, cfg);
        if (raw != null && encoded == null) delegate = new WavWriter(raw, cfg);
        else delegate = new MultiAudioWriter(new VorbisWriter(encoded, cfg), new WavWriter(raw, cfg));
    }

    @Override
    public AudioConfig getConfig() {
        return delegate.getConfig();
    }

    @Override
    public int write(ByteBuffer samples, int length) throws IOException {
        return delegate.write(samples, length);
    }

    @Override
    public void finalizeStream() throws IOException {
        delegate.finalizeStream();
    }

    @Override
    public boolean setNewPosition(long pos) throws IOException {
        return delegate.setNewPosition(pos);
    }

    @Override
    public boolean isClosed() {
        return delegate.isClosed();
    }

    @Override
    public long getDuration() {
        return delegate.getDuration();
    }

    @Override
    public AudioReader getAudioFile() throws IOException {
        return delegate.getAudioFile();
    }

    @Override
    public void close() throws IOException {
        delegate.close();
    }
}
