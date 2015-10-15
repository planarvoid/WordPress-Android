package com.soundcloud.android.playback.ui;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.ads.AdsOperations;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PlayControlEvent;
import com.soundcloud.android.events.PlayerUIEvent;
import com.soundcloud.android.events.TrackingEvent;
import com.soundcloud.android.playback.PlayQueueManager;
import com.soundcloud.android.playback.ui.view.PlaybackToastHelper;
import com.soundcloud.android.playback.ui.view.PlayerTrackPager;
import com.soundcloud.rx.eventbus.TestEventBus;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import rx.observers.TestObserver;

import android.support.v4.view.ViewPager;

@RunWith(MockitoJUnitRunner.class)
public class PlayerPagerScrollListenerTest {

    @Mock PlayQueueManager playQueueManager;
    @Mock PlayerTrackPager playerTrackPager;
    @Mock PlaybackToastHelper playbackToastHelper;
    @Mock AdsOperations adsOperations;
    @Mock PlayerPagerPresenter presenter;

    private PlayerPagerScrollListener pagerScrollListener;
    private TestEventBus eventBus = new TestEventBus();
    private TestObserver<Integer> observer;

    @Before
    public void setUp() {
        observer = new TestObserver<>();
        pagerScrollListener = new PlayerPagerScrollListener(playQueueManager, playbackToastHelper, eventBus, adsOperations);
        pagerScrollListener.initialize(playerTrackPager, presenter);
        pagerScrollListener.getPageChangedObservable().subscribe(observer);
    }

    @Test
    public void doesNotEmitTrackChangedOnIdleStateWithoutPageSelected() {
        pagerScrollListener.onPageScrollStateChanged(ViewPager.SCROLL_STATE_IDLE);

        assertThat(observer.getOnNextEvents()).isEmpty();
    }

    @Test
    public void doesNotEmitTrackChangedOnIdleStateWithoutPrecedingPageSelected() {
        pagerScrollListener.onPageSelected(2);
        pagerScrollListener.onPageScrollStateChanged(ViewPager.SCROLL_STATE_IDLE);
        pagerScrollListener.onPageScrollStateChanged(ViewPager.SCROLL_STATE_IDLE);

        assertThat(observer.getOnNextEvents()).hasSize(1);
    }

    @Test
    public void emitsPlayerControlSwipeSkipEventOnSwipeNextWithExpandedPlayer() {
        startPagerSwipe(PlayerUIEvent.fromPlayerExpanded(), 1, 2);

        pagerScrollListener.onPageScrollStateChanged(ViewPager.SCROLL_STATE_IDLE);

        TrackingEvent event = eventBus.lastEventOn(EventQueue.TRACKING);
        assertThat(event).isEqualTo(PlayControlEvent.swipeSkip(true));
    }

    @Test
    public void emitsPlayerControlSwipeSkipEventOnSwipeNextWithCollapsedPlayer() {
        startPagerSwipe(PlayerUIEvent.fromPlayerCollapsed(), 1, 2);

        pagerScrollListener.onPageScrollStateChanged(ViewPager.SCROLL_STATE_IDLE);

        TrackingEvent event = eventBus.lastEventOn(EventQueue.TRACKING);
        assertThat(event).isEqualTo(PlayControlEvent.swipeSkip(false));
    }

    @Test
    public void emitsPlayerControlSwipePreviousEventOnSwipePreviousWithExpandedPlayer() {
        startPagerSwipe(PlayerUIEvent.fromPlayerExpanded(), 2, 1);

        pagerScrollListener.onPageScrollStateChanged(ViewPager.SCROLL_STATE_IDLE);

        TrackingEvent event = eventBus.lastEventOn(EventQueue.TRACKING);
        assertThat(event).isEqualTo(PlayControlEvent.swipePrevious(true));
    }

    @Test
    public void emitsPlayerControlSwipePreviousEventOnSwipePreviousWithCollapsedPlayer() {
        startPagerSwipe(PlayerUIEvent.fromPlayerCollapsed(), 2, 1);

        pagerScrollListener.onPageScrollStateChanged(ViewPager.SCROLL_STATE_IDLE);

        TrackingEvent event = eventBus.lastEventOn(EventQueue.TRACKING);
        assertThat(event).isEqualTo(PlayControlEvent.swipePrevious(false));
    }

    @Test
    public void showsBlockedSwipeToastWhenSwipeOnAdPage() {
        when(adsOperations.isCurrentTrackAudioAd()).thenReturn(true);

        pagerScrollListener.onPageScrollStateChanged(ViewPager.SCROLL_STATE_IDLE);

        verify(playbackToastHelper).showUnskippableAdToast();
    }

    @Test
    public void doesNotShowBlocksSwipeToastWhenSwipeOnTrackPage() {
        pagerScrollListener.onPageScrollStateChanged(ViewPager.SCROLL_STATE_IDLE);

        verify(playbackToastHelper, never()).showUnskippableAdToast();
    }

    @Test
    public void setsPagingDisabledOnPageSelectedWithAudioAdNotAtCurrentPosition() {
        when(presenter.getPlayQueuePosition(1)).thenReturn(1);
        when(adsOperations.isAudioAdAtPosition(1)).thenReturn(true);
        when(playQueueManager.isCurrentPosition(1)).thenReturn(false);

        pagerScrollListener.onPageSelected(1);

        verify(playerTrackPager).setPagingEnabled(false);
    }

    @Test
    public void setsPagingEnabledOnPageSelectedWithAudioAdAtCurrentPosition() {
        when(presenter.getPlayQueuePosition(1)).thenReturn(1);
        when(adsOperations.isAudioAdAtPosition(1)).thenReturn(true);
        when(playQueueManager.isCurrentPosition(1)).thenReturn(true);

        pagerScrollListener.onPageSelected(1);

        verify(playerTrackPager).setPagingEnabled(true);
    }

    @Test
    public void setsPagingEnabledOnPageSelectedWithCurrentNormalTrack() {
        // for normal tracks paging should always be enabled
        when(presenter.getPlayQueuePosition(1)).thenReturn(1);
        when(adsOperations.isAudioAdAtPosition(1)).thenReturn(false);
        when(playQueueManager.isCurrentPosition(1)).thenReturn(true);

        pagerScrollListener.onPageSelected(1);

        verify(playerTrackPager).setPagingEnabled(true);
    }

    @Test
    public void setsPagingEnabledOnPageSelectedWithNormalTrack() {
        // for normal tracks paging should always be enabled
        when(presenter.getPlayQueuePosition(1)).thenReturn(1);
        when(adsOperations.isAudioAdAtPosition(1)).thenReturn(false);
        when(playQueueManager.isCurrentPosition(1)).thenReturn(false);

        pagerScrollListener.onPageSelected(1);

        verify(playerTrackPager).setPagingEnabled(true);
    }

    private void startPagerSwipe(PlayerUIEvent lastSlidingPlayerEvent, int newPosition, int oldPosition) {
        eventBus.publish(EventQueue.PLAYER_UI, lastSlidingPlayerEvent);
        when(playQueueManager.getCurrentPosition()).thenReturn(newPosition);
        when(playerTrackPager.getCurrentItem()).thenReturn(oldPosition);
        pagerScrollListener.onPageScrollStateChanged(ViewPager.SCROLL_STATE_DRAGGING);
        pagerScrollListener.onPageSelected(newPosition);
    }
}
