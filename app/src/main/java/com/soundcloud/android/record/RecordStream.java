package com.soundcloud.android.record;

import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.audio.AudioConfig;
import com.soundcloud.android.audio.AudioReader;
import com.soundcloud.android.audio.AudioWriter;
import com.soundcloud.android.audio.PlaybackStream;
import com.soundcloud.android.audio.writer.EmptyWriter;
import com.soundcloud.android.audio.writer.MultiAudioWriter;
import com.soundcloud.android.audio.writer.VorbisWriter;
import com.soundcloud.android.audio.writer.WavWriter;
import com.soundcloud.android.utils.BufferUtils;
import org.jetbrains.annotations.NotNull;

import android.util.Log;

import java.io.File;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.nio.ByteBuffer;

public class RecordStream implements AudioWriter {
    private @NotNull final AudioConfig mConfig;
    private @NotNull AudioWriter writer;
    private @NotNull AmplitudeData mAmplitudeData;
    private @NotNull final AmplitudeData mPreRecordAmplitudeData;
    private @NotNull final AmplitudeAnalyzer mAmplitudeAnalyzer;

    private float mLastAmplitude;

    public interface onAmplitudeGenerationListener {
        void onGenerationFinished(boolean success);
    }

    /**
     * @param cfg the audio config to use
     */
    public RecordStream(AudioConfig cfg) {
        if (cfg == null) throw new IllegalArgumentException("config is null");
        mConfig = cfg;
        mAmplitudeAnalyzer = new AmplitudeAnalyzer(cfg);
        mAmplitudeAnalyzer.frameAmplitude();

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

    public boolean hasValidAmplitudeData(){
        final long requiredSize = (int) (SoundRecorder.PIXELS_PER_SECOND * SoundCloudApplication.instance.getResources().getDisplayMetrics().density) * getDuration();
        return mAmplitudeData.size() >= (int) requiredSize / 1000;
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

        Log.i(SoundRecorder.TAG,"Regenerating amplitude file to " + (outFile == null ? "<null>" : outFile.getPath()));
        final WeakReference<onAmplitudeGenerationListener> listenerWeakReference = new WeakReference<onAmplitudeGenerationListener>(onAmplitudeListener);

        try {
            final long start = System.currentTimeMillis();
            mAmplitudeData = new AmplitudeData();
            final int bufferSize = mConfig.getvalidBufferSizeForValueRate((int) (SoundRecorder.PIXELS_PER_SECOND * SoundCloudApplication.instance.getResources().getDisplayMetrics().density));
            ByteBuffer buffer = BufferUtils.allocateAudioBuffer(bufferSize);

            final PlaybackStream playbackStream = new PlaybackStream(getAudioFile());
            playbackStream.initializePlayback();
            int n;
            while ((n = playbackStream.readForPlayback(buffer, bufferSize)) > -1) {
                updateAmplitude(buffer, n);
                mAmplitudeData.add(mLastAmplitude);
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
        }
    }

    public void setWriter(@NotNull AudioWriter writer) {
        this.writer = writer;
    }

    public void setWriters(File raw, File encoded) {
        AudioWriter w;
        if (encoded != null && raw == null)      w = new VorbisWriter(encoded, mConfig);
        else if (raw != null && encoded == null) w = new WavWriter(raw, mConfig);
        else w = new MultiAudioWriter(new VorbisWriter(encoded, mConfig), new WavWriter(raw, mConfig));
        setWriter(w);
    }

    @Override
    public AudioConfig getConfig() {
        return writer.getConfig();
    }

    @Override
    public int write(ByteBuffer samples, int length) throws IOException {
        updateAmplitude(samples, length);

        if (writer instanceof EmptyWriter) {
            mPreRecordAmplitudeData.add(mLastAmplitude);
            return -1;
        } else {
            mAmplitudeData.add(mLastAmplitude);
            return writer.write(samples, length);
        }
    }

    private void updateAmplitude(ByteBuffer samples, int length) {
        mAmplitudeAnalyzer.updateCurrentMax(samples, length);
        samples.rewind();
        mLastAmplitude = mAmplitudeAnalyzer.frameAmplitude();
    }

    @Override
    public void finalizeStream() throws IOException {
        writer.finalizeStream();
    }

    @Override
    public boolean setNewPosition(long pos) throws IOException {
        return writer.setNewPosition(pos);
    }

    @Override
    public boolean isClosed() {
        return writer.isClosed();
    }

    @Override
    public long getDuration() {
        return writer.getDuration();
    }

    @Override
    public AudioReader getAudioFile() throws IOException {
        return writer.getAudioFile();
    }

    @Override
    public void close() throws IOException {
        writer.close();
    }

    public AmplitudeData getAmplitudeData() {
        return mAmplitudeData;
    }

    public AmplitudeData getPreRecordAmplitudeData() {
        return mPreRecordAmplitudeData;
    }

    public boolean truncate(long pos, int valuesPerSecond) throws IOException {
        mAmplitudeData.truncate((int) ((pos / 1000d) * valuesPerSecond));
        return setNewPosition(pos);
    }

    public void finalizeStream(File amplitudeFile) throws IOException {
        finalizeStream();
        mAmplitudeData.store(amplitudeFile);
    }

    public float getLastAmplitude() {
        return mLastAmplitude;
    }

    public void reset() {
        try {
            close();
        } catch (IOException ignored) {
        }
        writer = new EmptyWriter(mConfig);
        mAmplitudeData.clear();
        mPreRecordAmplitudeData.clear();
        mLastAmplitude = 0f;
    }
}
