package com.soundcloud.android.playback;

import static com.soundcloud.android.playback.ui.SlidingPlayerController.EXTRA_EXPAND_PLAYER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sothree.slidinguppanel.SlidingUpPanelLayout;
import com.sothree.slidinguppanel.SlidingUpPanelLayout.PanelState;
import com.soundcloud.android.R;
import com.soundcloud.android.ads.AdFixtures;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PlayerUICommand;
import com.soundcloud.android.events.PlayerUIEvent;
import com.soundcloud.android.events.TrackingEvent;
import com.soundcloud.android.events.UIEvent;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.ui.PlayerFragment;
import com.soundcloud.android.playback.ui.SlidingPlayerController;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.fixtures.TestPlayQueueItem;
import com.soundcloud.rx.eventbus.TestEventBus;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.Window;

public class SlidingPlayerControllerTest extends AndroidUnitTest {

    @Mock private PlayQueueManager playQueueManager;
    @Mock private View layout;
    @Mock private AppCompatActivity activity;
    @Mock private SlidingUpPanelLayout slidingPanel;
    @Mock private View playerView;
    @Mock private FragmentManager fragmentManager;
    @Mock private PlayerFragment playerFragment;
    @Mock private ActionBar actionBar;
    @Mock private Window window;

    private TestEventBus eventBus = new TestEventBus();
    private SlidingPlayerController controller;
    private View.OnTouchListener touchListener;

    @Before
    public void setUp() throws Exception {
        controller = new SlidingPlayerController(playQueueManager, resources(), eventBus);
        when(activity.findViewById(R.id.sliding_layout)).thenReturn(slidingPanel);
        when(activity.getSupportFragmentManager()).thenReturn(fragmentManager);
        when(activity.getSupportActionBar()).thenReturn(actionBar);
        when(fragmentManager.findFragmentById(R.id.player_root)).thenReturn(playerFragment);
        when(playerFragment.getActivity()).thenReturn(activity);
        when(activity.getWindow()).thenReturn(window);
        when(playQueueManager.getCurrentPlayQueueItem()).thenReturn(PlayQueueItem.EMPTY);
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

        controller.onResume(activity);

        verify(slidingPanel).setPanelState(PanelState.HIDDEN);
    }

    @Test
    public void doNotHidePlayerWhenPlayQueueIsNotEmpty() {
        when(playQueueManager.isQueueEmpty()).thenReturn(false);

        controller.onResume(activity);

        verify(slidingPanel, never()).setPanelState(PanelState.HIDDEN);
    }

    @Test
    public void showPanelIfQueueHasItems() {
        when(slidingPanel.getPanelState()).thenReturn(PanelState.HIDDEN);
        when(playQueueManager.isQueueEmpty()).thenReturn(false);

        controller.onResume(activity);

        verify(slidingPanel).setPanelState(PanelState.COLLAPSED);
    }

    @Test
    public void doNotShowPanelIfItIsNotHidden() {
        when(playQueueManager.isQueueEmpty()).thenReturn(false);
        when(slidingPanel.getPanelState()).thenReturn(PanelState.COLLAPSED);

        controller.onResume(activity);

        verify(slidingPanel, never()).setPanelState(PanelState.COLLAPSED);
        verify(slidingPanel, never()).setPanelState(PanelState.EXPANDED);
    }

    @Test
    public void unsubscribesFromPlayerUIEvents() {
        controller.onResume(activity);
        controller.onPause(activity);

        eventBus.verifyUnsubscribed(EventQueue.PLAYER_COMMAND);
    }

    @Test
    public void hidesPlayerWhenHideEventIsReceived() {
        controller.onResume(activity);

        eventBus.publish(EventQueue.PLAYER_COMMAND, PlayerUICommand.hidePlayer());

        verify(slidingPanel).setPanelState(PanelState.HIDDEN);
    }

    @Test
    public void expandsPlayerWhenVisiblePlayTriggeredEventIsReceived() {
        controller.onResume(activity);
        when(slidingPanel.getPanelState()).thenReturn(PanelState.COLLAPSED);

        eventBus.publish(EventQueue.PLAYER_COMMAND, PlayerUICommand.expandPlayer());

        verify(slidingPanel).setPanelState(PanelState.EXPANDED);
    }

    @Test
    public void showsFooterPlayerWhenHiddenAndPlayTriggeredEventIsReceived() {
        when(playQueueManager.isQueueEmpty()).thenReturn(true);
        when(slidingPanel.getPanelState()).thenReturn(PanelState.HIDDEN);
        when(slidingPanel.getViewTreeObserver()).thenReturn(mock(ViewTreeObserver.class));

        controller.onResume(activity);
        eventBus.publish(EventQueue.PLAYER_COMMAND, PlayerUICommand.expandPlayer());

        verify(slidingPanel).setPanelState(PanelState.EXPANDED);
    }

