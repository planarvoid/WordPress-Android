package com.soundcloud.android.playback.ui;

import static com.soundcloud.android.playback.ui.SlidingPlayerController.EXTRA_EXPAND_PLAYER;
import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.R;
import com.soundcloud.android.ads.AdFixtures;
import com.soundcloud.android.analytics.performance.MetricType;
import com.soundcloud.android.analytics.performance.PerformanceMetricsEngine;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PlayerUICommand;
import com.soundcloud.android.events.PlayerUIEvent;
import com.soundcloud.android.events.UIEvent;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.PlayQueueItem;
import com.soundcloud.android.playback.PlayQueueManager;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.fixtures.TestPlayQueueItem;
import com.soundcloud.android.view.behavior.LockableBottomSheetBehavior;
import com.soundcloud.android.view.status.StatusBarColorController;
import com.soundcloud.rx.eventbus.TestEventBus;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.BottomSheetBehavior;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.Window;

public class SlidingPlayerControllerTest extends AndroidUnitTest {

    @Mock private PlayQueueManager playQueueManager;
    @Mock private View layout;
    @Mock private StatusBarColorController statusBarColorController;
    @Mock private AppCompatActivity activity;
    @Mock private LockableBottomSheetBehavior<View> playerBehavior;
    @Mock private View playerView;
    @Mock private FragmentManager fragmentManager;
    @Mock private PlayerFragment playerFragment;
    @Mock private ActionBar actionBar;
    @Mock private Window window;
    @Mock private PerformanceMetricsEngine performanceMetricsEngine;
    @Mock private LockableBottomSheetBehavior.Factory lockableBottomSheetBehaviorFactory;
    @Mock private ViewTreeObserver viewTreeObserver;

    private TestEventBus eventBus = new TestEventBus();
    private SlidingPlayerController controller;

    @Before
    public void setUp() throws Exception {
        controller = new SlidingPlayerController(playQueueManager, eventBus, statusBarColorController, performanceMetricsEngine, lockableBottomSheetBehaviorFactory);

        when(lockableBottomSheetBehaviorFactory.from(any())).thenReturn(playerBehavior);
        when(playerView.getViewTreeObserver()).thenReturn(viewTreeObserver);

        when(activity.getSupportFragmentManager()).thenReturn(fragmentManager);
        when(activity.getSupportActionBar()).thenReturn(actionBar);
        when(activity.findViewById(R.id.player_root)).thenReturn(playerView);

        when(fragmentManager.findFragmentById(R.id.player_root)).thenReturn(playerFragment);

        when(playerFragment.getActivity()).thenReturn(activity);
        when(activity.getWindow()).thenReturn(window);
        when(playQueueManager.getCurrentPlayQueueItem()).thenReturn(PlayQueueItem.EMPTY);
        controller.onCreate(activity, null);
    }

    @Test
    public void configuresSlidingPanelOnAttach() {
        verify(playerBehavior).setBottomSheetCallback(isA(BottomSheetBehavior.BottomSheetCallback.class));
    }

    @Test
    public void hidePlayerWhenPlayQueueIsEmpty() {
        when(playQueueManager.isQueueEmpty()).thenReturn(true);

        controller.onResume(activity);

        verify(playerBehavior).setState(BottomSheetBehavior.STATE_HIDDEN);
    }

    @Test
    public void doNotHidePlayerWhenPlayQueueIsNotEmpty() {
        when(playQueueManager.isQueueEmpty()).thenReturn(false);

        controller.onResume(activity);

        verify(playerBehavior, never()).setState(BottomSheetBehavior.STATE_HIDDEN);
    }

    @Test
    public void showPanelIfQueueHasItems() {
        when(playerBehavior.getState()).thenReturn(BottomSheetBehavior.STATE_HIDDEN);
        when(playQueueManager.isQueueEmpty()).thenReturn(false);

        controller.onResume(activity);

        verify(playerBehavior).setState(BottomSheetBehavior.STATE_COLLAPSED);
    }

