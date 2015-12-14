package com.soundcloud.android.playback.ui;

import static com.soundcloud.android.testsupport.PlayQueueAssertions.assertPlayQueueItemsEqual;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyListOf;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import com.soundcloud.android.ads.AdConstants;
import com.soundcloud.android.ads.AdFixtures;
import com.soundcloud.android.ads.AdsOperations;
import com.soundcloud.android.ads.AudioAd;
import com.soundcloud.android.events.CurrentPlayQueueItemEvent;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PlayQueueEvent;
import com.soundcloud.android.events.PlaybackProgressEvent;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.PlayQueueItem;
import com.soundcloud.android.playback.PlayQueueManager;
import com.soundcloud.android.playback.PlaySessionController;
import com.soundcloud.android.playback.PlaybackProgress;
import com.soundcloud.android.playback.ui.view.PlayerTrackPager;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.fixtures.TestPlayQueueItem;
import com.soundcloud.rx.eventbus.TestEventBus;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.robolectric.Robolectric;
import rx.subjects.PublishSubject;

import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.View;

import java.util.Arrays;
import java.util.List;

public class PlayerPresenterTest extends AndroidUnitTest {

    private static final Urn TRACK_URN = Urn.forTrack(456L);
    private static final PlayQueueItem TRACK_PLAY_QUEUE_ITEM = TestPlayQueueItem.createTrack(TRACK_URN);
    private static final AudioAd AUDIO_AD = AdFixtures.getAudioAd(TRACK_URN);
    private static final Urn AUDIO_AD_URN = Urn.forTrack(123L);
    private static final PlayQueueItem AUDIO_AD_PLAY_QUEUE_ITEM = TestPlayQueueItem.createTrack(AUDIO_AD_URN, AUDIO_AD);

    @Mock private PlayerPagerPresenter playerPagerPresenter;
    @Mock private PlayQueueManager playQueueManager;
    @Mock private PlaySessionController playSessionController;
    @Mock private AdsOperations adsOperations;
    @Mock private View container;
    @Mock private PlayerTrackPager viewPager;
    @Mock private PlayerPagerScrollListener playerPagerScrollListener;
    @Mock private PlayerFragment fragment;
    @Captor private ArgumentCaptor<List<PlayQueueItem>> playQueueItemsCaptor;

    private PlayerPresenter controller;
    private PublishSubject<Integer> scrollStateObservable = PublishSubject.create();
    private TestEventBus eventBus = new TestEventBus();
    private final List<PlayQueueItem> fullPlayQueue = Arrays.asList(
            TestPlayQueueItem.createTrack(Urn.forTrack(789)),
            TRACK_PLAY_QUEUE_ITEM,
            AUDIO_AD_PLAY_QUEUE_ITEM);

    @Before
    public void setUp() {
        controller = new PlayerPresenter(playerPagerPresenter, eventBus,
                playQueueManager, playSessionController, playerPagerScrollListener, adsOperations);
        when(playQueueManager.getPlayQueueItems()).thenReturn(fullPlayQueue);
        when(playQueueManager.getCurrentPlayQueueItem()).thenReturn(TRACK_PLAY_QUEUE_ITEM);
        when(playerPagerPresenter.getCurrentPlayQueue()).thenReturn(fullPlayQueue);
        when(container.findViewById(anyInt())).thenReturn(viewPager);
        when(container.getResources()).thenReturn(resources());
        when(viewPager.getContext()).thenReturn(context());
        when(playerPagerScrollListener.getPageChangedObservable()).thenReturn(scrollStateObservable);
        controller.onViewCreated(fragment, container, null);
    }

    @Test
    public void setPagerInitializesCurrentPosition() {
        controller.onViewCreated(fragment, container, null);

        verify(viewPager, times(2)).setCurrentItem(eq(1), anyBoolean());
    }

    @Test
    public void onPlayQueueEventForTrackChangeUpdatesPagerAndAdapter() {
        eventBus.publish(EventQueue.CURRENT_PLAY_QUEUE_ITEM, CurrentPlayQueueItemEvent.fromNewQueue(TRACK_PLAY_QUEUE_ITEM, Urn.NOT_SET, 0));

        verify(viewPager, times(2)).setCurrentItem(eq(1), anyBoolean());
    }