    @Test
    public void locksPlayerAsExpandedWhenLockEventIsReceived() {
        controller.onResume(activity);
        eventBus.publish(EventQueue.PLAYER_COMMAND, PlayerUICommand.lockPlayerExpanded());

        verify(slidingPanel).setTouchEnabled(false);
        verify(slidingPanel).setPanelState(PanelState.EXPANDED);
    }

    @Test
    public void locksWhenPlayQueueLockEventIsReceived() {
        controller.onResume(activity);
        eventBus.publish(EventQueue.PLAYER_COMMAND, PlayerUICommand.lockPlayQueue());
        eventBus.publish(EventQueue.PLAYER_COMMAND, PlayerUICommand.unlockPlayQueue());

        verify(slidingPanel).setTouchEnabled(false);
        verify(slidingPanel).setPanelState(PanelState.EXPANDED);
    }

    @Test
    public void unlocksWhenPlayQueueUnLockEventIsReceived() {
        controller.onResume(activity);
        eventBus.publish(EventQueue.PLAYER_COMMAND, PlayerUICommand.lockPlayQueue());
        eventBus.publish(EventQueue.PLAYER_COMMAND, PlayerUICommand.unlockPlayQueue());

        verify(slidingPanel).setTouchEnabled(true);
        verify(slidingPanel).setPanelState(PanelState.EXPANDED);
    }

    @Test
    public void doesNotUnlockWhenPlayQueueLockIsRecieved() {
        controller.onResume(activity);
        eventBus.publish(EventQueue.PLAYER_COMMAND, PlayerUICommand.lockPlayQueue());
        eventBus.publish(EventQueue.PLAYER_COMMAND, PlayerUICommand.unlockPlayer());

        verify(slidingPanel).setTouchEnabled(false);
        verify(slidingPanel).setPanelState(PanelState.EXPANDED);
    }


    @Test
    public void locksPlayerDoesntExpandAlreadyExpandedPlayerWhenLockEventIsReceived() {
        when(slidingPanel.getPanelState()).thenReturn(PanelState.EXPANDED);
        controller.onResume(activity);
        eventBus.publish(EventQueue.PLAYER_COMMAND, PlayerUICommand.lockPlayerExpanded());

        verify(slidingPanel, never()).setPanelState(PanelState.EXPANDED);
        verify(slidingPanel).setTouchEnabled(false);
    }

    @Test
    public void unlocksPlayerWhenUnlockEventIsReceived() {
        controller.onResume(activity);
        eventBus.publish(EventQueue.PLAYER_COMMAND, PlayerUICommand.unlockPlayer());

        verify(slidingPanel).setTouchEnabled(true);
    }

    @Test
    public void closesPlayerWhenPlayCloseEventIsReceived() {
        controller.onResume(activity);
        eventBus.publish(EventQueue.PLAYER_COMMAND, PlayerUICommand.collapsePlayer());

        verify(slidingPanel).setPanelState(PanelState.COLLAPSED);
    }

    @Test
    public void onlyRespondsToPlayTriggeredPlayerUIEvent() {
        controller.onResume(activity);
        eventBus.publish(EventQueue.PLAYER_UI, PlayerUIEvent.fromPlayerExpanded());

        verify(slidingPanel, times(0)).setPanelState(PanelState.EXPANDED);
    }

    @Test
    public void storesExpandingStateInBundle() {
        when(slidingPanel.getPanelState()).thenReturn(PanelState.EXPANDED);
        Bundle bundle = new Bundle();

        controller.onSaveInstanceState(activity, bundle);

        assertThat(bundle.getBoolean(EXTRA_EXPAND_PLAYER)).isTrue();
    }

    @Test
    public void sendsCollapsedEventWhenCollapsedListenerCalled() {
        controller.onPanelCollapsed(mock(View.class));

        PlayerUIEvent uiEvent = eventBus.lastEventOn(EventQueue.PLAYER_UI);
        assertThat(uiEvent.getKind()).isEqualTo(PlayerUIEvent.PLAYER_COLLAPSED);
    }

    @Test
    public void emitsClosePlayerFromSlideWhenCollapsedAfterDragging() {
        touchListener.onTouch(slidingPanel, MotionEvent.obtain(0, 0, MotionEvent.ACTION_DOWN, 0, 0, 0));
        touchListener.onTouch(slidingPanel, MotionEvent.obtain(0, 0, MotionEvent.ACTION_UP, 0, 0, 0));

        controller.onPanelCollapsed(slidingPanel);

        TrackingEvent event = eventBus.lastEventOn(EventQueue.TRACKING);
        UIEvent expected = UIEvent.fromPlayerClose(true);
        assertThat(event.getKind()).isEqualTo(expected.getKind());
        assertThat(event.getAttributes()).isEqualTo(expected.getAttributes());
    }

