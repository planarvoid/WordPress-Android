package com.soundcloud.android.playback;

import static org.mockito.Mockito.verify;

import com.soundcloud.android.cast.CastPlayer;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

@RunWith(SoundCloudTestRunner.class)
public class CastPlaybackStrategyTest {

    private CastPlaybackStrategy strategy;

    @Mock private CastPlayer castPlayer;

    @Before
    public void setUp() {
        strategy = new CastPlaybackStrategy(castPlayer);
    }

    @Test
    public void togglePlaybackTogglesPlaybackOnCastPlayer() {
        strategy.togglePlayback();

        verify(castPlayer).togglePlayback();
    }

    @Test
    public void resumeResumesOnCastPlayer() {
        strategy.resume();

        verify(castPlayer).resume();
    }

    @Test
    public void pausePausesOnCastPlayer() {
        strategy.pause();

        verify(castPlayer).pause();
    }

    @Test
    public void playCurrentCallsPlayCurrentOnCastPlayer() {
        strategy.playCurrent();

        verify(castPlayer).playCurrent();
    }

    @Test
    public void playCurrentFromPositionCallsPlayCurrentWithPositionOnCastPlayer() {
        strategy.playCurrent(123L);

        verify(castPlayer).playCurrent(123L);
    }

    @Test
    public void seekCallsSeekWithPositionOnCastPlayer() {
        strategy.seek(123L);

        verify(castPlayer).seek(123L);
    }
}