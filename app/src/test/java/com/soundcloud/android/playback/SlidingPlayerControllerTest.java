package com.soundcloud.android.playback;

import static com.soundcloud.android.Expect.expect;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
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
import android.os.Bundle;
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
    public void configuresSlidingPanelOnAttach() {
        verify(slidingPanel).setPanelSlideListener(controller);
        verify(slidingPanel).setEnableDragViewTouchEvents(true);
    }

    @Test
    public void subscribesToPlayerUIEvents() {
        controller.startListening();

        eventMonitor.verifySubscribedTo(EventQueue.PLAYER_UI);
    }

    @Test
    public void unsubscribesFromPlayerUIEvents() {
        controller.startListening();
        controller.stopListening();

        eventMonitor.verifyUnsubscribed();
    }

    @Test
    public void expandsPlayerWhenPlayTriggeredEventIsReceived() {
        controller.startListening();
        eventMonitor.publish(EventQueue.PLAYER_UI, PlayerUIEvent.forExpandPlayer());

        verify(slidingPanel).expandPane();
    }

    @Test
    public void closesPlayerWhenPlayCloseEventIsReceived() {
        controller.startListening();
        eventMonitor.publish(EventQueue.PLAYER_UI, PlayerUIEvent.forCollapsePlayer());

        verify(slidingPanel).collapsePane();
    }

    @Test
    public void onlyRespondsToPlayTriggeredPlayerUIEvent() {
        controller.startListening();
        eventMonitor.publish(EventQueue.PLAYER_UI, PlayerUIEvent.fromPlayerCollapsed());

        verify(slidingPanel, times(0)).expandPane();
    }

    @Test
    public void doesntInteractWithActionBarIfBundleIsNullOnRestoreState() {
        controller.restoreState(null);

        verifyZeroInteractions(actionBarController);
    }

    @Test
    public void hidesActionBarIfExpandedStateStored() {
        Bundle bundle = new Bundle();
        bundle.putBoolean("player_expanded", true);

        controller.restoreState(bundle);

        verify(actionBarController).setVisible(false);
    }

    @Test
    public void storesExpandedStateInBundle() {
        when(slidingPanel.isExpanded()).thenReturn(true);
        Bundle bundle = new Bundle();

        controller.storeState(bundle);

        expect(bundle.getBoolean("player_expanded")).toBeTrue();
    }

    @Test
    public void sendsExpandedEventWhenRestoringExpandedState() {
        Bundle bundle = new Bundle();
        bundle.putBoolean("player_expanded", false);

        controller.restoreState(bundle);

        PlayerUIEvent uiEvent = eventMonitor.verifyEventOn(EventQueue.PLAYER_UI);
        expect(uiEvent.getKind()).toEqual(PlayerUIEvent.PLAYER_COLLAPSED);
    }

    @Test
    public void sendsCollapsedEventWhenRestoringCollapsedState() {
        Bundle bundle = new Bundle();
        bundle.putBoolean("player_expanded", true);

        controller.restoreState(bundle);

        PlayerUIEvent uiEvent = eventMonitor.verifyEventOn(EventQueue.PLAYER_UI);
        expect(uiEvent.getKind()).toEqual(PlayerUIEvent.PLAYER_EXPANDED);
    }

    @Test
    public void setsCollapsedStateWhenPassingOverThreshold() {
        controller.onPanelSlide(layout, 0.4f);
        controller.onPanelSlide(layout, 0.6f);
        controller.onPanelSlide(layout, 0.7f);

        verify(actionBarController, times(1)).setVisible(true);
        PlayerUIEvent event = eventMonitor.verifyLastEventOn(EventQueue.PLAYER_UI);
        expect(event.getKind()).toEqual(PlayerUIEvent.PLAYER_COLLAPSED);
    }

    @Test
    public void setsExpandedStateWhenPassingUnderThreshold() {
        controller.onPanelSlide(layout, 0.6f);
        controller.onPanelSlide(layout, 0.4f);
        controller.onPanelSlide(layout, 0.3f);

        verify(actionBarController, times(1)).setVisible(false);
        PlayerUIEvent event = eventMonitor.verifyEventOn(EventQueue.PLAYER_UI);
        expect(event.getKind()).toEqual(PlayerUIEvent.PLAYER_EXPANDED);
    }

}