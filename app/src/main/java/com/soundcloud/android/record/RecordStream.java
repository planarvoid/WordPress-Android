package com.soundcloud.android.record;

import static com.soundcloud.android.record.SoundRecorder.TAG;

import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.activity.settings.DevSettings;
import com.soundcloud.android.audio.AudioConfig;
import com.soundcloud.android.audio.PlaybackStream;
import com.soundcloud.android.audio.VorbisFile;
import com.soundcloud.android.audio.WavFile;
import com.soundcloud.android.audio.WavWriter;
import com.soundcloud.android.jni.VorbisEncoder;

import android.net.Uri;
import android.preference.PreferenceManager;
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

    private boolean initialised;

    /**
     * @param raw the file to hold raw data
     * @param encoded the file to be encoded (pass in null to skip encoding)
     * @param cfg the audio config to use
     */
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

    public void finalizeStream() {
        if (initialised){
            try {
                mWavWriter.finalizeStream();
                if (mEncoder != null) mEncoder.pause();
                initialised = false;
            } catch (IOException e) {
                Log.e(TAG, "I/O exception occured while finalizing file", e);
            }
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

    /**
     * @return recording time in ms
     */
    public long elapsedTime() {
        return mWavWriter.getDuration();
    }

    private void initialise() throws IOException {
        if (shouldEncode() && mEncoder == null && mEncodedFile != null) {
            // initialise a new encoder object
            long start = System.currentTimeMillis();
            mEncoder = new VorbisEncoder(mEncodedFile, "a", config);
            Log.d(TAG, "init in " + (System.currentTimeMillis() - start) + " msecs");
        }
        initialised = true;
    }

    public PlaybackStream getPlaybackStream() throws IOException {
        return new PlaybackStream(mEncodedFile == null || !mEncodedFile.exists() ? new WavFile(mWavWriter.file) : new VorbisFile(mEncodedFile));
    }

    public boolean shouldEncode() {
        return "compressed".equals(PreferenceManager.getDefaultSharedPreferences(SoundCloudApplication.instance)
                .getString(DevSettings.DEV_RECORDING_TYPE, "compressed"));
    }

    public void setNextRecordingPosition(long pos) throws IOException {
        mWavWriter.setNewPosition(pos);
        if (mEncoder != null) {
            mEncoder.startNewStream(pos / 1000d);
        }
    }
}
