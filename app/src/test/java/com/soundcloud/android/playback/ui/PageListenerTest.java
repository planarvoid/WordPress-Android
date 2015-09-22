package com.soundcloud.android.playback.ui;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PlayControlEvent;
import com.soundcloud.android.events.PlayerUICommand;
import com.soundcloud.android.events.TrackingEvent;
import com.soundcloud.android.events.UIEvent;
import com.soundcloud.android.playback.PlaySessionController;
import com.soundcloud.android.playback.PlaySessionStateProvider;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.rx.eventbus.TestEventBus;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

public class PageListenerTest extends AndroidUnitTest {

    @Mock private PlaySessionController playSessionController;
    @Mock private PlaySessionStateProvider playSessionStateProvider;

    private TestEventBus eventBus = new TestEventBus();

    private PageListener listener;

    @Before
    public void setUp() throws Exception {
        listener = new PageListener(playSessionController,
                playSessionStateProvider, eventBus);
    }

    @Test
    public void onToggleFooterPlayEmitsPauseEventWhenWasPlaying() {
        when(playSessionStateProvider.isPlaying()).thenReturn(true);

        listener.onFooterTogglePlay();

        TrackingEvent event = eventBus.lastEventOn(EventQueue.TRACKING);
        assertThat(event).isEqualTo(PlayControlEvent.pause(PlayControlEvent.SOURCE_FOOTER_PLAYER));
    }

    @Test
    public void onToggleFooterPlayEmitsPlayEventWhenWasPaused() {
        when(playSessionStateProvider.isPlaying()).thenReturn(false);

        listener.onFooterTogglePlay();

        TrackingEvent event = eventBus.lastEventOn(EventQueue.TRACKING);
        assertThat(event).isEqualTo(PlayControlEvent.play(PlayControlEvent.SOURCE_FOOTER_PLAYER));
    }

    @Test
    public void onToggleFooterPlayTogglesPlaybackViaPlaybackOperations() {
        listener.onFooterTogglePlay();
        verify(playSessionController).togglePlayback();
    }

    @Test
    public void onTogglePlayEmitsPauseEventWhenWasPlaying() {
        when(playSessionStateProvider.isPlaying()).thenReturn(true);

        listener.onTogglePlay();

        TrackingEvent event = eventBus.lastEventOn(EventQueue.TRACKING);
        assertThat(event).isEqualTo(PlayControlEvent.pause(PlayControlEvent.SOURCE_FULL_PLAYER));
    }

    @Test
    public void onTogglePlayEmitsPlayEventWhenWasPaused() {
        when(playSessionStateProvider.isPlaying()).thenReturn(false);

        listener.onTogglePlay();

        TrackingEvent event = eventBus.lastEventOn(EventQueue.TRACKING);
        assertThat(event).isEqualTo(PlayControlEvent.play(PlayControlEvent.SOURCE_FULL_PLAYER));
    }

    @Test
    public void onTogglePlayTogglesPlaybackViaPlaybackOperations() {
        listener.onTogglePlay();
        verify(playSessionController).togglePlayback();
    }

    @Test
    public void onFooterTapEmitsUIEventOpenPlayer() {
        listener.onFooterTap();

        UIEvent event = (UIEvent) eventBus.lastEventOn(EventQueue.TRACKING);
        UIEvent expectedEvent = UIEvent.fromPlayerOpen(UIEvent.METHOD_TAP_FOOTER);
        assertThat(event.getKind()).isEqualTo(expectedEvent.getKind());
        assertThat(event.getAttributes()).isEqualTo(expectedEvent.getAttributes());
    }

    @Test
    public void onFooterTapPostsEventToExpandPlayer() {
        listener.onFooterTap();

        PlayerUICommand event = eventBus.lastEventOn(EventQueue.PLAYER_COMMAND);
        assertThat(event.isExpand()).isTrue();
    }

    @Test
    public void onPlayerClosePostsEventToClosePlayer() {
        listener.onPlayerClose();

        PlayerUICommand event = eventBus.lastEventOn(EventQueue.PLAYER_COMMAND);
        assertThat(event.isCollapse()).isTrue();
    }

    @Test
    public void onPlayerCloseEmitsUIEventClosePlayer() {
        listener.onPlayerClose();

        UIEvent event = (UIEvent) eventBus.lastEventOn(EventQueue.TRACKING);
        UIEvent expectedEvent = UIEvent.fromPlayerClose(UIEvent.METHOD_HIDE_BUTTON);
        assertThat(event.getKind()).isEqualTo(expectedEvent.getKind());
        assertThat(event.getAttributes()).isEqualTo(expectedEvent.getAttributes());
    }

}