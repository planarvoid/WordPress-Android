package com.soundcloud.android.playback.ui;

import static com.soundcloud.java.collections.Lists.newArrayList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyListOf;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.same;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import com.soundcloud.android.ads.AdConstants;
import com.soundcloud.android.ads.AdsOperations;
import com.soundcloud.android.events.CurrentPlayQueueTrackEvent;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PlayControlEvent;
import com.soundcloud.android.events.PlayQueueEvent;
import com.soundcloud.android.events.PlaybackProgressEvent;
import com.soundcloud.android.events.TrackingEvent;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.PlayQueueManager;
import com.soundcloud.android.playback.PlaybackOperations;
import com.soundcloud.android.playback.PlaybackProgress;
import com.soundcloud.android.playback.ui.view.PlayerTrackPager;
import com.soundcloud.android.rx.eventbus.TestEventBus;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.fixtures.TestPropertySets;
import com.soundcloud.java.collections.PropertySet;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.robolectric.Robolectric;
import rx.subjects.PublishSubject;

import android.support.v4.view.ViewPager;
import android.view.View;

import javax.inject.Provider;
import java.util.List;

public class PlayerPagerControllerTest extends AndroidUnitTest {

    private static final Urn TRACK_URN = Urn.forTrack(456L);
    private static final PropertySet AUDIO_AD = TestPropertySets.audioAdProperties(TRACK_URN);
    private static final Urn AUDIO_AD_URN = Urn.forTrack(123L);

    @Mock private TrackPagerAdapter adapter;
    @Mock private PlayerPresenter presenter;
    @Mock private PlayQueueManager playQueueManager;
    @Mock private PlaybackOperations playbackOperations;
    @Mock private AdsOperations adsOperations;
    @Mock private View container;
    @Mock private PlayerTrackPager viewPager;
    @Mock private PlayQueueDataSource playQueueDataSource;
    @Mock private PlayerPagerScrollListener playerPagerScrollListener;
    @Captor private ArgumentCaptor<SkipListener> skipListenerArgumentCaptor;

    private PlayerPagerController controller;
    private PublishSubject<Integer> scrollStateObservable = PublishSubject.create();
    private TestEventBus eventBus = new TestEventBus();
    private final List<TrackPageData> adQueueData = newArrayList(new TrackPageData(2, AUDIO_AD_URN, AUDIO_AD));
    private final List<TrackPageData> fullQueueData = newArrayList(new TrackPageData(1, TRACK_URN, PropertySet.create()),
            new TrackPageData(2, AUDIO_AD_URN, AUDIO_AD));

    @Before
    public void setUp() {
        final Provider<PlayQueueDataSource> playQueueDataControllerProvider = new Provider<PlayQueueDataSource>() {
            @Override
            public PlayQueueDataSource get() {
                return playQueueDataSource;
            }
        };

        controller = new PlayerPagerController(adapter, presenter, eventBus,
                playQueueManager, playbackOperations, playQueueDataControllerProvider, playerPagerScrollListener, adsOperations);
        when(playQueueManager.getCurrentPosition()).thenReturn(1);
        when(container.findViewById(anyInt())).thenReturn(viewPager);
        when(viewPager.getContext()).thenReturn(context());
        when(playerPagerScrollListener.getPageChangedObservable()).thenReturn(scrollStateObservable);
        when(playQueueDataSource.getCurrentTrackAsQueue()).thenReturn(adQueueData);
        when(playQueueDataSource.getFullQueue()).thenReturn(fullQueueData);
        controller.onViewCreated(container);
    }

    @Test
    public void setPagerInitializesCurrentPosition() {
        when(playQueueManager.getCurrentPosition()).thenReturn(3);

        controller.onViewCreated(container);

        verify(viewPager).setCurrentItem(eq(3), anyBoolean());
    }

    @Test
    public void onNextOnSkipListenerSetsPagerToNextPosition() {
        when(viewPager.getCurrentItem()).thenReturn(3);

        verify(adapter).onViewCreated(same(viewPager), skipListenerArgumentCaptor.capture(), any(ViewVisibilityProvider.class));
        skipListenerArgumentCaptor.getValue().onNext();

        verify(viewPager).setCurrentItem(eq(4));
    }

