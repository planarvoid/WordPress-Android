package com.soundcloud.android.playback.ui;

import static com.soundcloud.android.Expect.expect;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;

import com.soundcloud.android.events.EventBus;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PlayerUIEvent;
import com.soundcloud.android.playback.PlaybackOperations;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;

@RunWith(SoundCloudTestRunner.class)
public class TrackPageListenerTest {

    @Mock
    private PlaybackOperations playbackOperations;
    @Mock
    private EventBus eventBus;

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

        ArgumentCaptor<PlayerUIEvent> captor = ArgumentCaptor.forClass(PlayerUIEvent.class);
        verify(eventBus).publish(eq(EventQueue.PLAYER_UI), captor.capture());
        expect(captor.getValue().getKind()).toEqual(PlayerUIEvent.EXPAND_PLAYER);
    }

    @Test
    public void onPlayerClosePostsEventToClosePlayer() {
        listener.onPlayerClose();

        ArgumentCaptor<PlayerUIEvent> captor = ArgumentCaptor.forClass(PlayerUIEvent.class);
        verify(eventBus).publish(eq(EventQueue.PLAYER_UI), captor.capture());
        expect(captor.getValue().getKind()).toEqual(PlayerUIEvent.COLLAPSE_PLAYER);
    }

}