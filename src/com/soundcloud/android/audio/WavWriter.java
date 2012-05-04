package com.soundcloud.android.audio;

import android.util.Log;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;

public class WavWriter implements AudioWriter {
    private static final String TAG = WavWriter.class.getSimpleName();

    public final File file;
    public final AudioConfig config;

    private RandomAccessFile mWriter;

    public WavWriter(File file, AudioConfig config) {
        this.file = file;
        this.config = config;
    }

    private RandomAccessFile initializeWriter(File file, AudioConfig config) throws IOException {
        RandomAccessFile writer = new RandomAccessFile(file, "rw");
        if (!file.exists() || writer.length() == 0) {
            Log.d(TAG, "creating new WAV file");
            writer.setLength(0); // truncate
            WavHeader wh = config.createHeader();
            wh.write(writer);
        } else {
            Log.d(TAG, "appending to existing WAV file");
            writer.seek(writer.length());
        }
        return writer;
    }

    @Override public AudioConfig getConfig() {
        return config;
    }

    public int write(ByteBuffer buffer, int length) throws IOException {
        if (mWriter == null) {
            mWriter = initializeWriter(file, config);
        }
        return mWriter.getChannel().write(buffer);
    }

    public long finalizeStream() throws IOException {
        final long fileLength = mWriter.length();
        Log.d(TAG, "finalising recording file (length=" + fileLength + ")");
        if (fileLength == 0) {
            Log.w(TAG, "file length is zero");
        } else if (fileLength > WavHeader.LENGTH) {
            // remaining bytes
            mWriter.seek(4);
            mWriter.writeInt(Integer.reverseBytes((int) (fileLength - 8)));
            // total bytes
            mWriter.seek(WavHeader.LENGTH - 4);
            mWriter.writeInt(Integer.reverseBytes((int) (fileLength - WavHeader.LENGTH)));
        } else {
            Log.w(TAG, "data length is zero");
        }
        mWriter.close();
        mWriter = null;

        return getDuration();
    }

    /**
     * @return the duration, in ms
     */
    public long getDuration() {
        return (1000l * (file.length() - WavHeader.LENGTH)) / (config.sampleRate * config.channels * (config.bitsPerSample / 8));
    }

    @Override
    public void close() throws IOException {
        finalizeStream();
    }
}
