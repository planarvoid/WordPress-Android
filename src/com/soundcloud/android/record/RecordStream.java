package com.soundcloud.android.record;

import static com.soundcloud.android.record.CloudRecorder.TAG;

import com.soundcloud.android.jni.VorbisEncoder;

import android.net.Uri;
import android.util.Log;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;

public class RecordStream implements Closeable {
    private VorbisEncoder mEncoder;
    private RandomAccessFile mWriter;

    public final File file;
    private final AudioConfig config;

    private boolean initialised;

    public RecordStream(File f, AudioConfig config) {
        if (f == null) throw new IllegalArgumentException("file is null");
        if (config == null) throw new IllegalArgumentException("config is null");

        this.file = f;
        this.config = config;
    }

    public int write(ByteBuffer buffer, int length) throws IOException {
        if (!initialised) {
            initialise();
        }
        mWriter.getChannel().write(buffer);
        mEncoder.addSamples(buffer, length);
        return length;
    }

    public long length() {
        return file.length();
    }

    public long finalizeStream() {
        if (!initialised) return -1;

        try {
            final long length = mWriter.length();
            Log.d(TAG, "finalising recording file (length=" + length + ")");
            if (length == 0) {
                Log.w(TAG, "file length is zero");
            } else if (length > WaveHeader.LENGTH) {
                // fill in missing header bytes
                mWriter.seek(4);
                mWriter.writeInt(Integer.reverseBytes((int) (length - 8)));
                mWriter.seek(40);
                mWriter.writeInt(Integer.reverseBytes((int) (length - 44)));
            } else {
                Log.w(TAG, "data length is zero");
            }
            mWriter.close();
            mWriter = null;

            mEncoder.finish();
            mEncoder = null;

            initialised = false;
            return length;
        } catch (IOException e) {
            Log.e(TAG, "I/O exception occured while finalizing file", e);
            return -1;
        }
    }

    private void initialise() throws IOException {
        mWriter = new RandomAccessFile(file, "rw");
        mEncoder = new VorbisEncoder(new File(file.getParentFile(), file.getName().concat(".ogg")), "a", config);
        if (!file.exists() || mWriter.length() == 0) {
            Log.d(TAG, "creating new WAV file");
            mWriter.setLength(0); // truncate
            WaveHeader wh = config.createHeader();
            wh.write(mWriter);
        } else {
            Log.d(TAG, "appending to existing WAV file");
            mWriter.seek(mWriter.length());
        }
        initialised = true;
    }

    @Override
    public void close() throws IOException {
        finalizeStream();
    }

    public Uri toUri() {
        return Uri.fromFile(file);
    }
}
