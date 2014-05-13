package com.soundcloud.android.playback;

import static com.soundcloud.android.Expect.expect;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.soundcloud.android.actionbar.ActionBarController;
import com.soundcloud.android.events.EventBus;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PlayerUIEvent;
import com.soundcloud.android.robolectric.EventMonitor;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import android.view.View;

@RunWith(SoundCloudTestRunner.class)
public class PlayerPanelListenerTest {

    @Mock
    private EventBus eventBus;
    @Mock
    private ActionBarController actionBarController;
    @Mock
    private View layout;

    private EventMonitor eventMonitor;
    private PlayerPanelListener listener;

    @Before
    public void setUp() throws Exception {
        eventMonitor = EventMonitor.on(eventBus);
        listener = new PlayerPanelListener(eventBus, actionBarController);
    }

    @Test
    public void shouldSetCollapsedStateWhenPassingOverThreshold() {
        listener.onPanelSlide(layout, 0.4f);
        listener.onPanelSlide(layout, 0.6f);
        listener.onPanelSlide(layout, 0.7f);

        verify(actionBarController, times(1)).setVisible(true);
    }

    @Test
    public void shouldSetExpandedStateWhenPassingUnderThreshold() {
        listener.onPanelSlide(layout, 0.6f);
        listener.onPanelSlide(layout, 0.4f);
        listener.onPanelSlide(layout, 0.3f);

        verify(actionBarController, times(1)).setVisible(false);
        PlayerUIEvent event = eventMonitor.verifyEventOn(EventQueue.PLAYER_UI);
        expect(event.getKind()).toEqual(PlayerUIEvent.PLAYER_EXPANDED);
    }

}