    @Test
    public void restoreCollapsedPlayerStateOnResume() {
        when(playQueueManager.isQueueEmpty()).thenReturn(false);
        when(playerBehavior.getState()).thenReturn(BottomSheetBehavior.STATE_COLLAPSED);

        controller.onResume(activity);

        verify(playerBehavior).setState(BottomSheetBehavior.STATE_COLLAPSED);
        verify(playerBehavior, never()).setState(BottomSheetBehavior.STATE_EXPANDED);
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

        verify(playerBehavior).setState(BottomSheetBehavior.STATE_HIDDEN);
    }

    @Test
    public void expandsPlayerWhenVisiblePlayTriggeredEventIsReceived() {
        controller.onResume(activity);
        when(playerBehavior.getState()).thenReturn(BottomSheetBehavior.STATE_COLLAPSED);

        eventBus.publish(EventQueue.PLAYER_COMMAND, PlayerUICommand.expandPlayer());

        verify(playerBehavior).setState(BottomSheetBehavior.STATE_EXPANDED);
    }

    @Test
    public void showsFooterPlayerWhenHiddenAndPlayTriggeredEventIsReceived() {
        when(playQueueManager.isQueueEmpty()).thenReturn(true);
        when(playerBehavior.getState()).thenReturn(BottomSheetBehavior.STATE_HIDDEN);
        //when(slidingPanel.getViewTreeObserver()).thenReturn(mock(ViewTreeObserver.class));

        controller.onResume(activity);
        eventBus.publish(EventQueue.PLAYER_COMMAND, PlayerUICommand.expandPlayer());

        verify(playerBehavior).setState(BottomSheetBehavior.STATE_EXPANDED);
    }

    @Test
    public void locksPlayerAsExpandedWhenLockEventIsReceived() {
        controller.onResume(activity);
        eventBus.publish(EventQueue.PLAYER_COMMAND, PlayerUICommand.lockPlayerExpanded());

        verify(playerBehavior).setLocked(true);
        verify(playerBehavior).setState(BottomSheetBehavior.STATE_EXPANDED);
    }

    @Test
    public void locksWhenPlayQueueLockEventIsReceived() {
        controller.onResume(activity);
        eventBus.publish(EventQueue.PLAYER_COMMAND, PlayerUICommand.lockPlayQueue());
        eventBus.publish(EventQueue.PLAYER_COMMAND, PlayerUICommand.unlockPlayQueue());

        verify(playerBehavior).setLocked(true);
        verify(playerBehavior).setState(BottomSheetBehavior.STATE_EXPANDED);
    }

    @Test
    public void unlocksWhenPlayQueueUnLockEventIsReceived() {
        controller.onResume(activity);
        eventBus.publish(EventQueue.PLAYER_COMMAND, PlayerUICommand.lockPlayQueue());
        eventBus.publish(EventQueue.PLAYER_COMMAND, PlayerUICommand.unlockPlayQueue());

        verify(playerBehavior).setLocked(false);
        verify(playerBehavior).setState(BottomSheetBehavior.STATE_EXPANDED);
    }

    @Test
    public void doesNotUnlockWhenPlayQueueLockIsRecieved() {
        controller.onResume(activity);
        eventBus.publish(EventQueue.PLAYER_COMMAND, PlayerUICommand.lockPlayQueue());
        eventBus.publish(EventQueue.PLAYER_COMMAND, PlayerUICommand.unlockPlayer());

        verify(playerBehavior).setLocked(true);
        verify(playerBehavior).setState(BottomSheetBehavior.STATE_EXPANDED);
    }


    @Test
    public void locksPlayerDoesntExpandAlreadyExpandedPlayerWhenLockEventIsReceived() {
        when(playerBehavior.getState()).thenReturn(BottomSheetBehavior.STATE_EXPANDED);
        controller.onResume(activity);
        eventBus.publish(EventQueue.PLAYER_COMMAND, PlayerUICommand.lockPlayerExpanded());

        verify(playerBehavior, never()).setState(BottomSheetBehavior.STATE_EXPANDED);
        verify(playerBehavior).setLocked(true);
    }

