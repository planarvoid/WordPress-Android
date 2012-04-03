package com.soundcloud.android.record;

import static com.soundcloud.android.Expect.expect;

import org.junit.Test;

public class AudioConfigTest {

    @Test
    public void shouldCalculateStartPosition() throws Exception {
        expect(AudioConfig.PCM16_44100_1.startPosition(0)).toEqual(0l);
        expect(AudioConfig.PCM16_44100_1.startPosition(1)).toEqual(0l);
        expect(AudioConfig.PCM16_44100_1.startPosition(2)).toEqual(2l);
        expect(AudioConfig.PCM16_44100_1.startPosition(3)).toEqual(2l);

        expect(AudioConfig.PCM16_44100_2.startPosition(0)).toEqual(0l);
        expect(AudioConfig.PCM16_44100_2.startPosition(1)).toEqual(0l);
        expect(AudioConfig.PCM16_44100_2.startPosition(5)).toEqual(4l);
        expect(AudioConfig.PCM16_44100_2.startPosition(8)).toEqual(8l);
    }

    @Test
    public void shouldConvertBytesToMilliSeconds() throws Exception {
        expect(AudioConfig.PCM16_44100_1.bytesPerSecond).toEqual(44100 * 2);
        expect(AudioConfig.PCM16_44100_2.bytesPerSecond).toEqual(44100 * 2 * 2);

        expect(AudioConfig.PCM16_44100_1.bytesToMs(0)).toEqual(0l);
        expect(AudioConfig.PCM16_44100_2.bytesToMs(0)).toEqual(0l);

        expect(AudioConfig.PCM16_44100_1.bytesToMs(44100)).toEqual(500l);
        expect(AudioConfig.PCM16_44100_1.bytesToMs(44100 * 2)).toEqual(1000l);
        expect(AudioConfig.PCM16_44100_2.bytesToMs(44100 * 2)).toEqual(500l);
        expect(AudioConfig.PCM16_44100_2.bytesToMs(44100 * 2 * 2)).toEqual(1000l);
    }
}
