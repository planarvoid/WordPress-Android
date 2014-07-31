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
import com.soundcloud.android.robolectric.TestHelper;
import com.soundcloud.android.rx.eventbus.TestEventBus;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import android.annotation.TargetApi;
import android.app.Activity;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.Window;

@RunWith(SoundCloudTestRunner.class)
public class SlidingPlayerControllerTest {

    @Mock private PlayQueueManager playQueueManager;
    @Mock private ActionBarController actionBarController;
    @Mock private View layout;
    @Mock private Activity activity;
    @Mock private SlidingUpPanelLayout slidingPanel;
    @Mock private View playerView;
    @Mock private Window window;
    @Mock private View decorView;

    private TestEventBus eventBus = new TestEventBus();
    private SlidingPlayerController controller;

    @Before
    public void setUp() throws Exception {
        TestHelper.setSdkVersion(Build.VERSION_CODES.ICE_CREAM_SANDWICH);
        controller = new SlidingPlayerController(playQueueManager, eventBus);
        when(activity.findViewById(R.id.sliding_layout)).thenReturn(slidingPanel);
        when(slidingPanel.findViewById(R.id.player_root)).thenReturn(playerView);
        when(activity.getWindow()).thenReturn(window);
        when(window.getDecorView()).thenReturn(decorView);
    }

    @Test
    public void configuresSlidingPanelOnAttach() {
        attachController();

        verify(slidingPanel).setPanelSlideListener(controller);
        verify(slidingPanel).setEnableDragViewTouchEvents(true);
    }

    @Test
    public void hidePlayerWhenPlayQueueIsEmpty() {
        when(playQueueManager.isQueueEmpty()).thenReturn(true);
        attachController();

        controller.onResume();

        verify(slidingPanel).hidePanel();
    }

    @Test
    public void doNotHidePlayerWhenPlayQueueIsNotEmpty() {
        when(playQueueManager.isQueueEmpty()).thenReturn(false);
        attachController();

        controller.onResume();

        verify(slidingPanel, never()).hidePanel();
    }

    @Test
    public void showPanelIfQueueHasItems() {
        when(playQueueManager.isQueueEmpty()).thenReturn(false);
        attachController();

        controller.onResume();

        verify(slidingPanel, never()).showPanel();
    }

    @Test
    public void doNotShowPanelIfItIsNotHidden() {
        when(playQueueManager.isQueueEmpty()).thenReturn(false);
        when(slidingPanel.isPanelHidden()).thenReturn(false);
        attachController();

        controller.onResume();

        verify(slidingPanel, never()).showPanel();
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
        when(slidingPanel.isPanelHidden()).thenReturn(false);

        eventBus.publish(EventQueue.PLAYER_UI, PlayerUIEvent.forExpandPlayer());

        verify(slidingPanel).expandPanel();
    }

    @Test
    public void showsFooterPlayerWhenHiddenAndPlayTriggeredEventIsReceived() {
        when(playQueueManager.isQueueEmpty()).thenReturn(true);
        when(slidingPanel.isPanelHidden()).thenReturn(true);
        when(slidingPanel.getViewTreeObserver()).thenReturn(mock(ViewTreeObserver.class));
        attachController();
        controller.onResume();

        eventBus.publish(EventQueue.PLAYER_UI, PlayerUIEvent.forExpandPlayer());

        verify(slidingPanel).expandPanel();
    }

    @Test
    public void closesPlayerWhenPlayCloseEventIsReceived() {
        attachController();
        controller.onResume();
        eventBus.publish(EventQueue.PLAYER_UI, PlayerUIEvent.forCollapsePlayer());

        verify(slidingPanel).collapsePanel();
    }

    @Test
    public void onlyRespondsToPlayTriggeredPlayerUIEvent() {
        attachController();
        controller.onResume();
        eventBus.publish(EventQueue.PLAYER_UI, PlayerUIEvent.fromPlayerCollapsed());

        verify(slidingPanel, times(0)).expandPanel();
    }

    @Test
    public void showPanelWhenShowPlayerEventIsReceived() {
        attachController();
        controller.onResume();

        eventBus.publish(EventQueue.PLAYER_UI, PlayerUIEvent.forShowPlayer());

        verify(slidingPanel).showPanel();
    }

    @Test
    public void doesntInteractWithActionBarIfBundleIsNullOnRestoreState() {
        controller.onCreate(null);

        verifyZeroInteractions(actionBarController);
    }

