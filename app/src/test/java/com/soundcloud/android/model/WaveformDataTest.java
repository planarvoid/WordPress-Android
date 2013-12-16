package com.soundcloud.android.model;

import static com.soundcloud.android.Expect.expect;

import org.junit.Test;

import java.util.Arrays;

public class WaveformDataTest {
    @Test
    public void shouldDownscaleSampleData() throws Exception {
        int[] samples = {20, 30, 5, 10, 50, 90, 100, 500};
        WaveformData data = new WaveformData(500, samples);

        expect(Arrays.equals(data.scale(4).samples, new int[]{20, 5, 50, 100})).toBeTrue();
        expect(data.scale(4).maxAmplitude).toEqual(100);
    }

    @Test
    public void shouldUpscaleSampleData() throws Exception {
        int[] samples = {20, 30, 5, 10, 50, 90, 100, 500};
        WaveformData data = new WaveformData(500, samples);
        expect(Arrays.equals(data.scale(16).samples, new int[]{
                20, 20,
                30, 30,
                5, 5,
                10, 10,
                50, 50,
                90, 90,
                100, 100,
                500, 500})).toBeTrue();
        expect(data.scale(16).maxAmplitude).toEqual(500);
    }

    @Test
    public void shouldReturnSameInstanceIfNoScalingRequired() throws Exception {
        int[] samples = {20, 30, 5, 10, 50, 90, 100, 500};
        WaveformData data = new WaveformData(500, samples);
        expect(data.scale(samples.length)).toBe(data);
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldRequirePositiveWidth() throws Exception {
        int[] samples = {20, 30, 5, 10, 50, 90, 100, 500};
        WaveformData data = new WaveformData(100, samples);
        data.scale(-1);
    }
}
