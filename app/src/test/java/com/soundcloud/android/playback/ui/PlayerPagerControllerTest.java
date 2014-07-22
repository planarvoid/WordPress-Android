package com.soundcloud.android.playback.ui;

import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.events.CurrentPlayQueueTrackEvent;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PlayQueueEvent;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.PlaybackOperations;
import com.soundcloud.android.playback.service.PlayQueueManager;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.rx.eventbus.TestEventBus;
import com.soundcloud.android.tracks.TrackUrn;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import android.support.v4.view.ViewPager;
import android.view.View;

@RunWith(SoundCloudTestRunner.class)
public class PlayerPagerControllerTest {

    @Mock private TrackPagerAdapter adapter;
    @Mock private PlayerPresenter presenter;
    @Mock private PlayQueueManager playQueueManager;
    @Mock private PlaybackOperations playbackOperations;
    @Mock private View container;
    @Mock private ViewPager viewPager;

    private PlayerPagerController controller;

    private TestEventBus eventBus = new TestEventBus();

    @Before
    public void setUp() {
        controller = new PlayerPagerController(adapter, presenter, eventBus, playQueueManager, playbackOperations);
        when(playQueueManager.getCurrentPosition()).thenReturn(1);
        when(container.findViewById(anyInt())).thenReturn(viewPager);
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

    @Test
    public void onPlayQueueUpdateSetsTrackPagerAdapterIfNotSet() {
        eventBus.publish(EventQueue.PLAY_QUEUE, PlayQueueEvent.fromQueueUpdate());

        verify(viewPager).setAdapter(adapter);
    }

    @Test
    public void reportsPlayQueuePositionChangeOnIdleStateWithDifferentPagerPosition() {
        when(viewPager.getCurrentItem()).thenReturn(2);
        controller.onPageScrollStateChanged(ViewPager.SCROLL_STATE_IDLE);
        verify(playbackOperations).setPlayQueuePosition(2);
    }

    @Test
    public void callsOnTrackChangedOnIdleStateWithDifferentPagerPosition() {
        when(viewPager.getCurrentItem()).thenReturn(2);
        controller.onPageScrollStateChanged(ViewPager.SCROLL_STATE_IDLE);
        verify(adapter).onTrackChange();
    }

    @Test
    public void doesNotCallTrackChangedOnIdleStateWithSamePosition() {
        when(viewPager.getCurrentItem()).thenReturn(1);
        controller.onPageScrollStateChanged(ViewPager.SCROLL_STATE_IDLE);
        verify(adapter, never()).onTrackChange();
    }

}