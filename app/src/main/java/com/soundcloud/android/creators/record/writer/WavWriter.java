package com.soundcloud.android.creators.record.writer;

import com.soundcloud.android.creators.record.AudioConfig;
import com.soundcloud.android.creators.record.AudioReader;
import com.soundcloud.android.creators.record.AudioWriter;
import com.soundcloud.android.creators.record.WavHeader;
import com.soundcloud.android.creators.record.reader.WavReader;
import org.jetbrains.annotations.Nullable;

import android.util.Log;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;

public class WavWriter implements AudioWriter {
    private static final String TAG = WavWriter.class.getSimpleName();

    public final File file;
    public final AudioConfig config;

    private @Nullable RandomAccessFile writer;

    public WavWriter(File file, AudioConfig config) {
        this.file = file;
        this.config = config;
    }

    private RandomAccessFile initializeWriter() throws IOException {
        RandomAccessFile writer = new RandomAccessFile(file, "rw");
        if (!file.exists() || writer.length() == 0) {
            Log.d(TAG, "creating new WAV file (" + file.getAbsolutePath() + ")");
            writer.setLength(0); // truncate
            WavHeader wh = config.createHeader();
            wh.write(writer);
        } else {
            long seekTo = writer.length();
            Log.d(TAG, "appending to existing WAV file (" + file.getAbsolutePath() + ") at " + seekTo);
            writer.seek(seekTo);
        }
        return writer;
    }

    @Override
    public AudioConfig getConfig() {
        return config;
    }

    public int write(ByteBuffer samples, int length) throws IOException {
        if (writer == null) {
            // initialize writer lazily so this can be offloaded easily to a thread
            writer = initializeWriter();
        }
        return writer.getChannel().write(samples);
    }

    public void finalizeStream() throws IOException {
        if (writer != null) {
            final long fileLength = writer.length();
            Log.d(TAG, "finalising recording file (length=" + fileLength + ")");
            WavHeader.fixLength(writer);
            writer.close();
            writer = null;
        }
    }

    @Override
    public boolean setNewPosition(long pos) throws IOException {
        // truncate file to new length
        if (pos >= 0) {
            long newPos = config.msToByte(pos) + WavHeader.LENGTH;
            if (writer == null) {
                writer = initializeWriter();
            }
            if (newPos < writer.length()) {

                Log.d(TAG, "setting new pos " + newPos);
                writer.setLength(newPos);
                WavHeader.fixLength(writer);
                writer.seek(newPos);
                return true;
            } else {
                return false;
            }
        } else {
            return false;
        }
    }

    @Override
    public boolean isClosed() {
        return writer == null;
    }

    /**
     * @return the duration, in ms
     */
    public long getDuration() {
        return config.bytesToMs(file.length() - WavHeader.LENGTH);
    }

    @Override
    public AudioReader getAudioReader() throws IOException {
        return new WavReader(file);
    }

    @Override
    public void close() throws IOException {
        if (!isClosed()) {
            finalizeStream();
        }
    }
}
