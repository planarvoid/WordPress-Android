package com.soundcloud.android.playback.ui;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PlayerUICommand;
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
        listener = new PageListener(playSessionController, eventBus);
    }

    @Test
    public void onToggleFooterPlayTogglesPlaybackViaPlaybackOperations() {
        listener.onFooterTogglePlay();
        verify(playSessionController).togglePlayback();
    }

    @Test
    public void onTogglePlayTogglesPlaybackViaPlaybackOperations() {
        listener.onTogglePlay();
        verify(playSessionController).togglePlayback();
    }

    @Test
    public void onFooterTapPostsEventToExpandPlayer() {
        listener.onFooterTap();

        PlayerUICommand event = eventBus.lastEventOn(EventQueue.PLAYER_COMMAND);
        assertThat(event.isExpand()).isTrue();
    }

    @Test
    public void onPlayerClosePostsEventToClosePlayerManually() {
        listener.onPlayerClose();

        PlayerUICommand event = eventBus.lastEventOn(EventQueue.PLAYER_COMMAND);
        assertThat(event.isManualCollapse()).isTrue();
    }

    @Test
    public void requestPlayerCollapsePostsEventToClosePlayerAutomatically() {
        listener.requestPlayerCollapse();

        PlayerUICommand event = eventBus.lastEventOn(EventQueue.PLAYER_COMMAND);
        assertThat(event.isAutomaticCollapse()).isTrue();
    }

}
