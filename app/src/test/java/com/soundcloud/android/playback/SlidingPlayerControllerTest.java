package com.soundcloud.android.playback;

import static com.soundcloud.android.Expect.expect;
import static com.soundcloud.android.playback.ui.SlidingPlayerController.EXTRA_EXPAND_PLAYER;
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
import com.soundcloud.android.events.PlayerUICommand;
import com.soundcloud.android.events.PlayerUIEvent;
import com.soundcloud.android.playback.service.PlayQueueManager;
import com.soundcloud.android.playback.ui.PlayerFragment;
import com.soundcloud.android.playback.ui.SlidingPlayerController;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.robolectric.TestHelper;
import com.soundcloud.android.rx.eventbus.TestEventBus;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import android.annotation.TargetApi;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.Window;

@RunWith(SoundCloudTestRunner.class)
public class SlidingPlayerControllerTest {

    @Mock private PlayQueueManager playQueueManager;
    @Mock private ActionBarController actionBarController;
    @Mock private View layout;
    @Mock private FragmentActivity activity;
    @Mock private SlidingUpPanelLayout slidingPanel;
    @Mock private View playerView;
    @Mock private Window window;
    @Mock private View decorView;
    @Mock private FragmentManager fragmentManager;
    @Mock private PlayerFragment playerFragment;

    private TestEventBus eventBus = new TestEventBus();
    private SlidingPlayerController controller;

    @Before
    public void setUp() throws Exception {
        TestHelper.setSdkVersion(Build.VERSION_CODES.ICE_CREAM_SANDWICH);
        controller = new SlidingPlayerController(playQueueManager, eventBus);
        when(activity.findViewById(R.id.sliding_layout)).thenReturn(slidingPanel);
        when(activity.getWindow()).thenReturn(window);
        when(window.getDecorView()).thenReturn(decorView);
        when(activity.getSupportFragmentManager()).thenReturn(fragmentManager);
        when(fragmentManager.findFragmentById(R.id.player_root)).thenReturn(playerFragment);
        attachController();
    }

    @Test
    public void configuresSlidingPanelOnAttach() {
        verify(slidingPanel).setPanelSlideListener(controller);
        verify(slidingPanel).setEnableDragViewTouchEvents(true);
    }

    @Test
    public void hidePlayerWhenPlayQueueIsEmpty() {
        when(playQueueManager.isQueueEmpty()).thenReturn(true);

        controller.onResume();

        verify(slidingPanel).hidePanel();
    }

    @Test
    public void doNotHidePlayerWhenPlayQueueIsNotEmpty() {
        when(playQueueManager.isQueueEmpty()).thenReturn(false);

        controller.onResume();

        verify(slidingPanel, never()).hidePanel();
    }

    @Test
    public void showPanelIfQueueHasItems() {
        when(slidingPanel.isPanelHidden()).thenReturn(true);
        when(playQueueManager.isQueueEmpty()).thenReturn(false);

        controller.onResume();

        verify(slidingPanel).showPanel();
    }

    @Test
    public void doNotShowPanelIfItIsNotHidden() {
        when(playQueueManager.isQueueEmpty()).thenReturn(false);
        when(slidingPanel.isPanelHidden()).thenReturn(false);

        controller.onResume();

        verify(slidingPanel, never()).showPanel();
    }

    @Test
    public void unsubscribesFromPlayerUIEvents() {
        controller.onResume();
        controller.onPause();

        eventBus.verifyUnsubscribed(EventQueue.PLAYER_COMMAND);
    }

    @Test
    public void expandsPlayerWhenVisiblePlayTriggeredEventIsReceived() {
        controller.onResume();
        when(slidingPanel.isPanelHidden()).thenReturn(false);

        eventBus.publish(EventQueue.PLAYER_COMMAND, PlayerUICommand.expandPlayer());

        verify(slidingPanel).expandPanel();
    }

    @Test
    public void showsFooterPlayerWhenHiddenAndPlayTriggeredEventIsReceived() {
        when(playQueueManager.isQueueEmpty()).thenReturn(true);
        when(slidingPanel.isPanelHidden()).thenReturn(true);
        when(slidingPanel.getViewTreeObserver()).thenReturn(mock(ViewTreeObserver.class));

        controller.onResume();
        eventBus.publish(EventQueue.PLAYER_COMMAND, PlayerUICommand.expandPlayer());

        verify(slidingPanel).expandPanel();
    }

    @Test
    public void closesPlayerWhenPlayCloseEventIsReceived() {
        controller.onResume();
        eventBus.publish(EventQueue.PLAYER_COMMAND, PlayerUICommand.collapsePlayer());

        verify(slidingPanel).collapsePanel();
    }

    @Test
    public void onlyRespondsToPlayTriggeredPlayerUIEvent() {
        controller.onResume();
        eventBus.publish(EventQueue.PLAYER_UI, PlayerUIEvent.fromPlayerExpanded());

        verify(slidingPanel, times(0)).expandPanel();
    }

    @Test
    public void showPanelWhenShowPlayerEventIsReceived() {
        controller.onResume();

        eventBus.publish(EventQueue.PLAYER_COMMAND, PlayerUICommand.showPlayer());

        verify(slidingPanel).showPanel();
    }

