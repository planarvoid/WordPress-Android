package com.soundcloud.android.playback.ui;

import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import com.soundcloud.android.ads.AdConstants;
import com.soundcloud.android.events.CurrentPlayQueueTrackEvent;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PlayQueueEvent;
import com.soundcloud.android.events.PlaybackProgressEvent;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.PlaybackOperations;
import com.soundcloud.android.playback.PlaybackProgress;
import com.soundcloud.android.playback.service.PlayQueueManager;
import com.soundcloud.android.playback.ui.view.PlayerTrackPager;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.rx.eventbus.TestEventBus;
import com.soundcloud.android.tracks.TrackUrn;
import com.xtremelabs.robolectric.Robolectric;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;

import android.support.v4.view.ViewPager;
import android.view.View;

@RunWith(SoundCloudTestRunner.class)
public class PlayerPagerControllerTest {

    @Mock private TrackPagerAdapter adapter;
    @Mock private PlayerPresenter presenter;
    @Mock private PlayQueueManager playQueueManager;
    @Mock private PlaybackOperations playbackOperations;
    @Mock private View container;
    @Mock private PlayerTrackPager viewPager;

    private PlayerPagerController controller;
    private TestEventBus eventBus = new TestEventBus();

    @Before
    public void setUp() {
        controller = new PlayerPagerController(adapter, presenter, eventBus,
                playQueueManager, playbackOperations, null);
        when(playQueueManager.getCurrentPosition()).thenReturn(1);
        when(container.findViewById(anyInt())).thenReturn(viewPager);
        when(viewPager.getContext()).thenReturn(Robolectric.application);
        controller.onViewCreated(container);
    }

    @Test
    public void setPagerInitializesCurrentPosition() {
        when(playQueueManager.getCurrentPosition()).thenReturn(3);

        controller.onViewCreated(container);

        verify(viewPager).setCurrentItem(eq(3), anyBoolean());
    }

    @Test
    public void onPlayQueueEventForTrackChangeUpdatesPagerAndAdapter() {
        when(playQueueManager.getCurrentPosition()).thenReturn(3);

        eventBus.publish(EventQueue.PLAY_QUEUE_TRACK, CurrentPlayQueueTrackEvent.fromNewQueue(TrackUrn.NOT_SET));

        verify(viewPager).setCurrentItem(eq(3), anyBoolean());
    }

    @Test
    public void onPlayQueueEventForNewQueueUpdatesPagerAndAdapter() {
        when(playQueueManager.getCurrentPosition()).thenReturn(3);

        eventBus.publish(EventQueue.PLAY_QUEUE_TRACK, CurrentPlayQueueTrackEvent.fromNewQueue(Urn.forTrack(123L)));

        verify(viewPager).setCurrentItem(eq(3), anyBoolean());
    }

    @Test
    public void onPlayQueueEventShouldNotNotifyDataSetChangeOnTrackChange() {
        when(playQueueManager.getCurrentPosition()).thenReturn(3);

        eventBus.publish(EventQueue.PLAY_QUEUE_TRACK, CurrentPlayQueueTrackEvent.fromPositionChanged(Urn.forTrack(123L)));

        verify(adapter, never()).notifyDataSetChanged();
    }

    @Test
    public void playQueueChangeAnimatesToAdjacentTracks() throws Exception {
        when(viewPager.getCurrentItem()).thenReturn(2);
        when(playQueueManager.getCurrentPosition()).thenReturn(3);

        eventBus.publish(EventQueue.PLAY_QUEUE_TRACK, CurrentPlayQueueTrackEvent.fromPositionChanged(TrackUrn.NOT_SET));

        verify(viewPager).setCurrentItem(3, true);
    }

    @Test
    public void playQueueChangeDoesNotAnimateToNotAdjacentTracks() throws Exception {
        when(viewPager.getCurrentItem()).thenReturn(2);
        when(playQueueManager.getCurrentPosition()).thenReturn(4);

        eventBus.publish(EventQueue.PLAY_QUEUE_TRACK, CurrentPlayQueueTrackEvent.fromNewQueue(TrackUrn.NOT_SET));

        verify(viewPager).setCurrentItem(4, false);
    }

    @Test
    public void shouldUnsubscribeFromQueuesOnUnsubscribe() {
        controller.onDestroyView();

        eventBus.verifyUnsubscribed();
    }