    @Test
    public void onPlayQueueEventForNewQueueUpdatesPagerAndAdapter() {
        eventBus.publish(EventQueue.CURRENT_PLAY_QUEUE_ITEM, CurrentPlayQueueItemEvent.fromNewQueue(TRACK_PLAY_QUEUE_ITEM, Urn.NOT_SET, 0));

        verify(viewPager, times(2)).setCurrentItem(eq(1), anyBoolean());
    }

    @Test
    public void playQueueChangeAnimatesToAdjacentTracks() throws Exception {
        when(viewPager.getCurrentItem()).thenReturn(2);

        eventBus.publish(EventQueue.CURRENT_PLAY_QUEUE_ITEM, CurrentPlayQueueItemEvent.fromPositionChanged(TRACK_PLAY_QUEUE_ITEM, Urn.NOT_SET, 0));

        verify(viewPager).setCurrentItem(1, true);
    }

    @Test
    public void playQueueChangeDoesNotAnimateToNotAdjacentTracks() throws Exception {
        when(viewPager.getCurrentItem()).thenReturn(2);

        eventBus.publish(EventQueue.CURRENT_PLAY_QUEUE_ITEM, CurrentPlayQueueItemEvent.fromNewQueue(TRACK_PLAY_QUEUE_ITEM, Urn.NOT_SET, 0));

        verify(viewPager).setCurrentItem(1, false);
    }

    @Test
    public void shouldUnsubscribeFromQueuesOnUnsubscribe() {
        controller.onDestroyView(fragment);

        eventBus.verifyUnsubscribed();
    }

    @Test
    public void onPlayQueueChangedSetsNewCurrentDataInAdapter() {
        eventBus.publish(EventQueue.PLAY_QUEUE, PlayQueueEvent.fromNewQueue(Urn.NOT_SET));

        verify(playerPagerPresenter, times(2)).setCurrentPlayQueue(anyListOf(PlayQueueItem.class)); //once in setPager
    }

    @Test // Fixes issue #2045, should not happen after we implement invalidating event queues on logout
    public void onPlayQueueChangedDoesNotExpandPlayerWhenPlayQueueIsEmpty() {
        when(playQueueManager.isQueueEmpty()).thenReturn(true);

        eventBus.publish(EventQueue.CURRENT_PLAY_QUEUE_ITEM, CurrentPlayQueueItemEvent.fromNewQueue(TRACK_PLAY_QUEUE_ITEM, Urn.NOT_SET, 0));

        eventBus.verifyNoEventsOn(EventQueue.PLAYER_UI);
    }

    @Test
    public void changesPlayQueuePositionWhenInForegroundOnIdleStateAfterPageSelected() {
        when(viewPager.getCurrentItem()).thenReturn(2);
        when(playerPagerPresenter.getItemAtPosition(2)).thenReturn(AUDIO_AD_PLAY_QUEUE_ITEM);

        controller.onResume(fragment);
        scrollStateObservable.onNext(ViewPager.SCROLL_STATE_IDLE);

        Robolectric.flushForegroundThreadScheduler();
        verify(playSessionController).setCurrentPlayQueueItem(AUDIO_AD_PLAY_QUEUE_ITEM);
    }

    @Test
    public void doesNotChangePlayQueuePositionIfNeverInForegroundOnIdleStateAfterPageSelected() {
        when(viewPager.getCurrentItem()).thenReturn(2);
        when(playerPagerPresenter.getItemAtPosition(2)).thenReturn(AUDIO_AD_PLAY_QUEUE_ITEM);

        scrollStateObservable.onNext(ViewPager.SCROLL_STATE_IDLE);

        verify(playSessionController, never()).setCurrentPlayQueueItem(any(PlayQueueItem.class));
    }

    @Test
    public void doesNotChangePlayQueuePositionWhenInBackgroundOnIdleStateAfterPageSelected() {
        when(viewPager.getCurrentItem()).thenReturn(2);
        when(playerPagerPresenter.getItemAtPosition(2)).thenReturn(AUDIO_AD_PLAY_QUEUE_ITEM);
        controller.onResume(fragment);
        controller.onPause(fragment);

        scrollStateObservable.onNext(ViewPager.SCROLL_STATE_IDLE);

        verify(playSessionController, never()).setCurrentPlayQueueItem(any(PlayQueueItem.class));
    }

