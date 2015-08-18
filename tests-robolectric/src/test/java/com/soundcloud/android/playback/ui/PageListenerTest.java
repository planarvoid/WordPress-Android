package com.soundcloud.android.playback.ui;

import static com.soundcloud.android.Expect.expect;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PlayControlEvent;
import com.soundcloud.android.events.PlayerUICommand;
import com.soundcloud.android.events.TrackingEvent;
import com.soundcloud.android.events.UIEvent;
import com.soundcloud.android.playback.PlaySessionStateProvider;
import com.soundcloud.android.playback.PlaybackOperations;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.rx.eventbus.TestEventBus;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

@RunWith(SoundCloudTestRunner.class)
public class PageListenerTest {

    @Mock private PlaybackOperations playbackOperations;
    @Mock private PlaySessionStateProvider playSessionStateProvider;

    private TestEventBus eventBus = new TestEventBus();

    private PageListener listener;

    @Before
    public void setUp() throws Exception {
        listener = new PageListener(playbackOperations,
                playSessionStateProvider, eventBus);
    }

    @Test
    public void onToggleFooterPlayEmitsPauseEventWhenWasPlaying() {
        when(playSessionStateProvider.isPlaying()).thenReturn(true);

        listener.onFooterTogglePlay();

        TrackingEvent event = eventBus.lastEventOn(EventQueue.TRACKING);
        expect(event).toEqual(PlayControlEvent.pause(PlayControlEvent.SOURCE_FOOTER_PLAYER));
    }

    @Test
    public void onToggleFooterPlayEmitsPlayEventWhenWasPaused() {
        when(playSessionStateProvider.isPlaying()).thenReturn(false);

        listener.onFooterTogglePlay();

        TrackingEvent event = eventBus.lastEventOn(EventQueue.TRACKING);
        expect(event).toEqual(PlayControlEvent.play(PlayControlEvent.SOURCE_FOOTER_PLAYER));
    }

    @Test
    public void onToggleFooterPlayTogglesPlaybackViaPlaybackOperations() {
        listener.onFooterTogglePlay();
        verify(playbackOperations).togglePlayback();
    }

    @Test
    public void onTogglePlayEmitsPauseEventWhenWasPlaying() {
        when(playSessionStateProvider.isPlaying()).thenReturn(true);

        listener.onTogglePlay();

        TrackingEvent event = eventBus.lastEventOn(EventQueue.TRACKING);
        expect(event).toEqual(PlayControlEvent.pause(PlayControlEvent.SOURCE_FULL_PLAYER));
    }

    @Test
    public void onTogglePlayEmitsPlayEventWhenWasPaused() {
        when(playSessionStateProvider.isPlaying()).thenReturn(false);

        listener.onTogglePlay();

        TrackingEvent event = eventBus.lastEventOn(EventQueue.TRACKING);
        expect(event).toEqual(PlayControlEvent.play(PlayControlEvent.SOURCE_FULL_PLAYER));
    }

    @Test
    public void onTogglePlayTogglesPlaybackViaPlaybackOperations() {
        listener.onTogglePlay();
        verify(playbackOperations).togglePlayback();
    }

    @Test
    public void onFooterTapEmitsUIEventOpenPlayer() {
        listener.onFooterTap();

        UIEvent event = (UIEvent) eventBus.lastEventOn(EventQueue.TRACKING);
        UIEvent expectedEvent = UIEvent.fromPlayerOpen(UIEvent.METHOD_TAP_FOOTER);
        expect(event.getKind()).toEqual(expectedEvent.getKind());
        expect(event.getAttributes()).toEqual(expectedEvent.getAttributes());
    }

    @Test
    public void onFooterTapPostsEventToExpandPlayer() {
        listener.onFooterTap();

        PlayerUICommand event = eventBus.lastEventOn(EventQueue.PLAYER_COMMAND);
        expect(event.isExpand()).toBeTrue();
    }

    @Test
    public void onPlayerClosePostsEventToClosePlayer() {
        listener.onPlayerClose();

        PlayerUICommand event = eventBus.lastEventOn(EventQueue.PLAYER_COMMAND);
        expect(event.isCollapse()).toBeTrue();
    }

    @Test
    public void onPlayerCloseEmitsUIEventClosePlayer() {
        listener.onPlayerClose();

        UIEvent event = (UIEvent) eventBus.lastEventOn(EventQueue.TRACKING);
        UIEvent expectedEvent = UIEvent.fromPlayerClose(UIEvent.METHOD_HIDE_BUTTON);
        expect(event.getKind()).toEqual(expectedEvent.getKind());
        expect(event.getAttributes()).toEqual(expectedEvent.getAttributes());
    }

}