    @Test
    public void doesntInteractWithActionBarIfBundleIsNullOnRestoreState() {
        controller.onCreate(null);

        verifyZeroInteractions(actionBarController);
    }

    @Test
    public void hidesActionBarIfExpandingStateStored() {
        when(slidingPanel.isPanelExpanded()).thenReturn(true);

        controller.onCreate(createBundleWithExpandingCommand());
        controller.onResume();

        verify(actionBarController).setVisible(false);
    }

    @Test
    public void storesExpandingStateInBundle() {
        when(slidingPanel.isPanelExpanded()).thenReturn(true);
        Bundle bundle = new Bundle();

        controller.onSaveInstanceState(bundle);

        expect(bundle.getBoolean(EXTRA_EXPAND_PLAYER)).toBeTrue();
    }

    @Test
    public void sendsCollapsedEventWhenCollapsedListenerCalled() {
        controller.onPanelCollapsed(mock(View.class));

        PlayerUIEvent uiEvent = eventBus.lastEventOn(EventQueue.PLAYER_UI);
        expect(uiEvent.getKind()).toEqual(PlayerUIEvent.PLAYER_COLLAPSED);
    }

    @Test
    public void sendsCollapsedEventWhenRestoringCollapsedState() {
        Bundle bundle = new Bundle();
        bundle.putBoolean(EXTRA_EXPAND_PLAYER, false);

        controller.onCreate(bundle);
        controller.onResume();

        PlayerUIEvent uiEvent = eventBus.lastEventOn(EventQueue.PLAYER_UI);
        expect(uiEvent.getKind()).toEqual(PlayerUIEvent.PLAYER_COLLAPSED);
    }

    @Test
    public void sendsExpandedEventWhenRestoringExpandedState() {
        controller.onCreate(createBundleWithExpandingCommand());
        controller.onResume();

        PlayerUIEvent uiEvent = eventBus.lastEventOn(EventQueue.PLAYER_UI);
        expect(uiEvent.getKind()).toEqual(PlayerUIEvent.PLAYER_EXPANDED);
    }

    @Test
    public void sendsExpandedPlayerEventWhenResumingToExpandedPlayer() {
        when(slidingPanel.isPanelExpanded()).thenReturn(true);

        controller.onResume();

        PlayerUIEvent event = eventBus.lastEventOn(EventQueue.PLAYER_UI);
        expect(event.getKind()).toEqual(PlayerUIEvent.PLAYER_EXPANDED);
    }

    @Test
    public void sendsCollapsedPlayerEventWhenResumingToCollapsedPlayer() {
        when(slidingPanel.isPanelExpanded()).thenReturn(false);

        controller.onResume();

        PlayerUIEvent event = eventBus.lastEventOn(EventQueue.PLAYER_UI);
        expect(event.getKind()).toEqual(PlayerUIEvent.PLAYER_COLLAPSED);
    }

    @Test
    public void onPlayerSlideReportsSlidePositionToPlayerFragment() {
        controller.onCreate(createBundleWithExpandingCommand());
        controller.onPanelSlide(slidingPanel, .33f);

        verify(playerFragment).onPlayerSlide(.33f);
    }

    @Test
    public void shouldExpandPlayerOnResumeIfIntentHasExtraCommand() {
        Intent intent = createIntentWithExpandingCommand();
        controller.onNewIntent(intent);
        controller.onResume();

        verify(slidingPanel).expandPanel();
    }

    @Test
    public void setsCollapsingStateWhenPassingOverThreshold() {
        collapsePanel();

        verify(actionBarController, times(1)).setVisible(true);
    }

    @Test
    public void setsExpandingStateWhenPassingUnderThreshold() {
        expandPanel();

        verify(actionBarController, times(1)).setVisible(false);
    }

    @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
    @Test
    public void dimsSystemBarsWhenExpanding() {
        expandPanel();

        verify(decorView).setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE);
    }

    @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
    @Test
    public void clearsSystemUiFlagsWhenCollapsed() {
        collapsePanel();

        verify(decorView).setSystemUiVisibility(0);
    }

    @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
    @Test
    public void dimsSystemBarsWhenResumingToExpandedPlayer() {
        when(slidingPanel.isPanelExpanded()).thenReturn(true);

        controller.onResume();

        verify(decorView).setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE);
    }

    @Test
    public void doesNotSetSystemUiBeforeICS() {
        TestHelper.setSdkVersion(Build.VERSION_CODES.HONEYCOMB_MR2);

        expandPanel();

        verifyZeroInteractions(decorView);
    }

    @Test
    public void onBackPressedShouldDoNothingWhenPlayerIsCollapsed() {
        collapsePanel();

        expect(controller.handleBackPressed()).toBeFalse();
        verify(slidingPanel, never()).collapsePanel();
    }

    @Test
    public void onBackPressedShouldCollapsedWhenPlayerIsExpanding() {
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

    private Intent createIntentWithExpandingCommand() {
        Intent intent = new Intent();
        intent.putExtra(EXTRA_EXPAND_PLAYER, true);
        return intent;
    }

    private Bundle createBundleWithExpandingCommand() {
        Bundle bundle = new Bundle();
        bundle.putBoolean(EXTRA_EXPAND_PLAYER, true);
        return bundle;
    }
}