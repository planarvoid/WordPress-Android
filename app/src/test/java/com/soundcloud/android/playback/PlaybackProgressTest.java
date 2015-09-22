package com.soundcloud.android.playback;


import static org.assertj.core.api.Assertions.assertThat;

import com.soundcloud.android.utils.CurrentDateProvider;
import com.soundcloud.android.utils.TestDateProvider;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.concurrent.TimeUnit;

@RunWith(MockitoJUnitRunner.class)
public class PlaybackProgressTest {

    @Test
    public void handleZeroedDurationsGracefullyOnGettingProgressProportion() {
        PlaybackProgress event = createPlaybackProgress(0L, 0L);
        assertThat(event.getProgressProportion()).isEqualTo(0.0f);
    }

    @Test
    public void calculateProgressProportion() {
        PlaybackProgress event = createPlaybackProgress(31L, 100L);
        assertThat(event.getProgressProportion()).isEqualTo(0.31f);
    }

    @Test
    public void getTimeSinceCreatedReturnsTheTimeSinceCreation() {
        final TestDateProvider dateProvider = new TestDateProvider(1000L);
        PlaybackProgress event = new PlaybackProgress(31L, 100L, dateProvider);

        dateProvider.advanceBy(1000L, TimeUnit.MILLISECONDS);
        assertThat(event.getTimeSinceCreation()).isEqualTo(1000L);
    }

    @Test
    public void getTimeLeftReturnsDurationMinusPosition() {
        PlaybackProgress event = createPlaybackProgress(31L, 100L);
        assertThat(event.getTimeLeft()).isEqualTo(69L);
    }

    @Test
    public void durationIsInvalidDurationIsZero() {
        PlaybackProgress event = createPlaybackProgress(31L, 0L);
        assertThat(event.isDurationValid()).isFalse();
    }

    @Test
    public void durationIsInvalidDurationIsInferiorToZero() {
        PlaybackProgress event = createPlaybackProgress(31L, -1L);
        assertThat(event.isDurationValid()).isFalse();
    }

    @Test
    public void durationIsValidDurationIsSuperiorToZero() {
        PlaybackProgress event = createPlaybackProgress(31L, 1L);
        assertThat(event.isDurationValid()).isTrue();
    }

    @Test // had a crash in production because time left cam back as negative
    // see https://github.com/soundcloud/SoundCloud-Android/issues/2409
    public void shouldClampTimeLeftToZero() {
        PlaybackProgress event = createPlaybackProgress(1L, 0L);
        assertThat(event.getTimeLeft()).isEqualTo(0L);
    }

    private PlaybackProgress createPlaybackProgress(long position, long duration) {
        return new PlaybackProgress(position, duration, new CurrentDateProvider());
    }

}