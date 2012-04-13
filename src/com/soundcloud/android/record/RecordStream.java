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
import java.util.Arrays;

public class RecordStream implements Closeable {
    private VorbisEncoder mEncoder;
    private RandomAccessFile mWriter;

    public final File file;
    private final AudioConfig config;

    private boolean initialised;

    protected long startPosition;
    protected long endPosition;

    private boolean applyFades = true;

    private static final int FADE_LENGTH_MS = 1000;
    private static final int FADE_EXP_CURVE = 2;

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

        return file == null ? 0 : file.length();
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

            mEncoder.pause();
            initialised = false;
            endPosition = length;
            return length;
        } catch (IOException e) {
            Log.e(TAG, "I/O exception occured while finalizing file", e);
            return -1;
        }
    }

    @Override public void close() throws IOException {
        finalizeStream();
    }

    public void release()  {
        finalizeStream();
        if (mEncoder != null) mEncoder.release();
    }

    public Uri toUri() {
        return Uri.fromFile(file);
    }

    public long elapsedTime() {
        return config.bytesToMs(length());
    }

    private void initialise() throws IOException {
        mWriter = new RandomAccessFile(file, "rw");

        if (mEncoder == null) {
            // initialise a new encoder object
            long start = System.currentTimeMillis();
            mEncoder = new VorbisEncoder(new File(file.getParentFile(), file.getName().concat(".ogg")), "a", config);
            Log.d(TAG, "init in "+(System.currentTimeMillis()-start) + " msecs");
        }

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

    /** Playback Bounds **/

    public void resetPlaybackBounds() {
        startPosition = 0;
        endPosition = file.length();
    }

    public void setStartPositionByPercent(double percent) {
        startPosition = config.validBytePosition((long) (percent * length()));
    }

    public void setEndPositionByPercent(double percent) {
        endPosition = config.validBytePosition((long) (percent * length()));
    }

    public byte[] applyMods(byte[] buffer, long bufferIndex) {
        if (applyFades){
            final long fadeSize = config.msToByte(FADE_LENGTH_MS);
            final long startFadeLastIndex = startPosition + fadeSize;
            if (bufferIndex < startFadeLastIndex){
                final int count = bufferIndex + buffer.length > startFadeLastIndex ? (int) (startFadeLastIndex - bufferIndex) : buffer.length;
                for (int i = 0; i < count; i += 2) {
                    applyVolumeChangeToBuffer(buffer, i, (double) (bufferIndex - startPosition + i) / fadeSize);
                }
            }

            final long endFadeFirstIndex = endPosition - fadeSize;
            if (bufferIndex + buffer.length > endFadeFirstIndex){
                int start = (int) (bufferIndex >= endFadeFirstIndex ? 0 : endFadeFirstIndex - bufferIndex);
                for (int i = start; i < buffer.length; i += 2){
                    applyVolumeChangeToBuffer(buffer,i, 1 - ((double)(bufferIndex + i - endFadeFirstIndex)) / fadeSize);
                }
            }
        }
        return buffer;
    }

    private void applyVolumeChangeToBuffer(byte[] buffer, int byteIndex, double volChange) {
        final short s = (short) ((short) ((0xff & buffer[byteIndex + 1]) << 8 | (0xff & buffer[byteIndex])) * Math.pow(volChange, FADE_EXP_CURVE));
        buffer[byteIndex + 1] = (byte) ((s >> 8) & 0xff);
        buffer[byteIndex] = (byte) (s & 0xff);
    }
}
