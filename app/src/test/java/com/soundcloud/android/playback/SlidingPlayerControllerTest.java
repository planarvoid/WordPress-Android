package com.soundcloud.android.playback;

import static com.soundcloud.android.Expect.expect;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import com.sothree.slidinguppanel.SlidingUpPanelLayout;
import com.soundcloud.android.R;
import com.soundcloud.android.actionbar.ActionBarController;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PlayerUIEvent;
import com.soundcloud.android.playback.service.PlayQueueManager;
import com.soundcloud.android.playback.ui.SlidingPlayerController;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.robolectric.TestEventBus;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.view.ViewTreeObserver;

@RunWith(SoundCloudTestRunner.class)
public class SlidingPlayerControllerTest {

    @Mock
    private PlayQueueManager playQueueManager;
    @Mock
    private ActionBarController actionBarController;
    @Mock
    private View layout;
    @Mock
    private Activity activity;
    @Mock
    private SlidingUpPanelLayout slidingPanel;
    @Mock
    private View playerView;

    private TestEventBus eventBus = new TestEventBus();
    private SlidingPlayerController controller;


    @Before
    public void setUp() throws Exception {
        controller = new SlidingPlayerController(playQueueManager, eventBus);
        when(activity.findViewById(R.id.sliding_layout)).thenReturn(slidingPanel);
        when(slidingPanel.findViewById(R.id.player_root)).thenReturn(playerView);
    }

    @Test
    public void configuresSlidingPanelOnAttach() {
        attachController();

        verify(slidingPanel).setPanelSlideListener(controller);
        verify(slidingPanel).setEnableDragViewTouchEvents(true);
    }

    @Test
    public void hidePlayerWhenPlayQueueIsEmpty() throws Exception {
        when(playQueueManager.isQueueEmpty()).thenReturn(true);
        attachController();

        controller.onResume();

        verify(playerView).setVisibility(View.GONE);
    }

    @Test
    public void doNotHidePlayerWhenPlayQueueIsNotEmpty() throws Exception {
        when(playQueueManager.isQueueEmpty()).thenReturn(false);
        attachController();

        controller.onResume();

        verify(playerView, never()).setVisibility(View.GONE);
    }

    @Test
    public void unsubscribesFromPlayerUIEvents() {
        attachController();
        controller.onResume();
        controller.onPause();

        eventBus.verifyUnsubscribed(EventQueue.PLAYER_UI);
    }

    @Test
    public void expandsPlayerWhenVisiblePlayTriggeredEventIsReceived() {
        attachController();
        controller.onResume();
        when(slidingPanel.isPaneVisible()).thenReturn(true);

        eventBus.publish(EventQueue.PLAYER_UI, PlayerUIEvent.forExpandPlayer());

        verify(slidingPanel).expandPane();
    }

    @Test
    public void showsFooterPlayerWhenHiddenAndPlayTriggeredEventIsReceived() throws Exception {
        when(playQueueManager.isQueueEmpty()).thenReturn(true);
        when(slidingPanel.isPaneVisible()).thenReturn(false);
        when(slidingPanel.getViewTreeObserver()).thenReturn(mock(ViewTreeObserver.class));
        attachController();
        controller.onResume();

        eventBus.publish(EventQueue.PLAYER_UI, PlayerUIEvent.forExpandPlayer());

        verify(slidingPanel).showPane();
    }

    @Test
    public void closesPlayerWhenPlayCloseEventIsReceived() {
        attachController();
        controller.onResume();
        eventBus.publish(EventQueue.PLAYER_UI, PlayerUIEvent.forCollapsePlayer());

        verify(slidingPanel).collapsePane();
    }

    @Test
    public void onlyRespondsToPlayTriggeredPlayerUIEvent() {
        attachController();
        controller.onResume();
        eventBus.publish(EventQueue.PLAYER_UI, PlayerUIEvent.fromPlayerCollapsed());

        verify(slidingPanel, times(0)).expandPane();
    }

    @Test
    public void doesntInteractWithActionBarIfBundleIsNullOnRestoreState() {
        controller.restoreState(null);

        verifyZeroInteractions(actionBarController);
    }

    @Test
    public void hidesActionBarIfExpandedStateStored() {
        attachController();
        Bundle bundle = new Bundle();
        bundle.putBoolean("player_expanded", true);

        controller.restoreState(bundle);

        verify(actionBarController).setVisible(false);
    }

    @Test
    public void storesExpandedStateInBundle() {
        attachController();
        when(slidingPanel.isExpanded()).thenReturn(true);
        Bundle bundle = new Bundle();

        controller.storeState(bundle);

        expect(bundle.getBoolean("player_expanded")).toBeTrue();
    }

    @Test
    public void sendsExpandedEventWhenRestoringExpandedState() {
        attachController();
        Bundle bundle = new Bundle();
        bundle.putBoolean("player_expanded", false);

        controller.restoreState(bundle);

        PlayerUIEvent uiEvent = eventBus.firstEventOn(EventQueue.PLAYER_UI);
        expect(uiEvent.getKind()).toEqual(PlayerUIEvent.PLAYER_COLLAPSED);
    }

    @Test
    public void sendsCollapsedEventWhenRestoringExpandedStateWithNullBundle() {
        controller.restoreState(null);

        PlayerUIEvent uiEvent = eventBus.firstEventOn(EventQueue.PLAYER_UI);
        expect(uiEvent.getKind()).toEqual(PlayerUIEvent.PLAYER_COLLAPSED);
    }

    @Test
    public void sendsCollapsedEventWhenRestoringCollapsedState() {
        attachController();
        Bundle bundle = new Bundle();
        bundle.putBoolean("player_expanded", true);

        controller.restoreState(bundle);

        PlayerUIEvent uiEvent = eventBus.lastEventOn(EventQueue.PLAYER_UI);
        expect(uiEvent.getKind()).toEqual(PlayerUIEvent.PLAYER_EXPANDED);
    }

    @Test
    public void setsCollapsedStateWhenPassingOverThreshold() {
        attachController();
        controller.onPanelSlide(layout, 0.4f);
        controller.onPanelSlide(layout, 0.6f);
        controller.onPanelSlide(layout, 0.7f);

        verify(actionBarController, times(1)).setVisible(true);
        PlayerUIEvent event = eventBus.lastEventOn(EventQueue.PLAYER_UI);
        expect(event.getKind()).toEqual(PlayerUIEvent.PLAYER_COLLAPSED);
    }

    @Test
    public void setsExpandedStateWhenPassingUnderThreshold() {
        attachController();
        controller.onPanelSlide(layout, 0.6f);
        controller.onPanelSlide(layout, 0.4f);
        controller.onPanelSlide(layout, 0.3f);

        verify(actionBarController, times(1)).setVisible(false);
        PlayerUIEvent event = eventBus.lastEventOn(EventQueue.PLAYER_UI);
        expect(event.getKind()).toEqual(PlayerUIEvent.PLAYER_EXPANDED);
    }

    private void attachController() {
        controller.attach(activity, actionBarController);
    }

}