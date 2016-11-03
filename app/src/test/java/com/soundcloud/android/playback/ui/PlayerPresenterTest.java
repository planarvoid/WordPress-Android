package com.soundcloud.android.playback.ui;

import static com.soundcloud.android.testsupport.PlayQueueAssertions.assertPlayQueueItemsEqual;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyList;
import static org.mockito.Matchers.anyListOf;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import com.soundcloud.android.R;
import com.soundcloud.android.ads.AdConstants;
import com.soundcloud.android.ads.AdData;
import com.soundcloud.android.ads.AdFixtures;
import com.soundcloud.android.ads.AdsOperations;
import com.soundcloud.android.ads.AudioAd;
import com.soundcloud.android.events.CurrentPlayQueueItemEvent;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PlayQueueEvent;
import com.soundcloud.android.events.PlaybackProgressEvent;
import com.soundcloud.android.events.PlayerUICommand;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.PlayQueueItem;
import com.soundcloud.android.playback.PlayQueueManager;
import com.soundcloud.android.playback.PlaySessionController;
import com.soundcloud.android.playback.PlaybackProgress;
import com.soundcloud.android.playback.playqueue.PlayQueueFragment;
import com.soundcloud.android.playback.playqueue.PlayQueueFragmentFactory;
import com.soundcloud.android.playback.playqueue.PlayQueueUIEvent;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.fixtures.TestPlayQueueItem;
import com.soundcloud.java.functions.Predicate;
import com.soundcloud.java.optional.Optional;
import com.soundcloud.rx.eventbus.TestEventBus;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.robolectric.Robolectric;
import rx.observers.TestSubscriber;
import rx.subjects.PublishSubject;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.ViewPager;
import android.view.View;

import java.util.Arrays;
import java.util.List;

public class PlayerPresenterTest extends AndroidUnitTest {

    private static final Urn TRACK_URN = Urn.forTrack(456L);
    private static final PlayQueueItem TRACK_PLAY_QUEUE_ITEM = TestPlayQueueItem.createTrack(TRACK_URN);
    private static final AudioAd AUDIO_AD = AdFixtures.getAudioAd(TRACK_URN);
    private static final PlayQueueItem AUDIO_AD_PLAY_QUEUE_ITEM = TestPlayQueueItem.createAudioAd(AUDIO_AD);

