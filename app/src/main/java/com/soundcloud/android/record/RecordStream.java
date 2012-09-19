package com.soundcloud.android.record;

import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.audio.AudioConfig;
import com.soundcloud.android.audio.AudioReader;
import com.soundcloud.android.audio.AudioWriter;
import com.soundcloud.android.audio.PlaybackStream;
import com.soundcloud.android.audio.filter.FadeFilter;
import com.soundcloud.android.audio.writer.EmptyWriter;
import com.soundcloud.android.audio.writer.MultiAudioWriter;
import com.soundcloud.android.audio.writer.VorbisWriter;
import com.soundcloud.android.audio.writer.WavWriter;
import com.soundcloud.android.jni.NativeAmplitudeAnalyzer;
import com.soundcloud.android.utils.BufferUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import android.util.Log;

import java.io.File;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.nio.ByteBuffer;

public class RecordStream  {
    private @NotNull final AudioConfig mConfig;
    private @NotNull AudioWriter writer;
    private @NotNull AmplitudeData mAmplitudeData;
    private @NotNull final AmplitudeData mPreRecordAmplitudeData;
    private @NotNull final AmplitudeAnalyzer mAmplitudeAnalyzer;
    private float mLastAmplitude;
    private boolean mWasTruncated, mWasFinalized;

    public interface onAmplitudeGenerationListener {
        void onGenerationFinished(boolean success);
    }

    /**
     * @param cfg the audio config to use
     */
    public RecordStream(AudioConfig cfg) {
        if (cfg == null) throw new IllegalArgumentException("config is null");
        mConfig = cfg;
        mAmplitudeAnalyzer = new NativeAmplitudeAnalyzer(cfg);

        mAmplitudeData = new AmplitudeData();
        mPreRecordAmplitudeData = new AmplitudeData();
        writer = new EmptyWriter(cfg);
    }

    /**
     * @param cfg the audio config to use
     * @param raw     the file to hold raw data
     * @param encoded the file to be encoded (pass in null to skip encoding)
     * @param amplitudeFile previous amplitude data
     */
    public RecordStream(AudioConfig cfg, File raw, File encoded, File amplitudeFile) {
        this(cfg);

        setWriters(raw, encoded);
        try {
            if (amplitudeFile != null && amplitudeFile.exists()){
                mAmplitudeData = AmplitudeData.fromFile(amplitudeFile);
            } else {
                Log.d(SoundRecorder.TAG, "Amplitude file not found at " + (amplitudeFile == null ? "<null>" : amplitudeFile.getPath()));
            }
        } catch (IOException e) {
            Log.w(SoundRecorder.TAG, "error reading amplitude data", e);
        }
    }

    public boolean hasValidAmplitudeData() {
        // we may have never used the encoder in which case getDuration() is 0,
        // so make sure to use the audioreader duration
        long playDuration = 0;
        try {
            playDuration = writer.getAudioReader().getDuration();
        } catch (IOException ignored) {
        }
        final long requiredSize = (long) (((int) (SoundRecorder.PIXELS_PER_SECOND *
                        SoundCloudApplication.instance.getResources().getDisplayMetrics().density) * playDuration)
                * .95); // 5 percent tolerance

        int delta = (int) ((requiredSize / 1000d) -  mAmplitudeData.size());

        return Math.abs(delta) <= 5;
    }

