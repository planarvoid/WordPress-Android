package com.soundcloud.android.playback;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.ads.AdsController;
import com.soundcloud.android.ads.AdsOperations;
import com.soundcloud.android.cast.CastConnectionHelper;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.main.Screen;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.ui.view.PlaybackToastHelper;
import com.soundcloud.android.properties.FeatureFlags;
import com.soundcloud.android.properties.Flag;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.fixtures.TestPlayQueueItem;
import com.soundcloud.android.testsupport.fixtures.TestPlayStates;
import com.soundcloud.android.utils.NetworkConnectionHelper;
import com.soundcloud.java.collections.Iterables;
import com.soundcloud.java.functions.Predicate;
import com.soundcloud.rx.eventbus.TestEventBus;
import org.assertj.core.util.Lists;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import java.util.List;

public class PlayQueueAdvancerTest extends AndroidUnitTest {

    private PlayQueueItem trackPlayQueueItem;
    private Urn trackUrn;
    private TestEventBus eventBus = new TestEventBus();

    @Mock private PlayQueueManager playQueueManager;
    @Mock private AdsOperations adsOperations;
    @Mock private AdsController adsController;
    @Mock private CastConnectionHelper castConnectionHelper;
    @Mock private NetworkConnectionHelper networkConnectionHelper;
    @Mock private PlaybackToastHelper playbackToastHelper;
    @Mock private AccountOperations accountOperations;
    @Mock private FeatureFlags featureFlags;
    @Mock private PlaySessionController playSessionController;

    private PlayQueueAdvancer advancer;

    @Before
    public void setUp() throws Exception {
        advancer = new PlayQueueAdvancer(eventBus,
                                         playQueueManager,
                                         networkConnectionHelper,
                                         playSessionController,
                                         adsController);
        advancer.subscribe();

        trackUrn = TestPlayStates.URN;
        trackPlayQueueItem = TestPlayQueueItem.createTrack(trackUrn);

        when(playQueueManager.getCurrentPlayQueueItem()).thenReturn(trackPlayQueueItem);
        when(playQueueManager.isCurrentItem(trackUrn)).thenReturn(true);
        when(playQueueManager.getCurrentTrackSourceInfo()).thenReturn(new TrackSourceInfo("origin screen", true));
        when(playQueueManager.getCollectionUrn()).thenReturn(Urn.NOT_SET);
        when(playQueueManager.getCurrentPlaySessionSource()).thenReturn(PlaySessionSource.EMPTY);
        when(accountOperations.getLoggedInUserUrn()).thenReturn(Urn.forUser(456L));
        when(playQueueManager.getUpcomingPlayQueueItems(anyInt())).thenReturn(Lists.<Urn>newArrayList());
        when(featureFlags.isEnabled(Flag.EXPLODE_PLAYLISTS_IN_PLAYQUEUES)).thenReturn(true);
    }

    @Test
    public void onStateTransitionDoesNotTryToAdvanceItemIfTrackNotEnded() throws Exception {
        eventBus.publish(EventQueue.PLAYBACK_STATE_CHANGED, TestPlayStates.playing());
        eventBus.publish(EventQueue.PLAYBACK_STATE_CHANGED, TestPlayStates.buffering());
        eventBus.publish(EventQueue.PLAYBACK_STATE_CHANGED, TestPlayStates.error(PlayStateReason.ERROR_FAILED));
        verify(playQueueManager, never()).moveToNextPlayableItem();
    }

    @Test
    public void onStateTransitionDoesNotAdvanceItemIfTrackEndedWithNotFoundErrorAndNotUserTriggeredWithNoConnection() {
        when(playQueueManager.getCurrentTrackSourceInfo()).thenReturn(new TrackSourceInfo(Screen.ACTIVITIES.get(),
                                                                                          false));
        eventBus.publish(EventQueue.PLAYBACK_STATE_CHANGED,TestPlayStates.error(PlayStateReason.ERROR_NOT_FOUND));
        verify(playQueueManager, never()).moveToNextPlayableItem();
    }

    @Test
    public void onStateTransitionToAdvanceItemIfTrackEndedWithNotFoundErrorAndNotUserTriggeredWithConnection() {
        when(playQueueManager.getCurrentTrackSourceInfo()).thenReturn(new TrackSourceInfo(Screen.ACTIVITIES.get(),
                                                                                          false));
        when(networkConnectionHelper.isNetworkConnected()).thenReturn(true);
        when(playQueueManager.autoMoveToNextPlayableItem()).thenReturn(true);
        eventBus.publish(EventQueue.PLAYBACK_STATE_CHANGED,TestPlayStates.error(PlayStateReason.ERROR_NOT_FOUND));
        verify(playQueueManager).autoMoveToNextPlayableItem();
    }

    @Test
    public void onStateTransitionDoesNotTryToAdvanceItemIfTrackEndedWithNotFoundErrorAndUserTriggered() {
        when(playQueueManager.getCurrentTrackSourceInfo()).thenReturn(new TrackSourceInfo(Screen.ACTIVITIES.get(),
                                                                                          true));
        eventBus.publish(EventQueue.PLAYBACK_STATE_CHANGED,TestPlayStates.error(PlayStateReason.ERROR_NOT_FOUND));
        verify(playQueueManager, never()).moveToNextPlayableItem();
    }

