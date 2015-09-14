package com.soundcloud.android.playback;

import static org.mockito.Mockito.verify;

import com.soundcloud.android.cast.CastPlayer;
import com.soundcloud.android.model.Urn;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.Arrays;
import java.util.List;

@RunWith(MockitoJUnitRunner.class)
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
    public void setNewQueueCallsPlayNewQueue() {
        Urn track = Urn.forTrack(123L);
        List<Urn> tracks = Arrays.asList(track);

        strategy.setNewQueue(PlayQueue.fromTrackUrnList(tracks, PlaySessionSource.EMPTY), track, 3, false, PlaySessionSource.EMPTY);

        verify(castPlayer).setNewQueue(tracks, track, PlaySessionSource.EMPTY);
    }

    @Test
    public void seekCallsSeekWithPositionOnCastPlayer() {
        strategy.seek(123L);

        verify(castPlayer).seek(123L);
    }
}
