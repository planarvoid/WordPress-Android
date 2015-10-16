package com.soundcloud.android.playback.ui;

import static com.soundcloud.java.collections.Lists.newArrayList;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyListOf;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import com.soundcloud.android.ads.AdConstants;
import com.soundcloud.android.ads.AdsOperations;
import com.soundcloud.android.events.CurrentPlayQueueTrackEvent;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PlayQueueEvent;
import com.soundcloud.android.events.PlaybackProgressEvent;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.PlayQueueManager;
import com.soundcloud.android.playback.PlaySessionController;
import com.soundcloud.android.playback.PlaybackProgress;
import com.soundcloud.android.playback.ui.view.PlayerTrackPager;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.fixtures.TestPropertySets;
import com.soundcloud.java.collections.PropertySet;
import com.soundcloud.rx.eventbus.TestEventBus;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.robolectric.Robolectric;
import rx.subjects.PublishSubject;

import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.View;

import javax.inject.Provider;
import java.util.List;

public class PlayerPresenterTest extends AndroidUnitTest {

    private static final Urn TRACK_URN = Urn.forTrack(456L);
    private static final PropertySet AUDIO_AD = TestPropertySets.audioAdProperties(TRACK_URN);
    private static final Urn AUDIO_AD_URN = Urn.forTrack(123L);

    @Mock private PlayerPagerPresenter playerPagerPresenter;
    @Mock private PlayQueueManager playQueueManager;
    @Mock private PlaySessionController playSessionController;
    @Mock private AdsOperations adsOperations;
    @Mock private View container;
    @Mock private PlayerTrackPager viewPager;
    @Mock private PlayQueueDataSource playQueueDataSource;
    @Mock private PlayerPagerScrollListener playerPagerScrollListener;
    @Mock private PlayerFragment fragment;

    private PlayerPresenter controller;
    private PublishSubject<Integer> scrollStateObservable = PublishSubject.create();
    private TestEventBus eventBus = new TestEventBus();
    private final List<TrackPageData> adQueueData = newArrayList(new TrackPageData(2, AUDIO_AD_URN, Urn.NOT_SET, AUDIO_AD));
    private final List<TrackPageData> fullQueueData = newArrayList(new TrackPageData(1, TRACK_URN, Urn.NOT_SET, PropertySet.create()),
            new TrackPageData(2, AUDIO_AD_URN, Urn.NOT_SET, AUDIO_AD));

    @Before
    public void setUp() {
        final Provider<PlayQueueDataSource> playQueueDataControllerProvider = new Provider<PlayQueueDataSource>() {
            @Override
            public PlayQueueDataSource get() {
                return playQueueDataSource;
            }
        };
        controller = new PlayerPresenter(playerPagerPresenter, eventBus,
                playQueueManager, playSessionController, playQueueDataControllerProvider, playerPagerScrollListener, adsOperations);
        when(playQueueManager.getCurrentPosition()).thenReturn(1);
        when(container.findViewById(anyInt())).thenReturn(viewPager);
        when(container.getResources()).thenReturn(resources());
        when(viewPager.getContext()).thenReturn(context());
        when(playerPagerScrollListener.getPageChangedObservable()).thenReturn(scrollStateObservable);
        when(playQueueDataSource.getCurrentTrackAsQueue()).thenReturn(adQueueData);
        when(playQueueDataSource.getFullQueue()).thenReturn(fullQueueData);
        controller.onViewCreated(fragment, container, null);
    }

    @Test
    public void setPagerInitializesCurrentPosition() {
        when(playQueueManager.getCurrentPosition()).thenReturn(3);

        controller.onViewCreated(fragment, container, null);

        verify(viewPager).setCurrentItem(eq(3), anyBoolean());
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
        controller.onDestroyView(fragment);

        eventBus.verifyUnsubscribed();
    }

    @Test
    public void onPlayQueueChangedSetsNewCurrentDataInAdapter() {
        eventBus.publish(EventQueue.PLAY_QUEUE, PlayQueueEvent.fromNewQueue(Urn.NOT_SET));

        verify(playerPagerPresenter, times(2)).setCurrentData(anyListOf(TrackPageData.class)); //once in setPager
    }

