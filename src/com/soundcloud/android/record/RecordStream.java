package com.soundcloud.android.record;

import static com.soundcloud.android.record.SoundRecorder.TAG;

import com.soundcloud.android.audio.AudioConfig;
import com.soundcloud.android.audio.AudioFile;
import com.soundcloud.android.audio.VorbisFile;
import com.soundcloud.android.audio.WavFile;
import com.soundcloud.android.audio.WavWriter;
import com.soundcloud.android.jni.VorbisEncoder;

import android.net.Uri;
import android.util.Log;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;

public class RecordStream implements Closeable {
    private final AudioConfig config;

    private WavWriter mWavWriter;
    private VorbisEncoder mEncoder;
    private final File mEncodedFile;

    public boolean applyFades; //TODO , this

    private boolean initialised;

    private long duration;
    /* package */ long startPosition;
    /* package */ long endPosition;

    private static final int FADE_LENGTH_MS = 1000;
    private static final int FADE_EXP_CURVE = 2;

    public RecordStream(File raw, File encoded, AudioConfig cfg) {
        if (raw == null && encoded == null) throw new IllegalArgumentException("raw + encoded is null");
        if (cfg == null) throw new IllegalArgumentException("config is null");

        config = cfg;
        mWavWriter = new WavWriter(raw, cfg);
        mEncodedFile = encoded;
    }

    public int write(ByteBuffer buffer, int length) throws IOException {
        if (!initialised) {
            initialise();
        }
        mWavWriter.write(buffer, length);

        if (mEncoder != null) {
            mEncoder.write(buffer, length);
        }
        return length;
    }

    public long getDuration() {
        return duration;
    }

    public long finalizeStream() {
        if (!initialised) return -1;
        try {
            mWavWriter.finalizeStream();
            if (mEncoder != null) mEncoder.pause();
            initialised = false;
            endPosition = duration = getAudioFile().getDuration();
            return duration;

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
        return Uri.fromFile(mWavWriter.file);
    }

    public long elapsedTime() {
        return mWavWriter.getDuration();
    }

    private void initialise() throws IOException {
        if (mEncoder == null && mEncodedFile != null) {
            // initialise a new encoder object
            long start = System.currentTimeMillis();
            mEncoder = new VorbisEncoder(mEncodedFile, "a", config);
            Log.d(TAG, "init in " + (System.currentTimeMillis() - start) + " msecs");
        }
        initialised = true;
    }

    /** Playback Bounds **/
    public void resetPlaybackBounds() {
        startPosition = 0;
        endPosition = duration;
    }

    public void setStartPositionByPercent(double percent) {
        startPosition = (long) (percent * duration);
    }

    public void setEndPositionByPercent(double percent) {
        endPosition = (long) (percent * duration);
    }

    public AudioFile getAudioFile() throws IOException {
        return mEncodedFile == null ? new WavFile(mWavWriter.file) :  new VorbisFile(mEncodedFile);
    }

    public File getFile()  {
        try {
            return getAudioFile().getFile();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
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