    @Test
    public void onNextOnSkipListenerEmitsPlayerSkipClickEvent() {
        when(viewPager.getCurrentItem()).thenReturn(3);

        verify(adapter).onViewCreated(same(viewPager), skipListenerArgumentCaptor.capture(), any(ViewVisibilityProvider.class));
        skipListenerArgumentCaptor.getValue().onNext();

        TrackingEvent event = eventBus.lastEventOn(EventQueue.TRACKING);
        assertThat(event).isEqualTo(PlayControlEvent.skip(PlayControlEvent.SOURCE_FULL_PLAYER));
    }

    @Test
    public void onPreviousOnSkipListenerEmitsPlayerPreviousClickEvent() {
        when(viewPager.getCurrentItem()).thenReturn(3);

        verify(adapter).onViewCreated(same(viewPager), skipListenerArgumentCaptor.capture(), any(ViewVisibilityProvider.class));
        skipListenerArgumentCaptor.getValue().onPrevious();

        TrackingEvent event = eventBus.lastEventOn(EventQueue.TRACKING);
        assertThat(event).isEqualTo(PlayControlEvent.previous(PlayControlEvent.SOURCE_FULL_PLAYER));
    }

    @Test
    public void onPreviousOnSkipListenerSetsPagerToPreviousPosition() {
        when(viewPager.getCurrentItem()).thenReturn(3);

        verify(adapter).onViewCreated(same(viewPager), skipListenerArgumentCaptor.capture(), any(ViewVisibilityProvider.class));
        skipListenerArgumentCaptor.getValue().onPrevious();

        verify(viewPager).setCurrentItem(eq(2));
    }

    @Test
    public void onPlayQueueEventForTrackChangeUpdatesPagerAndAdapter() {
        when(playQueueManager.getCurrentPosition()).thenReturn(3);

        eventBus.publish(EventQueue.PLAY_QUEUE_TRACK, CurrentPlayQueueTrackEvent.fromNewQueue(Urn.NOT_SET, Urn.NOT_SET, 0));

        verify(viewPager).setCurrentItem(eq(3), anyBoolean());
    }

    @Test
    public void onPlayQueueEventForNewQueueUpdatesPagerAndAdapter() {
        when(playQueueManager.getCurrentPosition()).thenReturn(3);

        eventBus.publish(EventQueue.PLAY_QUEUE_TRACK, CurrentPlayQueueTrackEvent.fromNewQueue(Urn.forTrack(123L), Urn.NOT_SET, 0));

        verify(viewPager).setCurrentItem(eq(3), anyBoolean());
    }

    @Test
    public void onPlayQueueEventShouldNotNotifyDataSetChangeOnTrackChange() {
        when(playQueueManager.getCurrentPosition()).thenReturn(3);

        eventBus.publish(EventQueue.PLAY_QUEUE_TRACK, CurrentPlayQueueTrackEvent.fromPositionChanged(Urn.forTrack(123L), Urn.NOT_SET, 0));

        verify(adapter, never()).notifyDataSetChanged();
    }

    @Test
    public void playQueueChangeAnimatesToAdjacentTracks() throws Exception {
        when(viewPager.getCurrentItem()).thenReturn(2);
        when(playQueueManager.getCurrentPosition()).thenReturn(3);

        eventBus.publish(EventQueue.PLAY_QUEUE_TRACK, CurrentPlayQueueTrackEvent.fromPositionChanged(Urn.NOT_SET, Urn.NOT_SET, 0));

        verify(viewPager).setCurrentItem(3, true);
    }

    @Test
    public void playQueueChangeDoesNotAnimateToNotAdjacentTracks() throws Exception {
        when(viewPager.getCurrentItem()).thenReturn(2);
        when(playQueueManager.getCurrentPosition()).thenReturn(4);

        eventBus.publish(EventQueue.PLAY_QUEUE_TRACK, CurrentPlayQueueTrackEvent.fromNewQueue(Urn.NOT_SET, Urn.NOT_SET, 0));

        verify(viewPager).setCurrentItem(4, false);
    }

    @Test
    public void shouldUnsubscribeFromQueuesOnUnsubscribe() {
        controller.onDestroyView();

        eventBus.verifyUnsubscribed();
    }

    @Test
    public void onPlayQueueChangedSetsNewCurrentDataInAdapter() {
        eventBus.publish(EventQueue.PLAY_QUEUE, PlayQueueEvent.fromNewQueue());

        verify(adapter, times(2)).setCurrentData(anyListOf(TrackPageData.class)); //once in setPager
    }

    @Test
    public void onPlayQueueChangedSetsTrackPagerAdapterIfNotSet() {
        eventBus.publish(EventQueue.PLAY_QUEUE, PlayQueueEvent.fromNewQueue());

        verify(viewPager).setAdapter(adapter);
    }