    @Test
    public void onStateTransitionDoesNotTryToAdvanceItemIfTrackEndedWithForbiddenErrorAndNotUserTriggeredAndNoInternet() {
        when(playQueueManager.getCurrentTrackSourceInfo()).thenReturn(new TrackSourceInfo(Screen.ACTIVITIES.get(),
                                                                                          false));
        eventBus.publish(EventQueue.PLAYBACK_STATE_CHANGED,TestPlayStates.error(PlayStateReason.ERROR_FORBIDDEN));
        verify(playQueueManager, never()).moveToNextPlayableItem();
    }

    @Test
    public void onStateTransitionToAdvanceItemIfTrackEndedWithForbiddenErrorAndNotUserTriggeredWithConnection() {
        when(playQueueManager.getCurrentTrackSourceInfo()).thenReturn(new TrackSourceInfo(Screen.ACTIVITIES.get(),
                                                                                          false));
        when(networkConnectionHelper.isNetworkConnected()).thenReturn(true);
        when(playQueueManager.autoMoveToNextPlayableItem()).thenReturn(true);
        eventBus.publish(EventQueue.PLAYBACK_STATE_CHANGED,TestPlayStates.error(PlayStateReason.ERROR_FORBIDDEN));
        verify(playQueueManager).autoMoveToNextPlayableItem();
    }

    @Test
    public void onStateTransitionDoesNotTryToAdvanceItemIfTrackEndedWithForbiddenErrorAndUserTriggered() {
        when(playQueueManager.getCurrentTrackSourceInfo()).thenReturn(new TrackSourceInfo(Screen.ACTIVITIES.get(),
                                                                                          true));
        eventBus.publish(EventQueue.PLAYBACK_STATE_CHANGED,TestPlayStates.error(PlayStateReason.ERROR_FORBIDDEN));
        verify(playQueueManager, never()).moveToNextPlayableItem();
    }

    @Test
    public void onStateTransitionDoesNotTryToAdvanceItemIfTrackEndedWithFailedErrorAndNotUserTriggered() {
        when(playQueueManager.getCurrentTrackSourceInfo()).thenReturn(new TrackSourceInfo(Screen.ACTIVITIES.get(),
                                                                                          false));
        eventBus.publish(EventQueue.PLAYBACK_STATE_CHANGED,TestPlayStates.error(PlayStateReason.ERROR_FAILED));
        verify(playQueueManager, never()).moveToNextPlayableItem();
    }

    @Test
    public void onStateTransitionDoesNotAdvanceTracksIfNotCurrentPlayQueueTrack() {
        when(playQueueManager.isCurrentItem(trackUrn)).thenReturn(false);
        eventBus.publish(EventQueue.PLAYBACK_STATE_CHANGED, TestPlayStates.complete());
        verify(playQueueManager, never()).autoMoveToNextPlayableItem();
    }

    @Test
    public void onStateTransitionTriesToAdvanceItemIfTrackEndedWhileCasting() {
        when(castConnectionHelper.isCasting()).thenReturn(true);
        eventBus.publish(EventQueue.PLAYBACK_STATE_CHANGED, TestPlayStates.complete());
        verify(playQueueManager).autoMoveToNextPlayableItem();
    }

    @Test
    public void onStateTransitionTriesToAdvanceItem() {
        eventBus.publish(EventQueue.PLAYBACK_STATE_CHANGED, TestPlayStates.complete());
        verify(playQueueManager).autoMoveToNextPlayableItem();
    }

    @Test
    public void onStateTransitionTriesToAdvanceWhenCurrentItemIsVideo() {
        final Urn videoUrn = Urn.forAd("dfp", "video-ad");
        when(playQueueManager.isCurrentItem(videoUrn)).thenReturn(true);

        eventBus.publish(EventQueue.PLAYBACK_STATE_CHANGED, TestPlayStates.complete(videoUrn));

        verify(playQueueManager).autoMoveToNextPlayableItem();
    }

    @Test
    public void onStateTransitionTriesToReconfigureAd() {
        eventBus.publish(EventQueue.PLAYBACK_STATE_CHANGED, TestPlayStates.complete());
        verify(adsController).reconfigureAdForNextTrack();
        verify(adsController).publishAdDeliveryEventIfUpcoming();
    }

    @Test
    public void onStateTransitionDoesNotOpenCurrentTrackAfterFailingToAdvancePlayQueue() throws Exception {
        when(playQueueManager.moveToNextPlayableItem()).thenReturn(false);
        eventBus.publish(EventQueue.PLAYBACK_STATE_CHANGED, TestPlayStates.complete());
        verifyZeroInteractions(playSessionController);
    }

    @Test
    public void onStateTransitionPublishesPlayQueueCompleteEventAfterFailingToAdvancePlayQueue() throws Exception {
        when(playQueueManager.moveToNextPlayableItem()).thenReturn(false);
        eventBus.publish(EventQueue.PLAYBACK_STATE_CHANGED, TestPlayStates.complete());

        final List<PlayStateEvent> playStateEvents = eventBus.eventsOn(EventQueue.PLAYBACK_STATE_CHANGED);
        assertThat(Iterables.filter(playStateEvents, new Predicate<PlayStateEvent>() {
            @Override
            public boolean apply(PlayStateEvent event) {
                return event.isPlayQueueComplete();
            }
        })).hasSize(1);
    }
}
