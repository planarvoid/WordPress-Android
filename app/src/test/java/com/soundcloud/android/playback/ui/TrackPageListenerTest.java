package com.soundcloud.android.playback.ui;

import static com.soundcloud.android.Expect.expect;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PlayerUIEvent;
import com.soundcloud.android.playback.PlaybackOperations;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.rx.eventbus.TestEventBus;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

@RunWith(SoundCloudTestRunner.class)
public class TrackPageListenerTest {

    @Mock private PlaybackOperations playbackOperations;

    private TestEventBus eventBus = new TestEventBus();

    private TrackPageListener listener;

    @Before
    public void setUp() throws Exception {
        listener = new TrackPageListener(playbackOperations, eventBus);
    }

    @Test
    public void onTogglePlayTogglesPlaybackViaPlaybackOperations() {
        listener.onTogglePlay();
        verify(playbackOperations).togglePlayback();
    }

    @Test
    public void onFooterTapPostsEventToExpandPlayer() {
        listener.onFooterTap();

        PlayerUIEvent event = eventBus.lastEventOn(EventQueue.PLAYER_UI);
        expect(event.getKind()).toEqual(PlayerUIEvent.EXPAND_PLAYER);
    }

    @Test
    public void onPlayerClosePostsEventToClosePlayer() {
        listener.onPlayerClose();

        PlayerUIEvent event = eventBus.lastEventOn(EventQueue.PLAYER_UI);
        expect(event.getKind()).toEqual(PlayerUIEvent.COLLAPSE_PLAYER);
    }

    @Test
    public void shouldPerformPreviousActionIsProgressWithinTrackChangeThreshold() {
        when(playbackOperations.isProgressWithinTrackChangeThreshold()).thenReturn(true);

        listener.onPrevious();

        verify(playbackOperations).previousTrack();
    }

    @Test
    public void shouldRestartPlaybackOnPreviousIsProgressNotWithinTrackChangeThreshold() {
        when(playbackOperations.isProgressWithinTrackChangeThreshold()).thenReturn(false);

        listener.onPrevious();

        verify(playbackOperations).restartPlayback();
    }

}