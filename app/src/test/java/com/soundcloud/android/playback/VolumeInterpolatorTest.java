package com.soundcloud.android.playback;

import static com.soundcloud.android.playback.VolumeInterpolator.ACCELERATE;
import static com.soundcloud.android.playback.VolumeInterpolator.ACCELERATE_DECELERATE;
import static com.soundcloud.android.playback.VolumeInterpolator.DECELERATE;
import static com.soundcloud.android.playback.VolumeInterpolator.LINEAR;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;

public class VolumeInterpolatorTest {

    @Test
    public void testInterpolateAccelerate() {
        assertThat(ACCELERATE.interpolate(0)).isEqualTo(0);
        assertThat(ACCELERATE.interpolate(0.5f)).isEqualTo(0.25f);
        assertThat(ACCELERATE.interpolate(1f)).isEqualTo(1f);
    }

    @Test
    public void testInterpolateDecelerate() {
        assertThat(DECELERATE.interpolate(0)).isEqualTo(0);
        assertThat(DECELERATE.interpolate(0.5f)).isEqualTo(0.75f);
        assertThat(DECELERATE.interpolate(1f)).isEqualTo(1f);
    }

    @Test
    public void testInterpolateAccelerateDecelerate() {
        assertThat(ACCELERATE_DECELERATE.interpolate(0)).isEqualTo(0);
        assertThat(ACCELERATE_DECELERATE.interpolate(0.25f)).isEqualTo(0.14644662f);
        assertThat(ACCELERATE_DECELERATE.interpolate(0.5f)).isEqualTo(0.5f);
        assertThat(ACCELERATE_DECELERATE.interpolate(0.75f)).isEqualTo(0.8535534f);
        assertThat(ACCELERATE_DECELERATE.interpolate(1f)).isEqualTo(1f);
    }

    @Test
    public void testInterpolateLinear() {
        assertThat(LINEAR.interpolate(0)).isEqualTo(0);
        assertThat(LINEAR.interpolate(0.25f)).isEqualTo(0.25f);
        assertThat(LINEAR.interpolate(0.5f)).isEqualTo(0.5f);
        assertThat(LINEAR.interpolate(0.75f)).isEqualTo(0.75f);
        assertThat(LINEAR.interpolate(1f)).isEqualTo(1f);
    }

    @Test
    public void testRangeIncreasing() {
        assertThat(LINEAR.range(0, 20, 30)).isEqualTo(20);
        assertThat(LINEAR.range(0.5f, 20, 30)).isEqualTo(25);
        assertThat(LINEAR.range(1, 20, 30)).isEqualTo(30);
    }

    @Test
    public void testRangeDecreasing() {
        assertThat(LINEAR.range(0, 30, 20)).isEqualTo(30);
        assertThat(LINEAR.range(0.5f, 30, 20)).isEqualTo(25);
        assertThat(LINEAR.range(1, 30, 20)).isEqualTo(20);
    }
}
