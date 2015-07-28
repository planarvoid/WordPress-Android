package com.soundcloud.android.model;

import static org.assertj.core.api.Assertions.assertThat;

import com.soundcloud.android.waveform.WaveformData;
import nl.jqno.equalsverifier.EqualsVerifier;
import org.junit.Test;

import java.util.Arrays;

public class WaveformDataTest {

    @Test
    public void shouldDownscaleSampleData() throws Exception {
        int[] samples = {20, 30, 5, 10, 50, 90, 100, 500};
        WaveformData data = new WaveformData(500, samples);

        assertThat(Arrays.equals(data.scale(4).samples, new int[]{20, 5, 50, 100})).isTrue();
        assertThat(data.scale(4).maxAmplitude).isEqualTo(100);
    }

    @Test
    public void shouldUpscaleSampleData() throws Exception {
        int[] samples = {20, 30, 5, 10, 50, 90, 100, 500};
        WaveformData data = new WaveformData(500, samples);
        assertThat(Arrays.equals(data.scale(16).samples, new int[]{
                20, 20,
                30, 30,
                5, 5,
                10, 10,
                50, 50,
                90, 90,
                100, 100,
                500, 500})).isTrue();
        assertThat(data.scale(16).maxAmplitude).isEqualTo(500);
    }

    @Test
    public void shouldReturnSameInstanceIfNoScalingRequired() throws Exception {
        int[] samples = {20, 30, 5, 10, 50, 90, 100, 500};
        WaveformData data = new WaveformData(500, samples);
        assertThat(data.scale(samples.length)).isSameAs(data);
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldRequirePositiveWidth() throws Exception {
        int[] samples = {20, 30, 5, 10, 50, 90, 100, 500};
        WaveformData data = new WaveformData(100, samples);
        data.scale(-1);
    }

    @Test
    public void implementsEquals() {
        EqualsVerifier.forClass(WaveformData.class).verify();
    }
}