    @Test // Fixes issue #2045, should not happen after we implement invalidating event queues on logout
    public void onPlayQueueChangedDoesNotExpandPlayerWhenPlayQueueIsEmpty() {
        when(playQueueManager.isQueueEmpty()).thenReturn(true);

        eventBus.publish(EventQueue.PLAY_QUEUE_TRACK, CurrentPlayQueueTrackEvent.fromNewQueue(Urn.forTrack(1l), Urn.NOT_SET, 0));

        eventBus.verifyNoEventsOn(EventQueue.PLAYER_UI);
    }

    @Test
    public void changesPlayQueuePositionWhenInForegroundOnIdleStateAfterPageSelected() {
        when(viewPager.getCurrentItem()).thenReturn(2);
        when(playerPagerPresenter.getPlayQueuePosition(2)).thenReturn(2);

        controller.onResume(fragment);
        scrollStateObservable.onNext(ViewPager.SCROLL_STATE_IDLE);

        Robolectric.flushForegroundThreadScheduler();
        verify(playSessionController).setPlayQueuePosition(2);
    }

    @Test
    public void doesNotChangePlayQueuePositionIfNeverInForegroundOnIdleStateAfterPageSelected() {
        when(viewPager.getCurrentItem()).thenReturn(2);

        scrollStateObservable.onNext(ViewPager.SCROLL_STATE_IDLE);

        verify(playSessionController, never()).setPlayQueuePosition(anyInt());
    }

    @Test
    public void doesNotChangePlayQueuePositionWhenInBackgroundOnIdleStateAfterPageSelected() {
        when(viewPager.getCurrentItem()).thenReturn(2);
        controller.onResume(fragment);
        controller.onPause(fragment);

        scrollStateObservable.onNext(ViewPager.SCROLL_STATE_IDLE);

        verify(playSessionController, never()).setPlayQueuePosition(anyInt());
    }

    @Test
    public void callsOnTrackChangedOnIdleStateAfterPageSelected() {
        when(viewPager.getCurrentItem()).thenReturn(2);

        scrollStateObservable.onNext(ViewPager.SCROLL_STATE_IDLE);

        verify(playerPagerPresenter).onTrackChange();
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

        verify(playerPagerPresenter).setCurrentData(eq(adQueueData));
    }

    @Test
    public void trackChangeToAdSetsAdPlayQueueIfNotLookingAtItAndNotResumed() {
        when(adsOperations.isCurrentTrackAudioAd()).thenReturn(true);
        setupPositionsForAd(1, 1, 2);

        eventBus.publish(EventQueue.PLAY_QUEUE_TRACK, CurrentPlayQueueTrackEvent.fromPositionChanged(AUDIO_AD_URN, Urn.NOT_SET, 0));

        verify(playerPagerPresenter).setCurrentData(eq(adQueueData));
        verify(viewPager).setCurrentItem(2, true);
    }

    @Test
    public void trackChangeToAdAdvancesToAdIfNotLookingAtItAndResumed() {
        when(adsOperations.isCurrentTrackAudioAd()).thenReturn(true);
        setupPositionsForAd(1, 1, 2);
        controller.onResume(fragment);

        eventBus.publish(EventQueue.PLAY_QUEUE_TRACK, CurrentPlayQueueTrackEvent.fromPositionChanged(AUDIO_AD_URN, Urn.NOT_SET, 0));

        verify(playerPagerPresenter, never()).setCurrentData(eq(adQueueData));
        verify(viewPager).setCurrentItem(2, true);
    }

    @Test
    public void pageChangedAfterTrackChangeToAdSetsAdPlayQueue() {
        when(adsOperations.isCurrentTrackAudioAd()).thenReturn(true);
        setupPositionsForAd(1, 1, 2);
        eventBus.publish(EventQueue.PLAY_QUEUE_TRACK, CurrentPlayQueueTrackEvent.fromPositionChanged(AUDIO_AD_URN, Urn.NOT_SET, 0));
        setupPositionsForAd(2, 2, 2);

        scrollStateObservable.onNext(ViewPager.SCROLL_STATE_IDLE);

        verify(playerPagerPresenter).setCurrentData(eq(adQueueData));
    }

