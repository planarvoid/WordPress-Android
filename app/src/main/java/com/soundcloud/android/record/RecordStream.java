package com.soundcloud.android.record;

import com.soundcloud.android.audio.AudioConfig;
import com.soundcloud.android.audio.AudioReader;
import com.soundcloud.android.audio.AudioWriter;
import com.soundcloud.android.audio.writer.MultiAudioWriter;
import com.soundcloud.android.audio.writer.VorbisWriter;
import com.soundcloud.android.audio.writer.WavWriter;

import android.util.Log;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;

public class RecordStream implements AudioWriter {

    private AudioConfig mConfig;
    private AudioWriter delegate;

    private AmplitudeData mAmplitudeData;
    private AmplitudeData mPreRecordAmplitudeData;

    private final AmplitudeAnalyzer mAmplitudeAnalyzer;

    private float mLastAmplitude;

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
            mAmplitudeData = AmplitudeData.fromFile(amplitudeFile);
        } catch (IOException e) {
            mAmplitudeData = new AmplitudeData();
            Log.w(SoundRecorder.TAG, "error reading amplitude data", e);
        }
    }


    public void setWriters(File raw, File encoded) {
        if (encoded != null && raw == null) delegate = new VorbisWriter(encoded, mConfig);
        if (raw != null && encoded == null) delegate = new WavWriter(raw, mConfig);
        else delegate = new MultiAudioWriter(new VorbisWriter(encoded, mConfig), new WavWriter(raw, mConfig));
    }

    @Override
    public AudioConfig getConfig() {
        return delegate.getConfig();
    }

    @Override
    public int write(ByteBuffer samples, int length) throws IOException {

        mAmplitudeAnalyzer.updateCurrentMax(samples, length);
        mLastAmplitude = mAmplitudeAnalyzer.frameAmplitude();

        if (delegate == null){
            mPreRecordAmplitudeData.add(mLastAmplitude);
            return 0;
        } else {
            mAmplitudeData.add(mLastAmplitude);
            return delegate.write(samples, length);
        }
    }

    @Override
    public void finalizeStream() throws IOException {
        if (delegate != null) delegate.finalizeStream();
    }

    @Override
    public boolean setNewPosition(long pos) throws IOException {
        return delegate == null ? false : delegate.setNewPosition(pos);
    }

    @Override
    public boolean isClosed() {
        return delegate == null ? true : delegate.isClosed();
    }

    @Override
    public long getDuration() {
        return delegate == null ? -1 : delegate.getDuration();
    }

    @Override
    public AudioReader getAudioFile() throws IOException {
        return delegate == null ? null : delegate.getAudioFile();
    }

    @Override
    public void close() throws IOException {
        if (delegate != null) delegate.close();
    }

    public AmplitudeData getmAmplitudeData() {
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
}
