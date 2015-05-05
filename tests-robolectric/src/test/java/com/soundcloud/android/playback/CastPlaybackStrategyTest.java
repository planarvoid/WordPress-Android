package com.soundcloud.android.playback;

import static org.mockito.Mockito.verify;

import com.soundcloud.android.cast.CastPlayer;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.service.PlaySessionSource;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import java.util.Arrays;
import java.util.List;

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
    public void playNewQueueCallsPlayNewQueueWithZeroedProgressPosition() {
        Urn track = Urn.forTrack(123L);
        List<Urn> tracks = Arrays.asList(track);

        strategy.playNewQueue(tracks, track, 3, false, PlaySessionSource.EMPTY);

        verify(castPlayer).playNewQueue(tracks, track, 0L, PlaySessionSource.EMPTY);
    }

    @Test
    public void reloadAndPlayCurrentQueueRedirectsCallToCastPlayer() {
        strategy.reloadAndPlayCurrentQueue(123L);

        verify(castPlayer).reloadAndPlayCurrentQueue(123L);
    }

    @Test
    public void seekCallsSeekWithPositionOnCastPlayer() {
        strategy.seek(123L);

        verify(castPlayer).seek(123L);
    }
}