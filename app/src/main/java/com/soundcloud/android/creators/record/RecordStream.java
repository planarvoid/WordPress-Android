package com.soundcloud.android.creators.record;

import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.creators.record.filter.FadeFilter;
import com.soundcloud.android.creators.record.jni.NativeAmplitudeAnalyzer;
import com.soundcloud.android.creators.record.writer.EmptyWriter;
import com.soundcloud.android.creators.record.writer.MultiAudioWriter;
import com.soundcloud.android.creators.record.writer.VorbisWriter;
import com.soundcloud.android.creators.record.writer.WavWriter;
import com.soundcloud.android.utils.BufferUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import android.content.res.Resources;
import android.util.Log;

import java.io.File;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.nio.ByteBuffer;

public class RecordStream {
    private @NotNull final AudioConfig config;
    private @NotNull AudioWriter writer;
    private @NotNull AmplitudeData amplitudeData;
    private @NotNull final AmplitudeData preRecordAmplitudeData;
    private @NotNull final AmplitudeAnalyzer amplitudeAnalyzer;
    private float lastAmplitude;
    private boolean wasFinalized;

    /**
     * @param cfg the audio config to use
     */
    public RecordStream(AudioConfig cfg) {
        if (cfg == null) {
            throw new IllegalArgumentException("config is null");
        }
        config = cfg;
        amplitudeAnalyzer = new NativeAmplitudeAnalyzer(cfg);

        amplitudeData = new AmplitudeData();
        preRecordAmplitudeData = new AmplitudeData();
        writer = new EmptyWriter(cfg);
    }

    /**
     * @param cfg           the audio config to use
     * @param raw           the file to hold raw data
     * @param encoded       the file to be encoded (pass in null to skip encoding)
     * @param amplitudeFile previous amplitude data
     */
    public RecordStream(AudioConfig cfg, File raw, File encoded, File amplitudeFile) {
        this(cfg);

        setWriters(raw, encoded);
        try {
            if (amplitudeFile != null && amplitudeFile.exists()) {
                amplitudeData = AmplitudeData.fromFile(amplitudeFile);
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


        Log.i("asdf","Required size vs amp size " + requiredSize + " vs " + amplitudeData.size());
        int delta = (int) ((requiredSize / 1000d) - amplitudeData.size());

        return Math.abs(delta) <= 5;
    }

    private int getBufferSize(Resources resources) {
        final int valuesPerSecond = (int) (SoundRecorder.PIXELS_PER_SECOND * resources.getDisplayMetrics().density);
        return (int) config.validBytePosition((long) (config.bytesPerSecond / valuesPerSecond));
    }

    public void setWriter(@NotNull AudioWriter writer) {
        this.writer = writer;
    }

    public final void setWriters(@Nullable File raw, @Nullable File encoded) {
        AudioWriter w;

        if (raw == null && encoded == null) {
            w = new EmptyWriter(config);
        } else if (encoded != null && raw == null) {
            w = new VorbisWriter(encoded, config);
        } else if (encoded == null) {
            w = new WavWriter(raw, config);
        } else {
            w = new MultiAudioWriter(new VorbisWriter(encoded, config), new WavWriter(raw, config));
        }

        setWriter(w);
    }

    public int write(ByteBuffer samples, int length) throws IOException {
        samples.limit(length);
        lastAmplitude = amplitudeAnalyzer.frameAmplitude(samples, length);
        if (writer instanceof EmptyWriter) {
            preRecordAmplitudeData.add(lastAmplitude);
            return -1;
        } else {
            amplitudeData.add(lastAmplitude);
            if (wasFinalized) {
                // apply short fade at the beginning of new recording session
                new FadeFilter(FadeFilter.FADE_TYPE_BEGINNING, length).apply(samples, 0, length);
                wasFinalized = false;
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
        return amplitudeData;
    }

    public AmplitudeData getPreRecordAmplitudeData() {
        return preRecordAmplitudeData;
    }

    public boolean truncate(long pos, int valuesPerSecond) throws IOException {
        amplitudeData.truncate((int) ((pos / 1000d) * valuesPerSecond));
        return writer.setNewPosition(pos);
    }

    public void finalizeStream(File amplitudeFile) throws IOException {
        //writeFadeOut(100);
        // I removed this because it screws up the animation, as we don't broadcast out the added amplitudes
        // This whole thing is a house of cards

        wasFinalized = true;
        writer.finalizeStream();
        if (amplitudeFile != null) {
            amplitudeData.store(amplitudeFile);
        }
    }

    @SuppressWarnings("unused")
    private void writeFadeOut(long msecs) throws IOException {
        double last = amplitudeAnalyzer.getLastValue();
        if (last != 0) {
            final int bufferSize = getBufferSize(SoundCloudApplication.instance.getResources());
            int bytesToWrite = (int) config.msToByte(msecs);
            ByteBuffer buffer = BufferUtils.allocateAudioBuffer(bytesToWrite + 2);
            ByteBuffer amplitudeBuffer = BufferUtils.allocateAudioBuffer(bufferSize);

            final double slope = last / (bytesToWrite / 2);
            for (int i = 0; i < bytesToWrite; i += 2) {
                last -= slope;
                buffer.putShort((short) last);
                amplitudeBuffer.putShort((short) last);
                if (i % bufferSize == 0){
                    final float sample = amplitudeAnalyzer.frameAmplitude(amplitudeBuffer, bufferSize);
                    amplitudeData.add(sample);
                    amplitudeBuffer.rewind();
                }
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
        writer = new EmptyWriter(config);
        amplitudeData.clear();
        preRecordAmplitudeData.clear();
        lastAmplitude = 0f;
    }

    public float getLastAmplitude() {
        return lastAmplitude;
    }

}
