package com.soundcloud.android.playback.ui;

import static com.soundcloud.android.Expect.expect;
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

import com.google.common.collect.Lists;
import com.soundcloud.android.ads.AdConstants;
import com.soundcloud.android.ads.AdsOperations;
import com.soundcloud.android.events.CurrentPlayQueueTrackEvent;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PlayControlEvent;
import com.soundcloud.android.events.PlayQueueEvent;
import com.soundcloud.android.events.PlaybackProgressEvent;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.PlaybackOperations;
import com.soundcloud.android.playback.PlaybackProgress;
import com.soundcloud.android.playback.service.PlayQueueManager;
import com.soundcloud.android.playback.ui.view.PlayerTrackPager;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.rx.eventbus.TestEventBus;
import com.soundcloud.android.testsupport.fixtures.TestPropertySets;
import com.soundcloud.android.tracks.TrackUrn;
import com.soundcloud.propeller.PropertySet;
import com.xtremelabs.robolectric.Robolectric;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.Mockito;
import rx.subjects.PublishSubject;

import android.support.v4.view.ViewPager;
import android.view.View;

import javax.inject.Provider;
import java.util.List;

@RunWith(SoundCloudTestRunner.class)
public class PlayerPagerControllerTest {

    private static final TrackUrn TRACK_URN = Urn.forTrack(456L);
    private static final PropertySet AUDIO_AD = TestPropertySets.audioAdProperties(TRACK_URN);
    private static final TrackUrn AUDIO_AD_URN = Urn.forTrack(123L);

    @Mock private TrackPagerAdapter adapter;
    @Mock private PlayerPresenter presenter;
    @Mock private PlayQueueManager playQueueManager;
    @Mock private PlaybackOperations playbackOperations;
    @Mock private AdsOperations adsOperations;
    @Mock private View container;
    @Mock private PlayerTrackPager viewPager;
    @Mock private PlayQueueDataSwitcher playQueueDataSwitcher;
    @Mock private PlayerPagerScrollListener playerPagerScrollListener;
    @Captor private ArgumentCaptor<SkipListener> skipListenerArgumentCaptor;

    private PlayerPagerController controller;
    private PublishSubject<Integer> scrollStateObservable = PublishSubject.create();
    private TestEventBus eventBus = new TestEventBus();
    private final List<TrackPageData> adQueueData = Lists.newArrayList(TrackPageData.forAd(2, AUDIO_AD_URN, AUDIO_AD));
    private final List<TrackPageData> fullQueueData = Lists.newArrayList(TrackPageData.forTrack(1, TRACK_URN), TrackPageData.forAd(2, AUDIO_AD_URN, AUDIO_AD));

    @Before
    public void setUp() {
        final Provider<PlayQueueDataSwitcher> playQueueDataControllerProvider = new Provider<PlayQueueDataSwitcher>() {
            @Override
            public PlayQueueDataSwitcher get() {
                return playQueueDataSwitcher;
            }
        };

        controller = new PlayerPagerController(adapter, presenter, eventBus,
                playQueueManager, playbackOperations, null, playQueueDataControllerProvider, playerPagerScrollListener, adsOperations);
        when(playQueueManager.getCurrentPosition()).thenReturn(1);
        when(container.findViewById(anyInt())).thenReturn(viewPager);
        when(viewPager.getContext()).thenReturn(Robolectric.application);
        when(playerPagerScrollListener.getPageChangedObservable()).thenReturn(scrollStateObservable);
        when(playQueueDataSwitcher.getAdQueue()).thenReturn(adQueueData);
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

        verify(adapter).initialize(same(viewPager), skipListenerArgumentCaptor.capture(), any(ViewVisibilityProvider.class));
        skipListenerArgumentCaptor.getValue().onNext();

        verify(viewPager).setCurrentItem(eq(4));
    }

    @Test
    public void onNextOnSkipListenerEmitsPlayerSkipClickEvent() {
        when(viewPager.getCurrentItem()).thenReturn(3);

        verify(adapter).initialize(same(viewPager), skipListenerArgumentCaptor.capture(), any(ViewVisibilityProvider.class));
        skipListenerArgumentCaptor.getValue().onNext();

        PlayControlEvent event = eventBus.lastEventOn(EventQueue.PLAY_CONTROL);
        expect(event).toEqual(PlayControlEvent.skip(PlayControlEvent.SOURCE_FULL_PLAYER));
    }

    @Test
    public void onPreviousOnSkipListenerEmitsPlayerPreviousClickEvent() {
        when(viewPager.getCurrentItem()).thenReturn(3);

        verify(adapter).initialize(same(viewPager), skipListenerArgumentCaptor.capture(), any(ViewVisibilityProvider.class));
        skipListenerArgumentCaptor.getValue().onPrevious();

        PlayControlEvent event = eventBus.lastEventOn(EventQueue.PLAY_CONTROL);
        expect(event).toEqual(PlayControlEvent.previous(PlayControlEvent.SOURCE_FULL_PLAYER));
    }