    @Test
    public void emitsOpenPlayerFromFooterSlideWhenExpandedAfterDragging() {
        touchListener.onTouch(slidingPanel, MotionEvent.obtain(0, 0, MotionEvent.ACTION_DOWN, 0, 0, 0));
        touchListener.onTouch(slidingPanel, MotionEvent.obtain(0, 0, MotionEvent.ACTION_UP, 0, 0, 0));

        controller.onPanelExpanded(slidingPanel);

        TrackingEvent event = eventBus.lastEventOn(EventQueue.TRACKING);
        UIEvent expected = UIEvent.fromPlayerOpen(true);
        assertThat(event.getKind()).isEqualTo(expected.getKind());
        assertThat(event.getAttributes()).isEqualTo(expected.getAttributes());
    }

    @Test
    public void sendsExpandedEventWhenRestoringExpandedState() {
        controller.onCreate(activity, createBundleWithExpandingCommand());
        controller.onResume(activity);

        PlayerUIEvent uiEvent = eventBus.lastEventOn(EventQueue.PLAYER_UI);
        assertThat(uiEvent.getKind()).isEqualTo(PlayerUIEvent.PLAYER_EXPANDED);
    }

    @Test
    public void onPlayerSlideReportsSlidePositionToPlayerFragment() {
        controller.onCreate(activity, createBundleWithExpandingCommand());
        controller.onPanelSlide(slidingPanel, .33f);

        verify(playerFragment).onPlayerSlide(.33f);
    }

    @Test
    public void shouldExpandPlayerOnResumeIfIntentHasExtraCommand() {
        Intent intent = createIntentWithExpandingCommand();
        controller.onNewIntent(activity, intent);
        controller.onResume(activity);

        verify(slidingPanel).setPanelState(PanelState.EXPANDED);
    }

    @Test
    public void shouldExpandAndLockPlayerOnResumeIfCurrentItemIsVideoAd() {
        when(playQueueManager.getCurrentPlayQueueItem())
                .thenReturn(TestPlayQueueItem.createVideo(AdFixtures.getVideoAd(Urn.forTrack(123L))));
        when(slidingPanel.getPanelState()).thenReturn(PanelState.EXPANDED);
        controller.onResume(activity);

        verify(slidingPanel).setPanelState(PanelState.EXPANDED);
        verify(slidingPanel).setTouchEnabled(false);
    }

    @Test
    public void shouldExpandAndLockPlayerOnResumeIfPlaylistLock() {
        when(slidingPanel.getPanelState()).thenReturn(PanelState.EXPANDED);
        controller.onResume(activity);
        eventBus.publish(EventQueue.PLAYER_COMMAND, PlayerUICommand.lockPlayQueue());

        assertThat(slidingPanel.getPanelState()).isEqualTo(PanelState.EXPANDED);
        verify(slidingPanel).setTouchEnabled(false);
    }

    @Test
    public void onBackPressedShouldDoNothingWhenPlayerIsLocked() {
        assertThat(controller.handleBackPressed()).isFalse();
        verify(slidingPanel, never()).setPanelState(any(PanelState.class));
    }

    @Test
    public void onBackPressedShouldDoNothingWhenPlayerIsCollapsed() {
        collapsePanel();

        assertThat(controller.handleBackPressed()).isFalse();
        verify(slidingPanel, never()).setPanelState(PanelState.COLLAPSED);
    }

    @Test
    public void onBackPressedShouldCollapsedWhenPlayerIsExpanding() {
        expandPanel();

        assertThat(controller.handleBackPressed()).isTrue();
        verify(slidingPanel).setPanelState(PanelState.COLLAPSED);
    }

    @Test
    public void shouldNotReceiveEventsAfterPause() {
        controller.onPause(null);
        eventBus.publish(EventQueue.PLAYER_COMMAND, PlayerUICommand.expandPlayer());
        verify(slidingPanel, never()).setPanelState(PanelState.EXPANDED);
    }

    @Test
    public void shouldReceiveEventsAfterPauseAndResume() {
        controller.onPause(null);
        controller.onResume(activity);
        eventBus.publish(EventQueue.PLAYER_COMMAND, PlayerUICommand.expandPlayer());
        verify(slidingPanel).setPanelState(PanelState.EXPANDED);
    }

    private void attachController() {
        ArgumentCaptor<View.OnTouchListener> touchListenerArgumentCaptor = ArgumentCaptor.forClass(View.OnTouchListener.class);

        controller.onCreate(activity, null);

        verify(slidingPanel).setOnTouchListener(touchListenerArgumentCaptor.capture());

        touchListener = touchListenerArgumentCaptor.getValue();
    }

    private void collapsePanel() {
        controller.onPanelSlide(layout, 0.6f);
        controller.onPanelSlide(layout, 0.4f);
        controller.onPanelSlide(layout, 0.3f);
        when(slidingPanel.getPanelState()).thenReturn(PanelState.COLLAPSED);
    }

    private void expandPanel() {
        controller.onPanelSlide(layout, 0.4f);
        controller.onPanelSlide(layout, 0.6f);
        controller.onPanelSlide(layout, 0.7f);
        when(slidingPanel.getPanelState()).thenReturn(PanelState.EXPANDED);
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