    public void regenerateAmplitudeDataAsync(final File outFile, final onAmplitudeGenerationListener onAmplitudeListener) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                regenerateAmplitudeData(outFile, onAmplitudeListener);
            }
        }).start();
    }

    /*package*/ void regenerateAmplitudeData(File outFile, onAmplitudeGenerationListener onAmplitudeListener) {

        Log.d(SoundRecorder.TAG,"Regenerating amplitude file to " + (outFile == null ? "<null>" : outFile.getPath()));
        final WeakReference<onAmplitudeGenerationListener> listenerWeakReference = new WeakReference<onAmplitudeGenerationListener>(onAmplitudeListener);

        try {
            final long start = System.currentTimeMillis();
            mAmplitudeData = new AmplitudeData();
            final int bufferSize = mConfig.getvalidBufferSizeForValueRate((int) (SoundRecorder.PIXELS_PER_SECOND * SoundCloudApplication.instance.getResources().getDisplayMetrics().density));
            ByteBuffer buffer = BufferUtils.allocateAudioBuffer(bufferSize);

            final PlaybackStream playbackStream = new PlaybackStream(getAudioReader());
            playbackStream.initializePlayback();
            int n;
            while ((n = playbackStream.readForPlayback(buffer, bufferSize)) > -1) {
                mAmplitudeData.add(mAmplitudeAnalyzer.frameAmplitude(buffer, n));
                buffer.clear();
            }
            playbackStream.close();

            if (outFile != null) mAmplitudeData.store(outFile);
            Log.d(SoundRecorder.TAG, "Amplitude file regenerated in " + (System.currentTimeMillis() - start) + " milliseconds");
            onAmplitudeGenerationListener listener2 = listenerWeakReference.get();
            if (listener2 != null) listener2.onGenerationFinished(true);

        } catch (IOException e) {
            mAmplitudeData = new AmplitudeData();
            Log.w(SoundRecorder.TAG, "error regenerating amplitude data", e);
            onAmplitudeGenerationListener listener2 = listenerWeakReference.get();
            if (listener2 != null) listener2.onGenerationFinished(false);
        }
    }

    public void setWriter(@NotNull AudioWriter writer) {
        this.writer = writer;
    }

    public void setWriters(@Nullable File raw, @Nullable File encoded) {
        AudioWriter w;

        if (raw == null && encoded == null)      w = new EmptyWriter(mConfig);
        else if (encoded != null && raw == null) w = new VorbisWriter(encoded, mConfig);
        else if (encoded == null) w = new WavWriter(raw, mConfig);
        else w = new MultiAudioWriter(new VorbisWriter(encoded, mConfig), new WavWriter(raw, mConfig));
        setWriter(w);
    }

    public int write(ByteBuffer samples, int length) throws IOException {
        samples.limit(length);
        mLastAmplitude = mAmplitudeAnalyzer.frameAmplitude(samples, length);
        if (writer instanceof EmptyWriter) {
            mPreRecordAmplitudeData.add(mLastAmplitude);
            return -1;
        } else {
            mAmplitudeData.add(mLastAmplitude);
            if (mWasFinalized) {
                // apply short fade at the beginning of new recording session
                new FadeFilter(FadeFilter.FADE_TYPE_BEGINNING, length).apply(samples, 0, length);
                mWasFinalized = false;
            }
            return writer.write(samples, length);
        }
    }

    public long getDuration() {
        return writer.getDuration();
    }

    public AudioReader getAudioReader() throws IOException {
        return writer.getAudioReader();
    }

    public AmplitudeData getAmplitudeData() {
        return mAmplitudeData;
    }

    public AmplitudeData getPreRecordAmplitudeData() {
        return mPreRecordAmplitudeData;
    }

    public boolean truncate(long pos, int valuesPerSecond) throws IOException {
        mAmplitudeData.truncate((int) ((pos / 1000d) * valuesPerSecond));
        return writer.setNewPosition(pos);
    }

    public void finalizeStream(File amplitudeFile) throws IOException {
        writeFadeOut(100);
        mWasFinalized = true;
        writer.finalizeStream();
        if (amplitudeFile != null) {
            mAmplitudeData.store(amplitudeFile);
        }
    }

    private void writeFadeOut(long msecs) throws IOException {
        double last = mAmplitudeAnalyzer.getLastValue();
        if (last != 0) {
            int bytesToWrite = (int) mConfig.msToByte(msecs);
            ByteBuffer buffer = BufferUtils.allocateAudioBuffer(bytesToWrite + 2);
            final double slope = last / (bytesToWrite / 2);
            for (int i = 0; i< bytesToWrite; i+= 2) {
                last -= slope;
                buffer.putShort((short) last);
            }
            buffer.putShort((short) 0);
            write(buffer, bytesToWrite);
        }
    }


    public void reset() {
        try {
            writer.close();
        } catch (IOException ignored) {
        }
        writer = new EmptyWriter(mConfig);
        mAmplitudeData.clear();
        mPreRecordAmplitudeData.clear();
        mLastAmplitude = 0f;
    }

    public float getLastAmplitude() {
        return mLastAmplitude;
    }

}