    @Test // Fixes issue #2045, should not happen after we implement invalidating event queues on logout
    public void onPlayQueueChangedDoesNotExpandPlayerWhenPlayQueueIsEmpty() {
        when(playQueueManager.isQueueEmpty()).thenReturn(true);

        eventBus.publish(EventQueue.PLAY_QUEUE_TRACK, CurrentPlayQueueTrackEvent.fromNewQueue(Urn.forTrack(1l), Urn.NOT_SET, 0));

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
        when(adapter.getPlayQueuePosition(2)).thenReturn(2);

        controller.onResume();
        scrollStateObservable.onNext(ViewPager.SCROLL_STATE_IDLE);

        Robolectric.flushForegroundThreadScheduler();
        verify(playbackOperations).setPlayQueuePosition(2);
    }

    @Test
    public void doesNotChangePlayQueuePositionIfNeverInForegroundOnIdleStateAfterPageSelected() {
        when(viewPager.getCurrentItem()).thenReturn(2);

        scrollStateObservable.onNext(ViewPager.SCROLL_STATE_IDLE);

        verify(playbackOperations, never()).setPlayQueuePosition(anyInt());
    }

    @Test
    public void doesNotChangePlayQueuePositionWhenInBackgroundOnIdleStateAfterPageSelected() {
        when(viewPager.getCurrentItem()).thenReturn(2);
        controller.onResume();
        controller.onPause();

        scrollStateObservable.onNext(ViewPager.SCROLL_STATE_IDLE);

        verify(playbackOperations, never()).setPlayQueuePosition(anyInt());
    }

    @Test
    public void callsOnTrackChangedOnIdleStateAfterPageSelected() {
        when(viewPager.getCurrentItem()).thenReturn(2);

        scrollStateObservable.onNext(ViewPager.SCROLL_STATE_IDLE);

        verify(adapter).onTrackChange();
    }

    @Test
    public void trackChangeEventPreventsPagerUnlockFromPreviousAudioAd() throws Exception {
        when(adsOperations.isCurrentTrackAudioAd()).thenReturn(true);
        eventBus.publish(EventQueue.PLAY_QUEUE_TRACK, CurrentPlayQueueTrackEvent.fromPositionChanged(Urn.NOT_SET, Urn.NOT_SET, 0));
        when(adsOperations.isCurrentTrackAudioAd()).thenReturn(false);
        eventBus.publish(EventQueue.PLAY_QUEUE_TRACK, CurrentPlayQueueTrackEvent.fromPositionChanged(Urn.NOT_SET, Urn.NOT_SET, 0));
        Mockito.reset(viewPager);

        final PlaybackProgress playbackProgress = new PlaybackProgress(AdConstants.UNSKIPPABLE_TIME_MS, 1L);
        eventBus.publish(EventQueue.PLAYBACK_PROGRESS, new PlaybackProgressEvent(playbackProgress, Urn.NOT_SET));

        verifyZeroInteractions(viewPager);
    }

    @Test
    public void trackChangeToAdSetsQueueOfSingleAdIfLookingAtAd() {
        when(adsOperations.isCurrentTrackAudioAd()).thenReturn(true);
        setupPositionsForAd(2, 2, 2);

        eventBus.publish(EventQueue.PLAY_QUEUE_TRACK, CurrentPlayQueueTrackEvent.fromPositionChanged(AUDIO_AD_URN, Urn.NOT_SET, 0));

        verify(adapter).setCurrentData(eq(adQueueData));
    }

    @Test
    public void trackChangeToAdSetsAdPlayQueueIfNotLookingAtItAndNotResumed() {
        when(adsOperations.isCurrentTrackAudioAd()).thenReturn(true);
        setupPositionsForAd(1, 1, 2);

        eventBus.publish(EventQueue.PLAY_QUEUE_TRACK, CurrentPlayQueueTrackEvent.fromPositionChanged(AUDIO_AD_URN, Urn.NOT_SET, 0));

        verify(adapter).setCurrentData(eq(adQueueData));
        verify(viewPager).setCurrentItem(2, true);
    }

    @Test
    public void trackChangeToAdAdvancesToAdIfNotLookingAtItAndResumed() {
        when(adsOperations.isCurrentTrackAudioAd()).thenReturn(true);
        setupPositionsForAd(1, 1, 2);
        controller.onResume();

        eventBus.publish(EventQueue.PLAY_QUEUE_TRACK, CurrentPlayQueueTrackEvent.fromPositionChanged(AUDIO_AD_URN, Urn.NOT_SET, 0));

        verify(adapter, never()).setCurrentData(eq(adQueueData));
        verify(viewPager).setCurrentItem(2, true);
    }