    @Test
    public void onPreviousOnSkipListenerSetsPagerToPreviousPosition() {
        when(viewPager.getCurrentItem()).thenReturn(3);

        verify(adapter).initialize(same(viewPager), skipListenerArgumentCaptor.capture(), any(ViewVisibilityProvider.class));
        skipListenerArgumentCaptor.getValue().onPrevious();

        verify(viewPager).setCurrentItem(eq(2));
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
        when(adapter.getPlayQueuePosition(2)).thenReturn(2);

        controller.onResume();
        scrollStateObservable.onNext(ViewPager.SCROLL_STATE_IDLE);

        Robolectric.runUiThreadTasksIncludingDelayedTasks();
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
        eventBus.publish(EventQueue.PLAY_QUEUE_TRACK, CurrentPlayQueueTrackEvent.fromPositionChanged(TrackUrn.NOT_SET));
        when(adsOperations.isCurrentTrackAudioAd()).thenReturn(false);
        eventBus.publish(EventQueue.PLAY_QUEUE_TRACK, CurrentPlayQueueTrackEvent.fromPositionChanged(TrackUrn.NOT_SET));
        Mockito.reset(viewPager);

        final PlaybackProgress playbackProgress = new PlaybackProgress(AdConstants.UNSKIPPABLE_TIME_MS, 1L);
        eventBus.publish(EventQueue.PLAYBACK_PROGRESS, new PlaybackProgressEvent(playbackProgress, TrackUrn.NOT_SET));

        verifyZeroInteractions(viewPager);
    }

    @Test
    public void trackChangeToAdSetsQueueOfSingleAdIfLookingAtAd() {
        when(adsOperations.isCurrentTrackAudioAd()).thenReturn(true);
        setupPositionsForAd(2, 2, 2);

        eventBus.publish(EventQueue.PLAY_QUEUE_TRACK, CurrentPlayQueueTrackEvent.fromPositionChanged(AUDIO_AD_URN));

        verify(adapter).setCurrentData(eq(adQueueData));
    }

    @Test
    public void trackChangeToAdAdvancesToAdIfNotLookingAtIt() {
        when(adsOperations.isCurrentTrackAudioAd()).thenReturn(true);
        setupPositionsForAd(1, 1, 2);

        eventBus.publish(EventQueue.PLAY_QUEUE_TRACK, CurrentPlayQueueTrackEvent.fromPositionChanged(AUDIO_AD_URN));

        verify(viewPager).setCurrentItem(2, true);
    }

    @Test
    public void pageChangedAfterTrackChangeToAdSetsAdPlayQueue() {
        when(adsOperations.isCurrentTrackAudioAd()).thenReturn(true);
        setupPositionsForAd(1, 1, 2);
        eventBus.publish(EventQueue.PLAY_QUEUE_TRACK, CurrentPlayQueueTrackEvent.fromPositionChanged(AUDIO_AD_URN));
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
    public void trackPagerAdvancesAfterAdRemovedAndStillVisible() {
        when(viewPager.getCurrentItem()).thenReturn(1);
        when(adapter.isAudioAdAtPosition(1)).thenReturn(true);

        eventBus.publish(EventQueue.PLAY_QUEUE, PlayQueueEvent.fromAudioAdRemoved());

        verify(viewPager).setCurrentItem(2, true);
    }

    @Test
    public void refreshesPlayQueueAfterNewPlayQueueEvent() {
        when(playQueueManager.getCurrentPosition()).thenReturn(2);
        when(playQueueDataSwitcher.getFullQueue()).thenReturn(fullQueueData);

        eventBus.publish(EventQueue.PLAY_QUEUE, PlayQueueEvent.fromQueueUpdate());

        verify(adapter).setCurrentData(fullQueueData);
        verify(viewPager).setCurrentItem(2, false);
    }

    @Test
    public void refreshPlayQueueAfterAdRemovedAndScrolled() {
        when(playQueueManager.getCurrentPosition()).thenReturn(2);
        when(viewPager.getCurrentItem()).thenReturn(1);
        when(adapter.isAudioAdAtPosition(1)).thenReturn(true);
        when(playQueueDataSwitcher.getFullQueue()).thenReturn(fullQueueData);

        eventBus.publish(EventQueue.PLAY_QUEUE, PlayQueueEvent.fromAudioAdRemoved());


        scrollStateObservable.onNext(ViewPager.SCROLL_STATE_IDLE);

        verify(adapter).setCurrentData(fullQueueData);
        verify(viewPager).setCurrentItem(2, false);
    }


}