package com.soundcloud.android.audio.writer;

import com.soundcloud.android.audio.AudioConfig;
import com.soundcloud.android.audio.AudioReader;
import com.soundcloud.android.audio.AudioWriter;
import com.soundcloud.android.audio.reader.VorbisReader;
import com.soundcloud.android.jni.EncoderException;
import com.soundcloud.android.jni.VorbisEncoder;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;

public class VorbisWriter implements AudioWriter {
    private VorbisEncoder encoder;
    private final AudioConfig config;
    private final File file;

    public VorbisWriter(File out, AudioConfig config) {
        this.file = out;
        this.config = config;
    }

    @Override
    public AudioConfig getConfig() {
        return config;
    }

    @Override
    public int write(ByteBuffer samples, int length) throws IOException {
        initializeEncoder();
        final int written = encoder.write(samples, length);
        if (written < 0) {
            throw new EncoderException("Error writing", written);
        }
        return written;
    }

    @Override
    public void finalizeStream() throws IOException {
        initializeEncoder();
        encoder.pause();
    }

    @Override
    public boolean setNewPosition(long pos) throws IOException {
        initializeEncoder();
        encoder.pause();
        return encoder.startNewStream(pos  / 1000d);
    }

    @Override
    public boolean isClosed() {
        return encoder == null || (encoder != null &&
              (encoder.getState() == VorbisEncoder.STATE_PAUSED ||
               encoder.getState() == VorbisEncoder.STATE_CLOSED));
    }

    @Override
    public long getDuration() {
        return -1;
    }

    @Override
    public AudioReader getAudioFile() throws IOException {
        return encoder == null ? null : new VorbisReader(encoder.file);
    }

    @Override
    public void close() throws IOException {
        if (encoder != null){
            encoder.release();
            encoder = null;
        }
    }

    private void initializeEncoder() throws EncoderException {
        if (encoder == null) {
            encoder = new VorbisEncoder(file, "w+", config.channels, config.sampleRate, config.quality);
        }
    }
}
