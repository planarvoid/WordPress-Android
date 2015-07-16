package com.soundcloud.android.playback;

import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;

import com.google.common.collect.Lists;
import com.soundcloud.android.ServiceInitiator;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import rx.observers.TestObserver;

import java.util.Arrays;

public class DefaultPlaybackStrategyTest extends AndroidUnitTest {

    private static final Urn TRACK1 = Urn.forTrack(123L);
    private static final Urn TRACK2 = Urn.forTrack(456L);
    private static final Urn TRACK3 = Urn.forTrack(789L);

    private DefaultPlaybackStrategy defaultPlaybackStrategy;
    private TestObserver<PlaybackResult> observer;

    @Mock private PlayQueueManager playQueueManager;
    @Mock private ServiceInitiator serviceInitiator;

    @Before
    public void setUp() throws Exception {
        defaultPlaybackStrategy = new DefaultPlaybackStrategy(playQueueManager, serviceInitiator);
        observer = new TestObserver<>();
    }

    @Test
    public void playCurrentOpensCurrentTrackThroughService() throws Exception {
        defaultPlaybackStrategy.playCurrent();

        verify(serviceInitiator).playCurrent();
    }

    @Test
    public void pausePausesTrackThroughService() throws Exception {
        defaultPlaybackStrategy.pause();

        verify(serviceInitiator).pause();
    }

    @Test
    public void resumePlaysTrackThroughService() throws Exception {
        defaultPlaybackStrategy.resume();

        verify(serviceInitiator).resume();
    }

    @Test
    public void togglePlaybackSendsTogglePlaybackIntent() throws Exception {
        defaultPlaybackStrategy.togglePlayback();

        verify(serviceInitiator).togglePlayback();
    }

    @Test
    public void playNewQueueOpensCurrentTrackThroughService() {
        final PlaySessionSource playSessionSource = PlaySessionSource.EMPTY;
        defaultPlaybackStrategy.playNewQueue(PlayQueue.fromTrackUrnList(Arrays.asList(TRACK1), playSessionSource), TRACK1, 0, false, playSessionSource).subscribe(observer);

        verify(serviceInitiator).playCurrent();
    }

    @Test
    public void playNewQueueRemovesDuplicates() {
        PlaySessionSource playSessionSource = PlaySessionSource.EMPTY;
        defaultPlaybackStrategy.playNewQueue(
                PlayQueue.fromTrackUrnList(Lists.newArrayList(TRACK1, TRACK2, TRACK3, TRACK2, TRACK1), playSessionSource), TRACK1, 0, false, playSessionSource).subscribe(observer);

        PlayQueue expectedPlayQueue = PlayQueue.fromTrackUrnList(Lists.newArrayList(TRACK1, TRACK2, TRACK3), playSessionSource);
        verify(playQueueManager).setNewPlayQueue(eq(expectedPlayQueue), eq(playSessionSource), eq(0));
    }

    @Test
    public void playNewQueueShouldFallBackToPositionZeroIfInitialTrackNotFound() {
        PlaySessionSource playSessionSource = PlaySessionSource.EMPTY;
        defaultPlaybackStrategy.playNewQueue(
                PlayQueue.fromTrackUrnList(Lists.newArrayList(TRACK1, TRACK2), playSessionSource), TRACK1, 2, false, playSessionSource).subscribe(observer);

        PlayQueue expectedPlayQueue = PlayQueue.fromTrackUrnList(Lists.newArrayList(TRACK1, TRACK2), playSessionSource);
        verify(playQueueManager).setNewPlayQueue(eq(expectedPlayQueue), eq(playSessionSource), eq(0));
    }
}