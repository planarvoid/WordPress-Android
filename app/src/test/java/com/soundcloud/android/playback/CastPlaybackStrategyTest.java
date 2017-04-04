package com.soundcloud.android.playback;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.ads.AdFixtures;
import com.soundcloud.android.ads.AudioAd;
import com.soundcloud.android.cast.CastPlayer;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.fixtures.TestPlayQueue;
import com.soundcloud.android.testsupport.fixtures.TestPlayQueueItem;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import java.util.Collections;
import java.util.List;

public class CastPlaybackStrategyTest extends AndroidUnitTest {

    private CastPlaybackStrategy strategy;

    @Mock private PlayQueueManager playQueueManager;
    @Mock private CastPlayer castPlayer;

    @Before
    public void setUp() {
        strategy = new CastPlaybackStrategy(playQueueManager, castPlayer);
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
    public void fadeAndPauseJustPausesOnCastPlayer() {
        strategy.fadeAndPause();

        verify(castPlayer).pause();
    }

    @Test
    public void playCurrentCallsPlayCurrentOnCastPlayerForTrack() {
        when(playQueueManager.getCurrentPlayQueueItem()).thenReturn(TestPlayQueueItem.createTrack(Urn.forTrack(1234L)));

        strategy.playCurrent();

        verify(castPlayer).playCurrent();
    }

    @Test
    public void playCurrentCallsPlayCurrentOnCastPlayerForPlaylist() {
        when(playQueueManager.getCurrentPlayQueueItem()).thenReturn(TestPlayQueueItem.createPlaylist(Urn.forPlaylist(45678L)));

        strategy.playCurrent();

        verify(castPlayer).playCurrent();
    }

    @Test
    public void playCurrentDoesNotPlayAdOnCast() {
        final AudioAd audioAd = AdFixtures.getAudioAd(Urn.forTrack(123L));
        when(playQueueManager.getCurrentPlayQueueItem()).thenReturn(TestPlayQueueItem.createAudioAd(audioAd));

        strategy.playCurrent();

        verify(castPlayer, never()).playCurrent();
    }

    @Test
    public void playCurrentDoesNotPlayEmptyPlayQueueItemOnCast() {
        when(playQueueManager.getCurrentPlayQueueItem()).thenReturn(PlayQueueItem.EMPTY);

        strategy.playCurrent();

        verify(castPlayer, never()).playCurrent();
    }

    @Test
    public void setNewQueueCallsPlayNewQueue() {
        Urn track = Urn.forTrack(123L);
        List<Urn> tracks = Collections.singletonList(track);

        final PlayQueue playQueue = TestPlayQueue.fromUrns(tracks, PlaySessionSource.EMPTY);
        strategy.setNewQueue(playQueue, track, 3, PlaySessionSource.EMPTY);

        verify(castPlayer).setNewQueue(playQueue, track, PlaySessionSource.EMPTY);
    }

    @Test
    public void seekCallsSeekWithPositionOnCastPlayer() {
        strategy.seek(123L);

        verify(castPlayer).seek(123L);
    }
}
