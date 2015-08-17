package com.soundcloud.android.waveform;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;

public class WaveformDataTest {
    private int[] samples = {20, 30, 5, 10, 50, 90, 100, 500};
    private int[] upSampledOneAndAHalf = {20, 25, 30, 5, 7, 10, 50, 70, 90, 100, 300, 500};
    private int[] upSampledDouble = {20, 20, 30, 30, 5, 5, 10, 10, 50, 50, 90, 90, 100, 100, 500, 500};
    private int[] downSampledDecimal = {17, 69, 483};
    private int[] downSampledHalf = {25, 8, 70, 300};
    private int[] downSampledThreeQuarters = {23, 18, 9, 60, 95, 400};
    private int[] otherSamples = {1, 2, 3, 4, 5, 6, 7, 500};
    private int[] emptySamples = {};


    @Test
    public void shouldDownscaleSampleData() throws Exception {
        WaveformData data = new WaveformData(500, samples).scale(4);

        assertThat(data.samples).isEqualTo(downSampledHalf);
        assertThat(data.maxAmplitude).isEqualTo(300);
    }

    @Test
    public void shouldDownscaleSampleDataInterpolated() throws Exception {
        WaveformData data = new WaveformData(500, samples).scale(6);

        assertThat(data.samples).isEqualTo(downSampledThreeQuarters);
        assertThat(data.maxAmplitude).isEqualTo(400);
    }

    @Test
    public void shouldReturnSameInstanceIfNoScalingRequired() throws Exception {
        WaveformData expected = new WaveformData(500, samples);
        WaveformData data = expected.scale(samples.length);

        assertThat(data).isSameAs(expected);
    }

    @Test
    public void shouldUpscaleSampleDataInterpolated() throws Exception {
        WaveformData data = new WaveformData(500, samples).scale(12);

        assertThat(data.samples).isEqualTo(upSampledOneAndAHalf);
        assertThat(data.maxAmplitude).isEqualTo(500);
    }

    @Test
    public void shouldUpscaleSampleData() throws Exception {
        WaveformData data = new WaveformData(500, samples).scale(16);

        assertThat(data.samples).isEqualTo(upSampledDouble);
        assertThat(data.maxAmplitude).isEqualTo(500);
    }

    @Test
    public void shouldScaleDecimalSamples() throws Exception {
        WaveformData data = new WaveformData(500, samples).scale(2.3);

        assertThat(data.samples).isEqualTo(downSampledDecimal);
        assertThat(data.maxAmplitude).isEqualTo(483);
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldRequirePositiveWidth() throws Exception {
        WaveformData data = new WaveformData(500, samples);
        data.scale(-1);
    }

    @Test
    public void shouldAllowZeroWidth() throws Exception {
        WaveformData data = new WaveformData(500, samples).scale(0);

        assertThat(data.samples).isEqualTo(emptySamples);
        assertThat(data.maxAmplitude).isEqualTo(0);
    }

    @Test
    public void implementsEquals() {
        WaveformData same = new WaveformData(500, samples);
        WaveformData differentSamples = new WaveformData(500, otherSamples);
        WaveformData differentSamplesLength = new WaveformData(500, upSampledDouble);
        WaveformData differentMaxAmplitude = new WaveformData(300, samples);

        WaveformData data = new WaveformData(500, samples);

        assertThat(data).isEqualTo(same);
        assertThat(data).isNotEqualTo(differentSamples);
        assertThat(data).isNotEqualTo(differentSamplesLength);
        assertThat(data).isNotEqualTo(differentMaxAmplitude);
    }
}