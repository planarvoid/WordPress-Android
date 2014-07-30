package com.soundcloud.android.playback;

import static com.soundcloud.android.Expect.expect;

import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.robolectric.shadows.ShadowSystemClock;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(SoundCloudTestRunner.class)
public class PlaybackProgressTest {

    @After
    public void tearDown() throws Exception {
        ShadowSystemClock.reset();
    }

    @Test
    public void handleZeroedDurationsGracefullyOnGettingProgressProportion() {
        PlaybackProgress event = new PlaybackProgress(0L, 0L);
        expect(event.getProgressProportion()).toEqual(0.0f);
    }

    @Test
    public void calculateProgressProportion() {
        PlaybackProgress event = new PlaybackProgress(31L, 100L);
        expect(event.getProgressProportion()).toEqual(0.31f);
    }

    @Test
    public void getTimeSinceCreatedReturnsTheTimeSinceCreation() {
        ShadowSystemClock.setUptimeMillis(1000L);
        PlaybackProgress event = new PlaybackProgress(31L, 100L);

        ShadowSystemClock.setUptimeMillis(2000L);
        expect(event.getTimeSinceCreation()).toEqual(1000L);
    }

    @Test
    public void getTimeLeftReturnsDurationMinusPosition() {
        PlaybackProgress event = new PlaybackProgress(31L, 100L);
        expect(event.getTimeLeft()).toEqual(69L);
    }

    @Test
    public void durationIsInvalidDurationIsZero() {
        PlaybackProgress event = new PlaybackProgress(31L, 0L);
        expect(event.isDurationValid()).toBeFalse();
    }

    @Test
    public void durationIsInvalidDurationIsInferiorToZero() {
        PlaybackProgress event = new PlaybackProgress(31L, -1L);
        expect(event.isDurationValid()).toBeFalse();
    }

    @Test
    public void durationIsValidDurationIsSuperiorToZero() {
        PlaybackProgress event = new PlaybackProgress(31L, 1L);
        expect(event.isDurationValid()).toBeTrue();
    }
}