    @Test
    public void pageChangedAfterTrackChangeToAdSetsAdPlayQueue() {
        when(adsOperations.isCurrentTrackAudioAd()).thenReturn(true);
        setupPositionsForAd(1, 1, 2);
        eventBus.publish(EventQueue.PLAY_QUEUE_TRACK, CurrentPlayQueueTrackEvent.fromPositionChanged(AUDIO_AD_URN, Urn.NOT_SET, 0));
        setupPositionsForAd(2, 2, 2);

        scrollStateObservable.onNext(ViewPager.SCROLL_STATE_IDLE);

        verify(adapter).setCurrentData(eq(adQueueData));
    }

    private void setupPositionsForAd(int pagerPosition, int playQueuePosition, int adPosition){
        when(viewPager.getCurrentItem()).thenReturn(pagerPosition);
        when(adapter.getPlayQueuePosition(pagerPosition)).thenReturn(playQueuePosition);
        when(adsOperations.isAudioAdAtPosition(adPosition)).thenReturn(true);
        when(playQueueManager.getCurrentPosition()).thenReturn(adPosition);
        when(playQueueManager.isCurrentPosition(playQueuePosition)).thenReturn(adPosition == playQueuePosition); // ??

    }

    @Test
    public void trackPagerRefreshesPlayQueueWhenAdRemovedAndStillVisibleAndPaused() {
        when(viewPager.getCurrentItem()).thenReturn(1);
        when(adapter.isAudioAdAtPosition(1)).thenReturn(true);
        when(playQueueManager.getCurrentPosition()).thenReturn(2);

        eventBus.publish(EventQueue.PLAY_QUEUE, PlayQueueEvent.fromAudioAdRemoved());

        verify(adapter, times(2)).setCurrentData(fullQueueData);
        verify(viewPager).setCurrentItem(2, false);
    }

    @Test
    public void trackPagerRefreshesPlayQueueWhenAdRemovedAndStillVisibleAndResumedAndHasAdQueue() {
        when(viewPager.getCurrentItem()).thenReturn(0);
        when(adapter.isAudioAdAtPosition(0)).thenReturn(true);
        when(adapter.getCount()).thenReturn(1);
        when(playQueueManager.getCurrentPosition()).thenReturn(2);
        controller.onResume();

        eventBus.publish(EventQueue.PLAY_QUEUE, PlayQueueEvent.fromAudioAdRemoved());

        verify(adapter, times(2)).setCurrentData(fullQueueData);
        verify(viewPager).setCurrentItem(2, false);
    }

    @Test
    public void trackPagerAdvancesAfterAdRemovedAndStillVisibleAndResumed() {
        when(viewPager.getCurrentItem()).thenReturn(1);
        when(adapter.isAudioAdAtPosition(1)).thenReturn(true);
        when(adapter.getCount()).thenReturn(3);

        controller.onResume();

        eventBus.publish(EventQueue.PLAY_QUEUE, PlayQueueEvent.fromAudioAdRemoved());

        verify(adapter).setCurrentData(fullQueueData); // verify first fullQueue, but it should only happen once
        verify(viewPager).setCurrentItem(2, true);
    }

    @Test
    public void refreshesPlayQueueAfterNewPlayQueueEvent() {
        when(playQueueManager.getCurrentPosition()).thenReturn(2);

        eventBus.publish(EventQueue.PLAY_QUEUE, PlayQueueEvent.fromQueueUpdate());

        verify(adapter, times(2)).setCurrentData(fullQueueData);
        verify(viewPager).setCurrentItem(2, false);
    }

    @Test
    public void refreshPlayQueueAfterAdRemovedAndScrolled() {
        when(playQueueManager.getCurrentPosition()).thenReturn(2);
        when(viewPager.getCurrentItem()).thenReturn(1);
        when(adapter.isAudioAdAtPosition(1)).thenReturn(true);

        eventBus.publish(EventQueue.PLAY_QUEUE, PlayQueueEvent.fromAudioAdRemoved());

        scrollStateObservable.onNext(ViewPager.SCROLL_STATE_IDLE);

        verify(adapter, times(2)).setCurrentData(fullQueueData);
        verify(viewPager).setCurrentItem(2, false);
    }

}
