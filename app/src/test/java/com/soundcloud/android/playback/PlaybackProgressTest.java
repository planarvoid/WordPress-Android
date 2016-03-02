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
    public void getTimeSinceCreatedReturnsTheTimeSinceCreation() {
        final TestDateProvider dateProvider = new TestDateProvider(1000L);
        PlaybackProgress event = new PlaybackProgress(31L, 100L, dateProvider);

        dateProvider.advanceBy(1000L, TimeUnit.MILLISECONDS);
        assertThat(event.getTimeSinceCreation()).isEqualTo(1000L);
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

    @Test
    public void isPastFirstQuartileReturnsFalseWhenInvalidDuration() {
        PlaybackProgress event = createPlaybackProgress(0, 0);
        assertThat(event.isPastFirstQuartile()).isFalse();
    }

    @Test
    public void isPastFirstQuartileReturnsTrueWhenExactlyAtFirstQuartile() {
        PlaybackProgress event = createPlaybackProgress(1, 4);
        assertThat(event.isPastFirstQuartile()).isTrue();
    }

    @Test
    public void isPastFirstQuartileReturnsTrueWhenPastFirstQuartile() {
        PlaybackProgress event = createPlaybackProgress(2, 4);
        assertThat(event.isPastFirstQuartile()).isTrue();
    }

    @Test
    public void isPastSecondQuartileReturnsFalseWhenInvalidDuration() {
        PlaybackProgress event = createPlaybackProgress(0, 0);
        assertThat(event.isPastSecondQuartile()).isFalse();
    }

    @Test
    public void isPastSecondQuartileReturnsTrueWhenExactlyAtSecondQuartile() {
        PlaybackProgress event = createPlaybackProgress(2, 4);
        assertThat(event.isPastSecondQuartile()).isTrue();
    }

    @Test
    public void isPastSecondQuartileReturnsTrueWhenPastSecondQuartile() {
        PlaybackProgress event = createPlaybackProgress(3, 4);
        assertThat(event.isPastSecondQuartile()).isTrue();
    }

    @Test
    public void isPastThirdQuartileReturnsFalseWhenInvalidDuration() {
        PlaybackProgress event = createPlaybackProgress(0,0);
        assertThat(event.isPastThirdQuartile()).isFalse();
    }

    @Test
    public void isPastThirdQuartileReturnsTrueWhenExactlyAtThirdQuartile() {
        PlaybackProgress event = createPlaybackProgress(3, 4);
        assertThat(event.isPastThirdQuartile()).isTrue();
    }

    @Test
    public void isPastThirdQuartileReturnsTrueWhenPastThirdQuartile() {
        PlaybackProgress event = createPlaybackProgress(4, 4);
        assertThat(event.isPastThirdQuartile()).isTrue();
    }

    private PlaybackProgress createPlaybackProgress(long position, long duration) {
        return new PlaybackProgress(position, duration, new CurrentDateProvider());
    }

}
