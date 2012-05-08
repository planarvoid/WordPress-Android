package com.soundcloud.android.record;

import static com.soundcloud.android.record.CloudRecorder.TAG;

import com.soundcloud.android.audio.AudioConfig;
import com.soundcloud.android.audio.AudioFile;
import com.soundcloud.android.audio.VorbisFile;
import com.soundcloud.android.audio.WavFile;
import com.soundcloud.android.audio.WavWriter;
import com.soundcloud.android.jni.VorbisEncoder;
import com.soundcloud.android.model.Recording;

import android.net.Uri;
import android.util.Log;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;

public class RecordStream implements Closeable {
    private WavWriter mWavWriter;
    private final AudioConfig config;
    private VorbisEncoder mEncoder;

    public final Recording recording;
    public final File encodedFile;
    public boolean applyFades; //TODO , this

    private boolean initialised;

    /* package */ long startPosition;
    /* package */ long endPosition;



    private static final int FADE_LENGTH_MS = 1000;
    private static final int FADE_EXP_CURVE = 2;

    public RecordStream(Recording r, AudioConfig cfg, boolean encode) {
        if (r == null) throw new IllegalArgumentException("recording is null");
        if (cfg == null) throw new IllegalArgumentException("config is null");

        recording = r;
        config = cfg;
        mWavWriter = new WavWriter(r.audio_path, cfg);
        encodedFile = encode ? r.encodedFilename() : null;
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
        return mWavWriter == null ? 0 : mWavWriter.getDuration();
    }

    public long finalizeStream() {
        if (!initialised) return -1;
        try {
            final long duration = mWavWriter.finalizeStream();
            if (mEncoder != null) mEncoder.pause();
            initialised = false;
            endPosition = duration;
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
        if (mEncoder == null && encodedFile != null) {
            // initialise a new encoder object
            long start = System.currentTimeMillis();
            mEncoder = new VorbisEncoder(encodedFile, "a", config);
            Log.d(TAG, "init in " + (System.currentTimeMillis() - start) + " msecs");
        }
        initialised = true;
    }

    /** Playback Bounds **/
    public void resetPlaybackBounds() {
        startPosition = 0;
        endPosition   = getDuration();
    }

    public void setStartPositionByPercent(double percent) {
        startPosition = (long) (percent * getDuration());
    }

    public void setEndPositionByPercent(double percent) {
        endPosition = (long) (percent * getDuration());
    }

    public File getFile() {
        return recording.audio_path;
    }

    public AudioFile getAudioFile() throws IOException {
        return encodedFile == null ? new WavFile(mWavWriter.file) :  new VorbisFile(encodedFile);
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
