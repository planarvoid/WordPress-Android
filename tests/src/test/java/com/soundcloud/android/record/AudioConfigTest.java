package com.soundcloud.android.record;

import static com.soundcloud.android.Expect.expect;

import com.soundcloud.android.audio.AudioConfig;
import org.junit.Test;

import android.media.AudioFormat;

public class AudioConfigTest {

    @Test
    public void shouldCalculateStartPosition() throws Exception {
        expect(AudioConfig.PCM16_44100_1.validBytePosition(0)).toEqual(0l);
        expect(AudioConfig.PCM16_44100_1.validBytePosition(1)).toEqual(0l);
        expect(AudioConfig.PCM16_44100_1.validBytePosition(2)).toEqual(2l);
        expect(AudioConfig.PCM16_44100_1.validBytePosition(3)).toEqual(2l);

        expect(AudioConfig.PCM16_44100_2.validBytePosition(0)).toEqual(0l);
        expect(AudioConfig.PCM16_44100_2.validBytePosition(1)).toEqual(0l);
        expect(AudioConfig.PCM16_44100_2.validBytePosition(5)).toEqual(4l);
        expect(AudioConfig.PCM16_44100_2.validBytePosition(8)).toEqual(8l);
    }

    @Test
    public void shouldConvertBytesToMilliSeconds() throws Exception {
        expect(AudioConfig.PCM16_44100_1.bytesToMs(0)).toEqual(0l);
        expect(AudioConfig.PCM16_44100_2.bytesToMs(0)).toEqual(0l);

        expect(AudioConfig.PCM16_44100_1.bytesToMs(44100)).toEqual(500l);
        expect(AudioConfig.PCM16_44100_1.bytesToMs(44100 * 2)).toEqual(1000l);
        expect(AudioConfig.PCM16_44100_2.bytesToMs(44100 * 2)).toEqual(500l);
        expect(AudioConfig.PCM16_44100_2.bytesToMs(44100 * 2 * 2)).toEqual(1000l);
        expect(AudioConfig.PCM16_44100_2.bytesToMs(3313920)).toEqual(18786l);
    }


    @Test
    public void shouldMillisecondsToBytes() throws Exception {
        expect(AudioConfig.PCM16_44100_2.msToByte(18786l)).toEqual(3313850l);
        expect(AudioConfig.PCM16_44100_1.msToByte(500)).toEqual(44100l);
        expect(AudioConfig.PCM16_44100_1.msToByte(1000)).toEqual(88200l);
    }

    @Test
    public void shouldMillisecondsToBytesAdjusted() throws Exception {
        expect(AudioConfig.msToByte(1000, 22050, AudioConfig.PCM16_44100_1.sampleSize)).toEqual(44100l);
    }

    @Test
    public void shouldHaveBytesPerSecond() throws Exception {
        expect(AudioConfig.PCM16_44100_1.bytesPerSecond).toEqual(44100 * 2);
        expect(AudioConfig.PCM16_44100_2.bytesPerSecond).toEqual(44100 * 2 * 2);
    }

    @Test
    public void shouldSelectCorrectChannelConfig() throws Exception {
        expect(AudioConfig.PCM16_44100_1.getChannelConfig(true)).toEqual(AudioFormat.CHANNEL_IN_MONO);
        expect(AudioConfig.PCM16_44100_1.getChannelConfig(false)).toEqual(AudioFormat.CHANNEL_OUT_MONO);

        expect(AudioConfig.PCM16_44100_2.getChannelConfig(true)).toEqual(AudioFormat.CHANNEL_IN_STEREO);
        expect(AudioConfig.PCM16_44100_2.getChannelConfig(false)).toEqual(AudioFormat.CHANNEL_OUT_STEREO);
    }
}