    @Mock private PlayerPagerPresenter playerPagerPresenter;
    @Mock private PlayQueueManager playQueueManager;
    @Mock private PlaySessionController playSessionController;
    @Mock private AdsOperations adsOperations;
    @Mock private View container;
    @Mock private PlayerPagerScrollListener playerPagerScrollListener;
    @Mock private PlayerFragment fragment;
    @Mock private FragmentManager fragmentManager;
    @Mock private PlayQueueFragment playQueueFragment;
    @Mock private FragmentTransaction fragmentTransaction;
    @Mock private PlayQueueFragmentFactory playQueueFragmentFactory;
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
        setUpFragment();
        controller = new PlayerPresenter(playerPagerPresenter,
                                         eventBus,
                                         playQueueManager,
                                         playSessionController,
                                         playerPagerScrollListener,
                                         adsOperations,
                                         playQueueFragmentFactory);
        when(playQueueManager.getPlayQueueItems(any(Predicate.class))).thenReturn(fullPlayQueue);
        when(playQueueManager.getCurrentPlayQueueItem()).thenReturn(TRACK_PLAY_QUEUE_ITEM);
        when(playerPagerPresenter.getCurrentPlayQueue()).thenReturn(fullPlayQueue);
        when(container.getResources()).thenReturn(resources());
        when(playerPagerScrollListener.getPageChangedObservable()).thenReturn(scrollStateObservable);
        controller.onCreate(fragment, null);
        controller.onViewCreated(fragment, container, null);
    }

    private void setUpFragment() {
        when(playQueueFragmentFactory.create()).thenReturn(playQueueFragment);
        when(fragmentTransaction.add(eq(R.id.player_pager_holder), isA(Fragment.class), isA(String.class)))
                .thenReturn(fragmentTransaction);
        when(fragmentTransaction.setCustomAnimations(R.anim.ak_fade_in, R.anim.ak_fade_out))
                .thenReturn(fragmentTransaction);
        when(fragmentTransaction.remove(any(Fragment.class)))
                .thenReturn(fragmentTransaction);
        when(fragmentManager.beginTransaction()).thenReturn(fragmentTransaction);
        when(fragmentManager.findFragmentByTag(PlayQueueFragment.TAG)).thenReturn(null);
        when(fragment.getFragmentManager()).thenReturn(fragmentManager);
    }

    @Test
    public void setPagerInitializesCurrentPosition() {
        controller.onViewCreated(fragment, container, null);

        verify(playerPagerPresenter, times(2)).setCurrentItem(eq(1), anyBoolean());
    }

    @Test
    public void onPlayQueueEventForTrackChangeUpdatesPagerAndAdapter() {
        eventBus.publish(EventQueue.CURRENT_PLAY_QUEUE_ITEM,
                         CurrentPlayQueueItemEvent.fromNewQueue(TRACK_PLAY_QUEUE_ITEM, Urn.NOT_SET, 0));

        verify(playerPagerPresenter, times(2)).setCurrentItem(eq(1), anyBoolean());
    }

    @Test
    public void onPlayQueueEventForNewQueueUpdatesPagerAndAdapter() {
        eventBus.publish(EventQueue.CURRENT_PLAY_QUEUE_ITEM,
                         CurrentPlayQueueItemEvent.fromNewQueue(TRACK_PLAY_QUEUE_ITEM, Urn.NOT_SET, 0));

        verify(playerPagerPresenter, times(2)).setCurrentItem(eq(1), anyBoolean());
    }

    @Test
    public void playQueueChangeAnimatesToAdjacentTracks() throws Exception {
        when(playerPagerPresenter.getCurrentItemPosition()).thenReturn(2);

        eventBus.publish(EventQueue.CURRENT_PLAY_QUEUE_ITEM,
                         CurrentPlayQueueItemEvent.fromPositionChanged(TRACK_PLAY_QUEUE_ITEM, Urn.NOT_SET, 0));

        verify(playerPagerPresenter).setCurrentItem(1, true);
    }

    @Test
    public void playQueueChangeDoesNotAnimateToNotAdjacentTracks() throws Exception {
        when(playerPagerPresenter.getCurrentItemPosition()).thenReturn(2);

        eventBus.publish(EventQueue.CURRENT_PLAY_QUEUE_ITEM,
                         CurrentPlayQueueItemEvent.fromNewQueue(TRACK_PLAY_QUEUE_ITEM, Urn.NOT_SET, 0));

        verify(playerPagerPresenter).setCurrentItem(1, false);
    }

    @Test
    public void shouldUnsubscribeFromQueuesOnUnsubscribe() {
        controller.onDestroyView(fragment);

        eventBus.verifyUnsubscribed();
    }

    @Test
    public void onPlayQueueChangedSetsNewCurrentDataInAdapter() {
        eventBus.publish(EventQueue.PLAY_QUEUE, PlayQueueEvent.fromNewQueue(Urn.NOT_SET));

        verify(playerPagerPresenter, times(2)).setCurrentPlayQueue(anyListOf(PlayQueueItem.class),
                                                                   eq(1)); //once in setPager
    }

    @Test // Fixes issue #2045, should not happen after we implement invalidating event queues on logout
    public void onPlayQueueChangedDoesNotExpandPlayerWhenPlayQueueIsEmpty() {
        when(playQueueManager.isQueueEmpty()).thenReturn(true);

        eventBus.publish(EventQueue.CURRENT_PLAY_QUEUE_ITEM,
                         CurrentPlayQueueItemEvent.fromNewQueue(TRACK_PLAY_QUEUE_ITEM, Urn.NOT_SET, 0));

        eventBus.verifyNoEventsOn(EventQueue.PLAYER_UI);
    }

    @Test
    public void changesPlayQueuePositionWhenInForegroundOnIdleStateAfterPageSelected() {
        when(playerPagerPresenter.getCurrentItem()).thenReturn(AUDIO_AD_PLAY_QUEUE_ITEM);

        controller.onResume(fragment);
        scrollStateObservable.onNext(ViewPager.SCROLL_STATE_IDLE);

        Robolectric.flushForegroundThreadScheduler();
        verify(playSessionController).setCurrentPlayQueueItem(AUDIO_AD_PLAY_QUEUE_ITEM);
    }

    @Test
    public void doesNotChangePlayQueuePositionIfNeverInForegroundOnIdleStateAfterPageSelected() {
        when(playerPagerPresenter.getCurrentItemPosition()).thenReturn(2);
        when(playerPagerPresenter.getItemAtPosition(2)).thenReturn(AUDIO_AD_PLAY_QUEUE_ITEM);

        scrollStateObservable.onNext(ViewPager.SCROLL_STATE_IDLE);

        verify(playSessionController, never()).setCurrentPlayQueueItem(any(PlayQueueItem.class));
    }

    @Test
    public void doesNotChangePlayQueuePositionWhenInBackgroundOnIdleStateAfterPageSelected() {
        when(playerPagerPresenter.getCurrentItemPosition()).thenReturn(2);
        when(playerPagerPresenter.getItemAtPosition(2)).thenReturn(AUDIO_AD_PLAY_QUEUE_ITEM);
        controller.onResume(fragment);
        controller.onPause(fragment);

        scrollStateObservable.onNext(ViewPager.SCROLL_STATE_IDLE);

        verify(playSessionController, never()).setCurrentPlayQueueItem(any(PlayQueueItem.class));
    }

    @Test
    public void callsOnTrackChangedOnIdleStateAfterPageSelected() {
        when(playerPagerPresenter.getCurrentItemPosition()).thenReturn(2);

        scrollStateObservable.onNext(ViewPager.SCROLL_STATE_IDLE);

        verify(playerPagerPresenter).onTrackChange();
    }

    @Test
    public void trackChangeEventPreventsPagerUnlockFromPreviousAudioAd() {
        setupCurrentItemAsAd(true);
        eventBus.publish(EventQueue.CURRENT_PLAY_QUEUE_ITEM,
                         CurrentPlayQueueItemEvent.fromPositionChanged(TRACK_PLAY_QUEUE_ITEM, Urn.NOT_SET, 0));
        setupCurrentItemAsAd(false);
        eventBus.publish(EventQueue.CURRENT_PLAY_QUEUE_ITEM,
                         CurrentPlayQueueItemEvent.fromPositionChanged(TRACK_PLAY_QUEUE_ITEM, Urn.NOT_SET, 0));
        Mockito.reset(playerPagerPresenter);

        final PlaybackProgress playbackProgress = new PlaybackProgress(AdConstants.UNSKIPPABLE_TIME_MS, 1L, TRACK_URN);
        eventBus.publish(EventQueue.PLAYBACK_PROGRESS, PlaybackProgressEvent.create(playbackProgress, Urn.NOT_SET));

        verifyZeroInteractions(playerPagerPresenter);
    }

    @Test
    public void trackChangeToAdSetsQueueOfSingleAdIfLookingAtAd() {
        setupCurrentItemAsAd(true);
        when(playQueueManager.getCurrentPlayQueueItem()).thenReturn(AUDIO_AD_PLAY_QUEUE_ITEM);
        setupPositionsForAd(2);

        eventBus.publish(EventQueue.CURRENT_PLAY_QUEUE_ITEM,
                         CurrentPlayQueueItemEvent.fromPositionChanged(AUDIO_AD_PLAY_QUEUE_ITEM, Urn.NOT_SET, 1));

        assertLastQueueWasAdQueue();
    }

    @Test
    public void skippableAudioAdUnlocksPagerAfterSkipInterval() {
        setupCurrentItemAsAd(true);
        when(playQueueManager.getCurrentPlayQueueItem()).thenReturn(AUDIO_AD_PLAY_QUEUE_ITEM);
        setupPositionsForAd(2);

        eventBus.publish(EventQueue.CURRENT_PLAY_QUEUE_ITEM,
                         CurrentPlayQueueItemEvent.fromPositionChanged(AUDIO_AD_PLAY_QUEUE_ITEM, Urn.NOT_SET, 1));
        InOrder inOrder = inOrder(playerPagerPresenter);
        inOrder.verify(playerPagerPresenter, times(2)).setCurrentPlayQueue(anyList(), anyInt());
        final PlaybackProgress playbackProgress = new PlaybackProgress(AdConstants.UNSKIPPABLE_TIME_MS + 1, 1L, TRACK_URN);
        eventBus.publish(EventQueue.PLAYBACK_PROGRESS,
                         PlaybackProgressEvent.create(playbackProgress, AUDIO_AD_PLAY_QUEUE_ITEM.getUrn()));

        inOrder.verify(playerPagerPresenter).setCurrentPlayQueue(playQueueItemsCaptor.capture(), anyInt());
        assertThat(playQueueItemsCaptor.getValue().size()).isGreaterThan(1);
    }

    @Test
    public void unskippableAudioAdDoesNotUnlockPagerAfterSkipInterval() {
        final PlayQueueItem audioItem = TestPlayQueueItem.createAudioAd(AdFixtures.getNonskippableAudioAd(TRACK_URN));
        when(adsOperations.getCurrentTrackAdData()).thenReturn(audioItem.getAdData());
        when(playQueueManager.getCurrentPlayQueueItem()).thenReturn(audioItem);
        setupPositionsForAd(2);

        eventBus.publish(EventQueue.CURRENT_PLAY_QUEUE_ITEM,
                         CurrentPlayQueueItemEvent.fromPositionChanged(audioItem, Urn.NOT_SET, 1));
        InOrder inOrder = inOrder(playerPagerPresenter);
        inOrder.verify(playerPagerPresenter, times(2)).setCurrentPlayQueue(anyList(), anyInt());
        final PlaybackProgress playbackProgress = new PlaybackProgress(16000L, 30000L, audioItem.getUrn());
        eventBus.publish(EventQueue.PLAYBACK_PROGRESS,
                         PlaybackProgressEvent.create(playbackProgress, audioItem.getUrn()));

        inOrder.verify(playerPagerPresenter, never()).setCurrentPlayQueue(anyList(), anyInt());
    }

    @Test
    public void unskippableAudioAdUnlocksPagerWhenTrackIsEnding() {
        final PlayQueueItem audioItem = TestPlayQueueItem.createAudioAd(AdFixtures.getNonskippableAudioAd(TRACK_URN));
        when(adsOperations.getCurrentTrackAdData()).thenReturn(audioItem.getAdData());
        when(playQueueManager.getCurrentPlayQueueItem()).thenReturn(audioItem);
        setupPositionsForAd(2);

        eventBus.publish(EventQueue.CURRENT_PLAY_QUEUE_ITEM,
                         CurrentPlayQueueItemEvent.fromPositionChanged(audioItem, Urn.NOT_SET, 1));
        InOrder inOrder = inOrder(playerPagerPresenter);
        inOrder.verify(playerPagerPresenter, times(2)).setCurrentPlayQueue(anyList(), anyInt());
        final PlaybackProgress playbackProgress = new PlaybackProgress(29900L, 30000L, audioItem.getUrn());
        eventBus.publish(EventQueue.PLAYBACK_PROGRESS,
                         PlaybackProgressEvent.create(playbackProgress, audioItem.getUrn()));

        inOrder.verify(playerPagerPresenter).setCurrentPlayQueue(anyList(), anyInt());
    }

    @Test
    public void trackChangeToAdSetsAdPlayQueueIfNotLookingAtItAndNotResumed() {
        setupCurrentItemAsAd(true);
        when(playQueueManager.getCurrentPlayQueueItem()).thenReturn(AUDIO_AD_PLAY_QUEUE_ITEM);
        setupPositionsForAd(1);

        eventBus.publish(EventQueue.CURRENT_PLAY_QUEUE_ITEM,
                         CurrentPlayQueueItemEvent.fromPositionChanged(AUDIO_AD_PLAY_QUEUE_ITEM, Urn.NOT_SET, 0));

        assertLastQueueWasAdQueue();
        verify(playerPagerPresenter).setCurrentItem(2, true);
    }

    @Test
    public void trackChangeToAdAdvancesToAdIfNotLookingAtItAndResumed() {
        setupCurrentItemAsAd(true);
        when(playQueueManager.getCurrentPlayQueueItem()).thenReturn(AUDIO_AD_PLAY_QUEUE_ITEM);
        setupPositionsForAd(1);
        controller.onResume(fragment);

        eventBus.publish(EventQueue.CURRENT_PLAY_QUEUE_ITEM,
                         CurrentPlayQueueItemEvent.fromPositionChanged(AUDIO_AD_PLAY_QUEUE_ITEM, Urn.NOT_SET, 0));

        verify(playerPagerPresenter).setCurrentItem(2, true);
    }

    @Test
    public void pageChangedAfterTrackChangeToAdSetsAdPlayQueue() {
        setupCurrentItemAsAd(true);
        when(playQueueManager.getCurrentPlayQueueItem()).thenReturn(AUDIO_AD_PLAY_QUEUE_ITEM);
        setupPositionsForAd(1);
        eventBus.publish(EventQueue.CURRENT_PLAY_QUEUE_ITEM,
                         CurrentPlayQueueItemEvent.fromPositionChanged(AUDIO_AD_PLAY_QUEUE_ITEM, Urn.NOT_SET, 0));
        setupPositionsForAd(2);

        scrollStateObservable.onNext(ViewPager.SCROLL_STATE_IDLE);

        assertLastQueueWasAdQueue();
    }

    private void setupCurrentItemAsAd(boolean isAd) {
        when(adsOperations.isCurrentItemAd()).thenReturn(isAd);
        when(adsOperations.getCurrentTrackAdData()).thenReturn(isAd
                                                               ? AUDIO_AD_PLAY_QUEUE_ITEM.getAdData()
                                                               : Optional.<AdData>absent());
    }

    @Test
    public void trackPagerRefreshesPlayQueueWhenAdRemovedAndStillVisibleAndPaused() {
        // TODO: 11/4/16  
        when(playerPagerPresenter.getCurrentItemPosition()).thenReturn(1);
        when(playerPagerPresenter.getCurrentItem()).thenReturn(AUDIO_AD_PLAY_QUEUE_ITEM);

        eventBus.publish(EventQueue.PLAY_QUEUE, PlayQueueEvent.fromAdsRemoved(Urn.NOT_SET));

        verify(playerPagerPresenter, times(2)).setCurrentPlayQueue(fullPlayQueue, 1);
        verify(playerPagerPresenter, times(2)).setCurrentItem(1, false);
    }

    @Test
    public void trackPagerRefreshesPlayQueueWhenAdRemovedAndStillVisibleAndResumedAndHasAdQueue() {
        // TODO: 11/4/16  
        when(playerPagerPresenter.getCurrentItemPosition()).thenReturn(0);
        when(playerPagerPresenter.getCurrentItem()).thenReturn(AUDIO_AD_PLAY_QUEUE_ITEM);
        when(playerPagerPresenter.getCount()).thenReturn(1);
        controller.onResume(fragment);

        eventBus.publish(EventQueue.PLAY_QUEUE, PlayQueueEvent.fromAdsRemoved(Urn.NOT_SET));

        verify(playerPagerPresenter, times(2)).setCurrentPlayQueue(fullPlayQueue, 1);
        verify(playerPagerPresenter, times(2)).setCurrentItem(1, false);
    }

    @Test
    public void trackPagerAdvancesAfterAdRemovedAndStillVisibleAndResumed() {
        when(playerPagerPresenter.getCurrentItem()).thenReturn(AUDIO_AD_PLAY_QUEUE_ITEM);
        when(playerPagerPresenter.getCount()).thenReturn(3);

        controller.onResume(fragment);

        eventBus.publish(EventQueue.PLAY_QUEUE, PlayQueueEvent.fromAdsRemoved(Urn.NOT_SET));

        // verify first fullQueue, but it should only happen once
        verify(playerPagerPresenter).setCurrentPlayQueue(fullPlayQueue, 1);
        verify(playerPagerPresenter).setCurrentItem(1, true);
    }

    @Test
    public void refreshesPlayQueueAfterNewPlayQueueEvent() {
        eventBus.publish(EventQueue.PLAY_QUEUE, PlayQueueEvent.fromQueueUpdate(Urn.NOT_SET));

        verify(playerPagerPresenter, times(2)).setCurrentPlayQueue(fullPlayQueue, 1);
        verify(playerPagerPresenter, times(2)).setCurrentItem(1, false);
    }

    @Test
    public void refreshPlayQueueAfterAdRemovedAndScrolled() {
        when(playerPagerPresenter.getCurrentItemPosition()).thenReturn(1);
        when(playerPagerPresenter.getCurrentItem()).thenReturn(AUDIO_AD_PLAY_QUEUE_ITEM);

        eventBus.publish(EventQueue.PLAY_QUEUE, PlayQueueEvent.fromAdsRemoved(Urn.NOT_SET));

        scrollStateObservable.onNext(ViewPager.SCROLL_STATE_IDLE);

        verify(playerPagerPresenter, times(2)).setCurrentPlayQueue(fullPlayQueue, 1);
        verify(playerPagerPresenter, times(2)).setCurrentItem(1, false);
    }

    @Test
    public void displayPlayQueue() {
        TestSubscriber subscriber = TestSubscriber.create();
        eventBus.subscribe(EventQueue.PLAYER_COMMAND, subscriber);
        eventBus.publish(EventQueue.PLAY_QUEUE_UI, PlayQueueUIEvent.createDisplayEvent());

        verify(fragmentManager, times(1)).beginTransaction();
        verify(fragmentTransaction, times(1)).add(any(Integer.class), any(Fragment.class), eq(PlayQueueFragment.TAG));
        subscriber.assertValue(PlayerUICommand.lockPlayQueue());
    }

    @Test
    public void hidePlayQueue() {
        TestSubscriber subscriber = TestSubscriber.create();
        eventBus.publish(EventQueue.PLAY_QUEUE_UI, PlayQueueUIEvent.createDisplayEvent());
        eventBus.subscribe(EventQueue.PLAYER_COMMAND, subscriber);
        when(fragmentManager.findFragmentByTag(PlayQueueFragment.TAG)).thenReturn(playQueueFragment);
        eventBus.publish(EventQueue.PLAY_QUEUE_UI, PlayQueueUIEvent.createHideEvent());

        verify(fragmentManager, times(2)).beginTransaction();
        verify(fragmentTransaction, times(1)).remove(any(Fragment.class));
        subscriber.assertValue(PlayerUICommand.unlockPlayQueue());
    }

    private void setupPositionsForAd(int pagerPosition) {
        when(playerPagerPresenter.getCurrentItemPosition()).thenReturn(pagerPosition);
        when(playerPagerPresenter.getItemAtPosition(pagerPosition)).thenReturn(AUDIO_AD_PLAY_QUEUE_ITEM);
    }

    private void assertLastQueueWasAdQueue() {
        verify(playerPagerPresenter, atLeastOnce()).setCurrentPlayQueue(playQueueItemsCaptor.capture(), anyInt());
        final List<List<PlayQueueItem>> allValues = playQueueItemsCaptor.getAllValues();
        assertPlayQueueItemsEqual(Arrays.asList(AUDIO_AD_PLAY_QUEUE_ITEM), allValues.get(allValues.size() - 1));
    }
}