    @Test
    public void unlocksPlayerWhenUnlockEventIsReceived() {
        controller.onResume(activity);
        eventBus.publish(EventQueue.PLAYER_COMMAND, PlayerUICommand.unlockPlayer());

        verify(playerBehavior).setLocked(false);
    }

    @Test
    public void closesPlayerWhenCollapsePlayerAutomaticallyEventIsReceived() {
        controller.onResume(activity);
        eventBus.publish(EventQueue.PLAYER_COMMAND, PlayerUICommand.collapsePlayerAutomatically());

        verify(playerBehavior, times(2)).setState(BottomSheetBehavior.STATE_COLLAPSED);
    }

    @Test
    public void closesPlayerWhenCollapsePlayerManuallyEventIsReceived() {
        controller.onResume(activity);
        eventBus.publish(EventQueue.PLAYER_COMMAND, PlayerUICommand.collapsePlayerManually());

        verify(playerBehavior, times(2)).setState(BottomSheetBehavior.STATE_COLLAPSED);
    }

    @Test
    public void onlyRespondsToPlayTriggeredPlayerUIEvent() {
        controller.onResume(activity);
        eventBus.publish(EventQueue.PLAYER_UI, PlayerUIEvent.fromPlayerExpanded());

        verify(playerBehavior, times(0)).setState(BottomSheetBehavior.STATE_EXPANDED);
    }

    @Test
    public void storesExpandingStateInBundle() {
        when(playerBehavior.getState()).thenReturn(BottomSheetBehavior.STATE_EXPANDED);
        Bundle bundle = new Bundle();

        controller.onSaveInstanceState(activity, bundle);

        assertThat(bundle.getBoolean(EXTRA_EXPAND_PLAYER)).isTrue();
    }

    @Test
    public void sendsCollapsedEventWhenCollapsedListenerCalled() {
        controller.onPanelCollapsed();

        PlayerUIEvent uiEvent = eventBus.lastEventOn(EventQueue.PLAYER_UI);
        assertThat(uiEvent.getKind()).isEqualTo(PlayerUIEvent.PLAYER_COLLAPSED);
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
        controller.onPanelSlide(playerView, .33f);

        verify(playerFragment).onPlayerSlide(.33f);
    }

    @Test
    public void shouldExpandPlayerOnResumeIfIntentHasExtraCommand() {
        Intent intent = createIntentWithExpandingCommand();
        controller.onNewIntent(activity, intent);
        controller.onResume(activity);

        verify(playerBehavior).setState(BottomSheetBehavior.STATE_EXPANDED);
    }

    @Test
    public void shouldExpandAndLockPlayerOnResumeIfCurrentItemIsVideoAd() {
        when(playQueueManager.getCurrentPlayQueueItem())
                .thenReturn(TestPlayQueueItem.createVideo(AdFixtures.getVideoAd(Urn.forTrack(123L))));
        when(playerBehavior.getState()).thenReturn(BottomSheetBehavior.STATE_EXPANDED);
        controller.onResume(activity);

        verify(playerBehavior).setState(BottomSheetBehavior.STATE_EXPANDED);
        verify(playerBehavior).setLocked(true);
    }

    @Test
    public void shouldExpandAndLockPlayerOnResumeIfPlaylistLock() {
        when(playerBehavior.getState()).thenReturn(BottomSheetBehavior.STATE_EXPANDED);
        controller.onResume(activity);
        eventBus.publish(EventQueue.PLAYER_COMMAND, PlayerUICommand.lockPlayQueue());

        assertThat(playerBehavior.getState()).isEqualTo(BottomSheetBehavior.STATE_EXPANDED);
        verify(playerBehavior).setLocked(true);
    }

    @Test
    public void onBackPressedShouldDoNothingWhenPlayerIsLocked() {
        assertThat(controller.handleBackPressed()).isFalse();
        verify(playerBehavior, never()).setState(anyInt());
    }

