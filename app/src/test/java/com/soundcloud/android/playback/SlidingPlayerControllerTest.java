package com.soundcloud.android.playback;

import static com.soundcloud.android.Expect.expect;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sothree.slidinguppanel.SlidingUpPanelLayout;
import com.soundcloud.android.R;
import com.soundcloud.android.actionbar.ActionBarController;
import com.soundcloud.android.events.EventBus;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PlayerUIEvent;
import com.soundcloud.android.playback.ui.SlidingPlayerController;
import com.soundcloud.android.robolectric.EventMonitor;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import android.app.Activity;
import android.view.View;

@RunWith(SoundCloudTestRunner.class)
public class SlidingPlayerControllerTest {

    @Mock
    private EventBus eventBus;
    @Mock
    private ActionBarController actionBarController;
    @Mock
    private View layout;
    @Mock
    private Activity activity;
    @Mock
    private SlidingUpPanelLayout slidingPanel;

    private EventMonitor eventMonitor;
    private SlidingPlayerController controller;

    @Before
    public void setUp() throws Exception {
        eventMonitor = EventMonitor.on(eventBus);
        controller = new SlidingPlayerController(eventBus);
        when(activity.findViewById(R.id.sliding_layout)).thenReturn(slidingPanel);
        controller.attach(activity, actionBarController);
    }

    @Test
    public void shouldConfigureSlidingPanelOnAttach() {
        verify(slidingPanel).setPanelSlideListener(controller);
        verify(slidingPanel).setDragView(any(View.class));
    }

    @Test
    public void shouldSetCollapsedStateWhenPassingOverThreshold() {
        controller.onPanelSlide(layout, 0.4f);
        controller.onPanelSlide(layout, 0.6f);
        controller.onPanelSlide(layout, 0.7f);

        verify(actionBarController, times(1)).setVisible(true);
    }

    @Test
    public void shouldSetExpandedStateWhenPassingUnderThreshold() {
        controller.onPanelSlide(layout, 0.6f);
        controller.onPanelSlide(layout, 0.4f);
        controller.onPanelSlide(layout, 0.3f);

        verify(actionBarController, times(1)).setVisible(false);
        PlayerUIEvent event = eventMonitor.verifyEventOn(EventQueue.PLAYER_UI);
        expect(event.getKind()).toEqual(PlayerUIEvent.PLAYER_EXPANDED);
    }

}