    @Test
    public void callsOnTrackChangedOnIdleStateAfterPageSelected() {
        when(viewPager.getCurrentItem()).thenReturn(2);

        scrollStateObservable.onNext(ViewPager.SCROLL_STATE_IDLE);

        verify(playerPagerPresenter).onTrackChange();
    }

    @Test
    public void trackChangeEventPreventsPagerUnlockFromPreviousAudioAd() throws Exception {
        when(adsOperations.isCurrentItemAd()).thenReturn(true);
        eventBus.publish(EventQueue.CURRENT_PLAY_QUEUE_ITEM, CurrentPlayQueueItemEvent.fromPositionChanged(TRACK_PLAY_QUEUE_ITEM, Urn.NOT_SET, 0));
        when(adsOperations.isCurrentItemAd()).thenReturn(false);
        eventBus.publish(EventQueue.CURRENT_PLAY_QUEUE_ITEM, CurrentPlayQueueItemEvent.fromPositionChanged(TRACK_PLAY_QUEUE_ITEM, Urn.NOT_SET, 0));
        Mockito.reset(viewPager);

        final PlaybackProgress playbackProgress = new PlaybackProgress(AdConstants.UNSKIPPABLE_TIME_MS, 1L);
        eventBus.publish(EventQueue.PLAYBACK_PROGRESS, PlaybackProgressEvent.create(playbackProgress, Urn.NOT_SET));

        verifyZeroInteractions(viewPager);
    }

    @Test
    public void trackChangeToAdSetsQueueOfSingleAdIfLookingAtAd() {
        when(adsOperations.isCurrentItemAd()).thenReturn(true);
        when(playQueueManager.getCurrentPlayQueueItem()).thenReturn(AUDIO_AD_PLAY_QUEUE_ITEM);
        setupPositionsForAd(2, 2, 2);

        eventBus.publish(EventQueue.CURRENT_PLAY_QUEUE_ITEM, CurrentPlayQueueItemEvent.fromPositionChanged(AUDIO_AD_PLAY_QUEUE_ITEM, Urn.NOT_SET, 1));

        assertLastQueueWasAdQueue();
    }

    @Test
    public void trackChangeToAdSetsAdPlayQueueIfNotLookingAtItAndNotResumed() {
        when(adsOperations.isCurrentItemAd()).thenReturn(true);
        when(playQueueManager.getCurrentPlayQueueItem()).thenReturn(AUDIO_AD_PLAY_QUEUE_ITEM);
        setupPositionsForAd(1, 1, 2);

        eventBus.publish(EventQueue.CURRENT_PLAY_QUEUE_ITEM, CurrentPlayQueueItemEvent.fromPositionChanged(AUDIO_AD_PLAY_QUEUE_ITEM, Urn.NOT_SET, 0));

        assertLastQueueWasAdQueue();
        verify(viewPager).setCurrentItem(2, true);
    }

    @Test
    public void trackChangeToAdAdvancesToAdIfNotLookingAtItAndResumed() {
        when(adsOperations.isCurrentItemAd()).thenReturn(true);
        when(playQueueManager.getCurrentPlayQueueItem()).thenReturn(AUDIO_AD_PLAY_QUEUE_ITEM);
        setupPositionsForAd(1, 1, 2);
        controller.onResume(fragment);

        eventBus.publish(EventQueue.CURRENT_PLAY_QUEUE_ITEM, CurrentPlayQueueItemEvent.fromPositionChanged(AUDIO_AD_PLAY_QUEUE_ITEM, Urn.NOT_SET, 0));

        verify(viewPager).setCurrentItem(2, true);
    }

    @Test
    public void pageChangedAfterTrackChangeToAdSetsAdPlayQueue() {
        when(adsOperations.isCurrentItemAd()).thenReturn(true);
        when(playQueueManager.getCurrentPlayQueueItem()).thenReturn(AUDIO_AD_PLAY_QUEUE_ITEM);
        setupPositionsForAd(1, 1, 2);
        eventBus.publish(EventQueue.CURRENT_PLAY_QUEUE_ITEM, CurrentPlayQueueItemEvent.fromPositionChanged(AUDIO_AD_PLAY_QUEUE_ITEM, Urn.NOT_SET, 0));
        setupPositionsForAd(2, 2, 2);

        scrollStateObservable.onNext(ViewPager.SCROLL_STATE_IDLE);

        assertLastQueueWasAdQueue();
    }