    @Test
    public void hidesActionBarIfExpandedStateStored() {
        attachController();
        Bundle bundle = new Bundle();
        bundle.putBoolean("player_expanded", true);

        controller.onCreate(bundle);

        verify(actionBarController).setVisible(false);
    }

    @Test
    public void storesExpandedStateInBundle() {
        attachController();
        when(slidingPanel.isPanelExpanded()).thenReturn(true);
        Bundle bundle = new Bundle();

        controller.onSaveInstanceState(bundle);

        expect(bundle.getBoolean("player_expanded")).toBeTrue();
    }

    @Test
    public void sendsExpandedEventWhenRestoringExpandedState() {
        attachController();
        Bundle bundle = new Bundle();
        bundle.putBoolean("player_expanded", false);

        controller.onCreate(bundle);

        PlayerUIEvent uiEvent = eventBus.firstEventOn(EventQueue.PLAYER_UI);
        expect(uiEvent.getKind()).toEqual(PlayerUIEvent.PLAYER_COLLAPSED);
    }

    @Test
    public void sendsCollapsedEventWhenRestoringExpandedStateWithNullBundle() {
        controller.onCreate(null);

        PlayerUIEvent uiEvent = eventBus.firstEventOn(EventQueue.PLAYER_UI);
        expect(uiEvent.getKind()).toEqual(PlayerUIEvent.PLAYER_COLLAPSED);
    }

    @Test
    public void sendsCollapsedEventWhenRestoringCollapsedState() {
        attachController();
        Bundle bundle = new Bundle();
        bundle.putBoolean("player_expanded", true);

        controller.onCreate (bundle);

        PlayerUIEvent uiEvent = eventBus.lastEventOn(EventQueue.PLAYER_UI);
        expect(uiEvent.getKind()).toEqual(PlayerUIEvent.PLAYER_EXPANDED);
    }

    @Test
    public void setsCollapsedStateWhenPassingOverThreshold() {
        attachController();
        collapsePanel();

        verify(actionBarController, times(1)).setVisible(true);
        PlayerUIEvent event = eventBus.lastEventOn(EventQueue.PLAYER_UI);
        expect(event.getKind()).toEqual(PlayerUIEvent.PLAYER_COLLAPSED);
    }

    @Test
    public void setsExpandedStateWhenPassingUnderThreshold() {
        attachController();
        expandPanel();

        verify(actionBarController, times(1)).setVisible(false);
        PlayerUIEvent event = eventBus.lastEventOn(EventQueue.PLAYER_UI);
        expect(event.getKind()).toEqual(PlayerUIEvent.PLAYER_EXPANDED);
    }

    @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
    @Test
    public void dimsSystemBarsWhenExpanded() {
        attachController();
        expandPanel();

        verify(decorView).setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE);
    }

    @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
    @Test
    public void clearsSystemUiFlagsWhenCollapsed() {
        attachController();
        collapsePanel();

        verify(decorView).setSystemUiVisibility(0);
    }

    @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
    @Test
    public void dimsSystemBarsWhenResumingToExpandedPlayer() {
        when(slidingPanel.isPanelExpanded()).thenReturn(true);
        attachController();

        controller.onResume();

        verify(decorView).setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE);
    }

    @Test
    public void doesNotSetSystemUiBeforeICS() {
        TestHelper.setSdkVersion(Build.VERSION_CODES.HONEYCOMB_MR2);
        attachController();

        expandPanel();

        verifyZeroInteractions(decorView);
    }

    @Test
    public void onBackPressedShouldDoNothingWhenPlayerIsCollapsed() {
        attachController();
        collapsePanel();

        expect(controller.handleBackPressed()).toBeFalse();
        verify(slidingPanel, never()).collapsePanel();
    }

    @Test
    public void onBackPressedShouldCollapsedWhenPlayerIsExpanded() {
        attachController();
        expandPanel();

        expect(controller.handleBackPressed()).toBeTrue();
        verify(slidingPanel).collapsePanel();
    }


    private void attachController() {
        controller.attach(activity, actionBarController);
    }

    private void collapsePanel() {
        controller.onPanelSlide(layout, 0.6f);
        controller.onPanelSlide(layout, 0.4f);
        controller.onPanelSlide(layout, 0.3f);
        when(slidingPanel.isPanelExpanded()).thenReturn(false);
    }

    private void expandPanel() {
        controller.onPanelSlide(layout, 0.4f);
        controller.onPanelSlide(layout, 0.6f);
        controller.onPanelSlide(layout, 0.7f);
        when(slidingPanel.isPanelExpanded()).thenReturn(true);
    }

}