    @Test
    public void onBackPressedShouldDoNothingWhenPlayerIsCollapsed() {
        collapsePanel();

        assertThat(controller.handleBackPressed()).isFalse();
        verify(playerBehavior, never()).setState(BottomSheetBehavior.STATE_COLLAPSED);
    }

    @Test
    public void onBackPressedShouldCollapsedWhenPlayerIsExpanding() {
        expandPanel();

        assertThat(controller.handleBackPressed()).isTrue();
        verify(playerBehavior).setState(BottomSheetBehavior.STATE_COLLAPSED);
    }

    @Test
    public void onBackPressedShouldCollapsePlayQueue() {
        when(playerBehavior.getState()).thenReturn(BottomSheetBehavior.STATE_EXPANDED);
        controller.onResume(activity);
        eventBus.publish(EventQueue.PLAYER_COMMAND, PlayerUICommand.lockPlayQueue());

        assertThat(controller.handleBackPressed()).isTrue();

        assertThat(playerBehavior.getState()).isEqualTo(BottomSheetBehavior.STATE_EXPANDED);
        verify(playerBehavior).setLocked(false);
    }

    @Test
    public void onBackPressedShouldTrackPlayerClose() {
        when(playerBehavior.getState()).thenReturn(BottomSheetBehavior.STATE_EXPANDED);
        controller.onResume(activity);
        eventBus.publish(EventQueue.PLAYER_COMMAND, PlayerUICommand.lockPlayQueue());

        controller.handleBackPressed();

        final UIEvent trackingEvent = (UIEvent) eventBus.lastEventOn(EventQueue.TRACKING);
        assertThat(trackingEvent.kind()).isEqualTo(UIEvent.Kind.PLAY_QUEUE_CLOSE);
    }


    @Test
    public void shouldNotReceiveEventsAfterPause() {
        controller.onPause(null);
        eventBus.publish(EventQueue.PLAYER_COMMAND, PlayerUICommand.expandPlayer());
        verify(playerBehavior, never()).setState(BottomSheetBehavior.STATE_EXPANDED);
    }

    @Test
    public void shouldReceiveEventsAfterPauseAndResume() {
        controller.onPause(null);
        controller.onResume(activity);
        eventBus.publish(EventQueue.PLAYER_COMMAND, PlayerUICommand.expandPlayer());
        verify(playerBehavior).setState(BottomSheetBehavior.STATE_EXPANDED);
    }

    @Test
    public void shouldEndMeasuringOnPlayerExpanded() {
        controller.onPanelExpanded();

        verify(performanceMetricsEngine).endMeasuring(MetricType.TIME_TO_EXPAND_PLAYER);
    }

    @Test
    public void shouldBeHideableWhenToHidden() {
        controller.onResume(activity);

        eventBus.publish(EventQueue.PLAYER_COMMAND, PlayerUICommand.hidePlayer());

        verify(playerBehavior).setHideable(true);
    }

    @Test
    public void shouldBeNotHideableWhenExpanded() {
        controller.onResume(activity);

        controller.onPanelExpanded();

        verify(playerBehavior).setHideable(false);
    }

    @Test
    public void shouldBeNotHideableWhenCollapsed() {
        controller.onResume(activity);

        controller.onPanelCollapsed();

        verify(playerBehavior).setHideable(false);
    }

    private void collapsePanel() {
        controller.onPanelSlide(layout, 0.6f);
        controller.onPanelSlide(layout, 0.4f);
        controller.onPanelSlide(layout, 0.3f);
        when(playerBehavior.getState()).thenReturn(BottomSheetBehavior.STATE_COLLAPSED);
    }

    private void expandPanel() {
        controller.onPanelSlide(layout, 0.4f);
        controller.onPanelSlide(layout, 0.6f);
        controller.onPanelSlide(layout, 0.7f);
        when(playerBehavior.getState()).thenReturn(BottomSheetBehavior.STATE_EXPANDED);
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