    @Test
    public void trackPagerRefreshesPlayQueueWhenAdRemovedAndStillVisibleAndPaused() {
        when(viewPager.getCurrentItem()).thenReturn(1);
        when(playerPagerPresenter.isAdPageAtPosition(1)).thenReturn(true);

        eventBus.publish(EventQueue.PLAY_QUEUE, PlayQueueEvent.fromAudioAdRemoved(Urn.NOT_SET));

        verify(playerPagerPresenter, times(2)).setCurrentPlayQueue(fullPlayQueue);
        verify(viewPager, times(2)).setCurrentItem(1, false);
    }

    @Test
    public void trackPagerRefreshesPlayQueueWhenAdRemovedAndStillVisibleAndResumedAndHasAdQueue() {
        final PagerAdapter adapter = mock(PagerAdapter.class);
        when(viewPager.getCurrentItem()).thenReturn(0);
        when(playerPagerPresenter.isAdPageAtPosition(0)).thenReturn(true);
        when(viewPager.getAdapter()).thenReturn(adapter);
        when(adapter.getCount()).thenReturn(1);
        controller.onResume(fragment);

        eventBus.publish(EventQueue.PLAY_QUEUE, PlayQueueEvent.fromAudioAdRemoved(Urn.NOT_SET));

        verify(playerPagerPresenter, times(2)).setCurrentPlayQueue(fullPlayQueue);
        verify(viewPager, times(2)).setCurrentItem(1, false);
    }

    @Test
    public void trackPagerAdvancesAfterAdRemovedAndStillVisibleAndResumed() {
        final PagerAdapter adapter = mock(PagerAdapter.class);
        when(viewPager.getCurrentItem()).thenReturn(1);
        when(playerPagerPresenter.isAdPageAtPosition(1)).thenReturn(true);
        when(viewPager.getAdapter()).thenReturn(adapter);
        when(adapter.getCount()).thenReturn(3);

        controller.onResume(fragment);

        eventBus.publish(EventQueue.PLAY_QUEUE, PlayQueueEvent.fromAudioAdRemoved(Urn.NOT_SET));

        verify(playerPagerPresenter).setCurrentPlayQueue(fullPlayQueue); // verify first fullQueue, but it should only happen once
        verify(viewPager).setCurrentItem(2, true);
    }

    @Test
    public void refreshesPlayQueueAfterNewPlayQueueEvent() {
        eventBus.publish(EventQueue.PLAY_QUEUE, PlayQueueEvent.fromQueueUpdate(Urn.NOT_SET));

        verify(playerPagerPresenter, times(2)).setCurrentPlayQueue(fullPlayQueue);
        verify(viewPager, times(2)).setCurrentItem(1, false);
    }

    @Test
    public void refreshPlayQueueAfterAdRemovedAndScrolled() {
        when(viewPager.getCurrentItem()).thenReturn(1);
        when(playerPagerPresenter.isAdPageAtPosition(1)).thenReturn(true);

        eventBus.publish(EventQueue.PLAY_QUEUE, PlayQueueEvent.fromAudioAdRemoved(Urn.NOT_SET));

        scrollStateObservable.onNext(ViewPager.SCROLL_STATE_IDLE);

        verify(playerPagerPresenter, times(2)).setCurrentPlayQueue(fullPlayQueue);
        verify(viewPager, times(2)).setCurrentItem(1, false);
    }

    private void setupPositionsForAd(int pagerPosition, int playQueuePosition, int adPosition){
        when(viewPager.getCurrentItem()).thenReturn(pagerPosition);
        when(playerPagerPresenter.getItemAtPosition(pagerPosition)).thenReturn(AUDIO_AD_PLAY_QUEUE_ITEM);
    }

    private void assertLastQueueWasAdQueue() {
        verify(playerPagerPresenter, atLeastOnce()).setCurrentPlayQueue(playQueueItemsCaptor.capture());
        final List<List<PlayQueueItem>> allValues = playQueueItemsCaptor.getAllValues();
        assertPlayQueueItemsEqual(Arrays.asList(AUDIO_AD_PLAY_QUEUE_ITEM), allValues.get(allValues.size() - 1));
    }
}