    @Test
    public void onPlayQueueChangedNotifiesDataSetChangedOnAdapter() {
        eventBus.publish(EventQueue.PLAY_QUEUE, PlayQueueEvent.fromNewQueue());

        verify(adapter).notifyDataSetChanged();
    }

    @Test
    public void onPlayQueueChangedSetsTrackPagerAdapterIfNotSet() {
        eventBus.publish(EventQueue.PLAY_QUEUE, PlayQueueEvent.fromNewQueue());

        verify(viewPager).setAdapter(adapter);
    }

    @Test // Fixes issue #2045, should not happen after we implement invalidating event queues on logout
    public void onPlayQueueChangedDoesNotExpandPlayerWhenPlayQueueIsEmpty() {
        when(playQueueManager.isQueueEmpty()).thenReturn(true);

        eventBus.publish(EventQueue.PLAY_QUEUE_TRACK, CurrentPlayQueueTrackEvent.fromNewQueue(Urn.forTrack(1l)));

        eventBus.verifyNoEventsOn(EventQueue.PLAYER_UI);
    }

    @Test
    public void onPlayQueueUpdateSetsTrackPagerAdapterIfNotSet() {
        eventBus.publish(EventQueue.PLAY_QUEUE, PlayQueueEvent.fromQueueUpdate());

        verify(viewPager).setAdapter(adapter);
    }

    @Test
    public void changesPlayQueuePositionWhenInForegroundOnIdleStateAfterPageSelected() {
        when(viewPager.getCurrentItem()).thenReturn(2);
        controller.onResume();
        controller.onPageSelected(2);
        controller.onPageScrollStateChanged(ViewPager.SCROLL_STATE_IDLE);
        Robolectric.runUiThreadTasksIncludingDelayedTasks();
        verify(playbackOperations).setPlayQueuePosition(2);
    }

    @Test
    public void doesNotChangePlayQueuePositionIfNeverInForegroundOnIdleStateAfterPageSelected() {
        when(viewPager.getCurrentItem()).thenReturn(2);
        controller.onPageSelected(2);
        controller.onPageScrollStateChanged(ViewPager.SCROLL_STATE_IDLE);
        verify(playbackOperations, never()).setPlayQueuePosition(anyInt());
    }

    @Test
    public void doesNotChangePlayQueuePositionWhenInBackgroundOnIdleStateAfterPageSelected() {
        when(viewPager.getCurrentItem()).thenReturn(2);
        controller.onResume();
        controller.onPause();
        controller.onPageSelected(2);
        controller.onPageScrollStateChanged(ViewPager.SCROLL_STATE_IDLE);
        verify(playbackOperations, never()).setPlayQueuePosition(anyInt());
    }

    @Test
    public void callsOnTrackChangedOnIdleStateAfterPageSelected() {
        when(viewPager.getCurrentItem()).thenReturn(2);
        controller.onPageSelected(2);
        controller.onPageScrollStateChanged(ViewPager.SCROLL_STATE_IDLE);
        verify(adapter).onTrackChange();
    }

    @Test
    public void doesNotCallTrackChangedOnIdleStateWithoutPageSelected() {
        when(viewPager.getCurrentItem()).thenReturn(1);
        controller.onPageScrollStateChanged(ViewPager.SCROLL_STATE_IDLE);
        verify(adapter, never()).onTrackChange();
    }

    @Test
    public void doesNotCallTrackChangedOnIdleStateWithoutPrecedingPageSelected() {
        when(viewPager.getCurrentItem()).thenReturn(1);
        controller.onPageSelected(2);
        controller.onPageScrollStateChanged(ViewPager.SCROLL_STATE_IDLE);
        controller.onPageScrollStateChanged(ViewPager.SCROLL_STATE_IDLE);
        verify(adapter).onTrackChange(); // times(1)
    }

    @Test
    public void disablePagingWhenThePageSelectedIsAnAudioAd() {
        when(playQueueManager.isAudioAdAtPosition(2)).thenReturn(true);

        controller.onPageSelected(2);

        verify(viewPager).setPagingEnabled(false);
    }

    @Test
    public void enablePagingWhenThePageSelectedIsNotAnAudioAd() {
        when(playQueueManager.isAudioAdAtPosition(2)).thenReturn(false);

        controller.onPageSelected(2);

        verify(viewPager).setPagingEnabled(true);
    }