    private void setupPositionsForAd(int pagerPosition, int playQueuePosition, int adPosition){
        when(viewPager.getCurrentItem()).thenReturn(pagerPosition);
        when(playerPagerPresenter.getPlayQueuePosition(pagerPosition)).thenReturn(playQueuePosition);
        when(adsOperations.isAudioAdAtPosition(adPosition)).thenReturn(true);
        when(playQueueManager.getCurrentPosition()).thenReturn(adPosition);
        when(playQueueManager.isCurrentPosition(playQueuePosition)).thenReturn(adPosition == playQueuePosition); // ??

    }

    @Test
    public void trackPagerRefreshesPlayQueueWhenAdRemovedAndStillVisibleAndPaused() {
        when(viewPager.getCurrentItem()).thenReturn(1);
        when(playerPagerPresenter.isAudioAdAtPosition(1)).thenReturn(true);
        when(playQueueManager.getCurrentPosition()).thenReturn(2);

        eventBus.publish(EventQueue.PLAY_QUEUE, PlayQueueEvent.fromAudioAdRemoved(Urn.NOT_SET));

        verify(playerPagerPresenter, times(2)).setCurrentData(fullQueueData);
        verify(viewPager).setCurrentItem(2, false);
    }

    @Test
    public void trackPagerRefreshesPlayQueueWhenAdRemovedAndStillVisibleAndResumedAndHasAdQueue() {
        final PagerAdapter adapter = mock(PagerAdapter.class);
        when(viewPager.getCurrentItem()).thenReturn(0);
        when(playerPagerPresenter.isAudioAdAtPosition(0)).thenReturn(true);
        when(viewPager.getAdapter()).thenReturn(adapter);
        when(adapter.getCount()).thenReturn(1);
        when(playQueueManager.getCurrentPosition()).thenReturn(2);
        controller.onResume(fragment);

        eventBus.publish(EventQueue.PLAY_QUEUE, PlayQueueEvent.fromAudioAdRemoved(Urn.NOT_SET));

        verify(playerPagerPresenter, times(2)).setCurrentData(fullQueueData);
        verify(viewPager).setCurrentItem(2, false);
    }

    @Test
    public void trackPagerAdvancesAfterAdRemovedAndStillVisibleAndResumed() {
        final PagerAdapter adapter = mock(PagerAdapter.class);
        when(viewPager.getCurrentItem()).thenReturn(1);
        when(playerPagerPresenter.isAudioAdAtPosition(1)).thenReturn(true);
        when(viewPager.getAdapter()).thenReturn(adapter);
        when(adapter.getCount()).thenReturn(3);

        controller.onResume(fragment);

        eventBus.publish(EventQueue.PLAY_QUEUE, PlayQueueEvent.fromAudioAdRemoved(Urn.NOT_SET));

        verify(playerPagerPresenter).setCurrentData(fullQueueData); // verify first fullQueue, but it should only happen once
        verify(viewPager).setCurrentItem(2, true);
    }

    @Test
    public void refreshesPlayQueueAfterNewPlayQueueEvent() {
        when(playQueueManager.getCurrentPosition()).thenReturn(2);

        eventBus.publish(EventQueue.PLAY_QUEUE, PlayQueueEvent.fromQueueUpdate(Urn.NOT_SET));

        verify(playerPagerPresenter, times(2)).setCurrentData(fullQueueData);
        verify(viewPager).setCurrentItem(2, false);
    }

    @Test
    public void refreshPlayQueueAfterAdRemovedAndScrolled() {
        when(playQueueManager.getCurrentPosition()).thenReturn(2);
        when(viewPager.getCurrentItem()).thenReturn(1);
        when(playerPagerPresenter.isAudioAdAtPosition(1)).thenReturn(true);

        eventBus.publish(EventQueue.PLAY_QUEUE, PlayQueueEvent.fromAudioAdRemoved(Urn.NOT_SET));

        scrollStateObservable.onNext(ViewPager.SCROLL_STATE_IDLE);

        verify(playerPagerPresenter, times(2)).setCurrentData(fullQueueData);
        verify(viewPager).setCurrentItem(2, false);
    }

}