    @Test
    public void trackChangeEventWhenAdIsPlayingDisablesViewPager() {
        when(playQueueManager.isCurrentTrackAudioAd()).thenReturn(true);

        eventBus.publish(EventQueue.PLAY_QUEUE_TRACK, CurrentPlayQueueTrackEvent.fromPositionChanged(TrackUrn.NOT_SET));

        verify(viewPager).setPagingEnabled(false);
    }

    @Test
    public void progressEventLessThanTimeoutWhilePlayingAdDoesNotEnablePaging() {
        when(playQueueManager.isCurrentTrackAudioAd()).thenReturn(true);
        final PlaybackProgress playbackProgress = new PlaybackProgress(AdConstants.UNSKIPPABLE_TIME_MS - 1, 1L);

        eventBus.publish(EventQueue.PLAY_QUEUE_TRACK, CurrentPlayQueueTrackEvent.fromPositionChanged(TrackUrn.NOT_SET));
        eventBus.publish(EventQueue.PLAYBACK_PROGRESS, new PlaybackProgressEvent(playbackProgress, TrackUrn.NOT_SET));

        verify(viewPager, never()).setPagingEnabled(true);
    }

    @Test
    public void progressEventEqualToTimeoutWhilePlayingAdEnablesPaging() {
        when(playQueueManager.isCurrentTrackAudioAd()).thenReturn(true);
        final PlaybackProgress playbackProgress = new PlaybackProgress(AdConstants.UNSKIPPABLE_TIME_MS, 1L);

        eventBus.publish(EventQueue.PLAY_QUEUE_TRACK, CurrentPlayQueueTrackEvent.fromPositionChanged(TrackUrn.NOT_SET));
        eventBus.publish(EventQueue.PLAYBACK_PROGRESS, new PlaybackProgressEvent(playbackProgress, TrackUrn.NOT_SET));

        verify(viewPager).setPagingEnabled(true);
    }

    @Test
    public void progressEventEqualToTimeoutWhilePlayingAdAfterOnDestroyViewDoesNothing() {
        when(playQueueManager.isCurrentTrackAudioAd()).thenReturn(true);
        final PlaybackProgress playbackProgress = new PlaybackProgress(AdConstants.UNSKIPPABLE_TIME_MS, 1L);

        eventBus.publish(EventQueue.PLAY_QUEUE_TRACK, CurrentPlayQueueTrackEvent.fromPositionChanged(TrackUrn.NOT_SET));
        controller.onDestroyView();
        eventBus.publish(EventQueue.PLAYBACK_PROGRESS, new PlaybackProgressEvent(playbackProgress, TrackUrn.NOT_SET));

        verify(viewPager, never()).setPagingEnabled(true);
    }

    @Test
    public void trackChangeEventEnablesPagingIfNotAudioAd() throws Exception {
        when(playQueueManager.isCurrentTrackAudioAd()).thenReturn(true);
        eventBus.publish(EventQueue.PLAY_QUEUE_TRACK, CurrentPlayQueueTrackEvent.fromPositionChanged(TrackUrn.NOT_SET));
        Mockito.reset(viewPager);

        when(playQueueManager.isCurrentTrackAudioAd()).thenReturn(false);
        eventBus.publish(EventQueue.PLAY_QUEUE_TRACK, CurrentPlayQueueTrackEvent.fromPositionChanged(TrackUrn.NOT_SET));

        verify(viewPager).setPagingEnabled(true);
    }

    @Test
    public void trackChangeEventPreventsPagerUnlockFromPreviousAudioAd() throws Exception {
        when(playQueueManager.isCurrentTrackAudioAd()).thenReturn(true);
        eventBus.publish(EventQueue.PLAY_QUEUE_TRACK, CurrentPlayQueueTrackEvent.fromPositionChanged(TrackUrn.NOT_SET));
        when(playQueueManager.isCurrentTrackAudioAd()).thenReturn(false);
        eventBus.publish(EventQueue.PLAY_QUEUE_TRACK, CurrentPlayQueueTrackEvent.fromPositionChanged(TrackUrn.NOT_SET));
        Mockito.reset(viewPager);

        final PlaybackProgress playbackProgress = new PlaybackProgress(AdConstants.UNSKIPPABLE_TIME_MS, 1L);
        eventBus.publish(EventQueue.PLAYBACK_PROGRESS, new PlaybackProgressEvent(playbackProgress, TrackUrn.NOT_SET));

        verifyZeroInteractions(viewPager);
    }
}