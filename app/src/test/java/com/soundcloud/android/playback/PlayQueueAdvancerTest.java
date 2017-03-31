package com.soundcloud.android.playback;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import com.soundcloud.android.PlaybackServiceController;
import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.ads.AdsController;
import com.soundcloud.android.ads.AdsOperations;
import com.soundcloud.android.cast.CastConnectionHelper;
import com.soundcloud.android.main.Screen;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.ui.view.PlaybackFeedbackHelper;
import com.soundcloud.android.properties.FeatureFlags;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.fixtures.TestPlayQueueItem;
import com.soundcloud.android.testsupport.fixtures.TestPlayStates;
import com.soundcloud.android.utils.NetworkConnectionHelper;
import org.assertj.core.util.Lists;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

public class PlayQueueAdvancerTest extends AndroidUnitTest {

    private PlayQueueItem trackPlayQueueItem;
    private Urn trackUrn;

    @Mock private PlayQueueManager playQueueManager;
    @Mock private AdsOperations adsOperations;
    @Mock private AdsController adsController;
    @Mock private NetworkConnectionHelper networkConnectionHelper;
    @Mock private PlaybackFeedbackHelper playbackFeedbackHelper;
    @Mock private AccountOperations accountOperations;
    @Mock private FeatureFlags featureFlags;
    @Mock private PlaySessionController playSessionController;
    @Mock private PlaybackServiceController serviceInitiator;
    @Mock private CastConnectionHelper castConnectionHelper;

    private PlayQueueAdvancer advancer;

    @Before
    public void setUp() throws Exception {
        advancer = new PlayQueueAdvancer(
                playQueueManager,
                networkConnectionHelper,
                playSessionController,
                adsController, serviceInitiator, castConnectionHelper);
        trackUrn = TestPlayStates.URN;
        trackPlayQueueItem = TestPlayQueueItem.createTrack(trackUrn);

        when(playQueueManager.getCurrentPlayQueueItem()).thenReturn(trackPlayQueueItem);
        when(playQueueManager.isCurrentItem(trackUrn)).thenReturn(true);
        when(playQueueManager.getCurrentTrackSourceInfo()).thenReturn(new TrackSourceInfo("origin screen", true));
        when(playQueueManager.getCollectionUrn()).thenReturn(Urn.NOT_SET);
        when(playQueueManager.getCurrentPlaySessionSource()).thenReturn(PlaySessionSource.EMPTY);
        when(accountOperations.getLoggedInUserUrn()).thenReturn(Urn.forUser(456L));
        when(playQueueManager.getUpcomingPlayQueueItems(anyInt())).thenReturn(Lists.newArrayList());
    }

    @Test
    public void onStateTransitionDoesNotTryToAdvanceItemIfTrackNotEnded() throws Exception {
        advancer.onPlayStateChanged(TestPlayStates.playing());
        advancer.onPlayStateChanged(TestPlayStates.buffering());

        assertThat(advancer.onPlayStateChanged(TestPlayStates.error(PlayStateReason.ERROR_FAILED))).isSameAs(
                PlayQueueAdvancer.Result.NO_OP);
        verify(playQueueManager, never()).moveToNextPlayableItem();
    }

    @Test
    public void onStateTransitionDoesNotAdvanceItemIfTrackEndedWithNotFoundErrorAndNotUserTriggeredWithNoConnection() {
        when(playQueueManager.getCurrentTrackSourceInfo()).thenReturn(new TrackSourceInfo(Screen.ACTIVITIES.get(),
                                                                                          false));
        assertThat(advancer.onPlayStateChanged(TestPlayStates.error(PlayStateReason.ERROR_NOT_FOUND)))
                .isSameAs(PlayQueueAdvancer.Result.NO_OP);
        verify(playQueueManager, never()).moveToNextPlayableItem();
    }

    @Test
    public void onStateTransitionToAdvanceItemIfTrackEndedWithNotFoundErrorAndNotUserTriggeredWithConnection() {
        when(playQueueManager.getCurrentTrackSourceInfo()).thenReturn(new TrackSourceInfo(Screen.ACTIVITIES.get(),
                                                                                          false));
        when(networkConnectionHelper.isNetworkConnected()).thenReturn(true);
        when(playQueueManager.autoMoveToNextPlayableItem()).thenReturn(true);

        assertThat(advancer.onPlayStateChanged(TestPlayStates.error(PlayStateReason.ERROR_NOT_FOUND)))
                .isSameAs(PlayQueueAdvancer.Result.ADVANCED);
        verify(playQueueManager).autoMoveToNextPlayableItem();
    }

    @Test
    public void onStateTransitionDoesNotTryToAdvanceItemIfTrackEndedWithNotFoundErrorAndUserTriggered() {
        when(playQueueManager.getCurrentTrackSourceInfo()).thenReturn(new TrackSourceInfo(Screen.ACTIVITIES.get(),
                                                                                          true));
        assertThat(advancer.onPlayStateChanged(TestPlayStates.error(PlayStateReason.ERROR_NOT_FOUND)))
                .isSameAs(PlayQueueAdvancer.Result.NO_OP);

        verify(playQueueManager, never()).moveToNextPlayableItem();
    }

    @Test
    public void onStateTransitionDoesNotTryToAdvanceItemIfTrackEndedWithForbiddenErrorAndNotUserTriggeredAndNoInternet() {
        when(playQueueManager.getCurrentTrackSourceInfo()).thenReturn(new TrackSourceInfo(Screen.ACTIVITIES.get(),
                                                                                          false));
        assertThat(advancer.onPlayStateChanged(TestPlayStates.error(PlayStateReason.ERROR_FORBIDDEN)))
                .isSameAs(PlayQueueAdvancer.Result.NO_OP);
        verify(playQueueManager, never()).moveToNextPlayableItem();
    }

    @Test
    public void onStateTransitionToAdvanceItemIfTrackEndedWithForbiddenErrorAndNotUserTriggeredWithConnection() {
        when(playQueueManager.getCurrentTrackSourceInfo()).thenReturn(new TrackSourceInfo(Screen.ACTIVITIES.get(),
                                                                                          false));
        when(networkConnectionHelper.isNetworkConnected()).thenReturn(true);
        when(playQueueManager.autoMoveToNextPlayableItem()).thenReturn(true);
        assertThat(advancer.onPlayStateChanged(TestPlayStates.error(PlayStateReason.ERROR_NOT_FOUND)))
                .isSameAs(PlayQueueAdvancer.Result.ADVANCED);
        verify(playQueueManager).autoMoveToNextPlayableItem();
    }

    @Test
    public void onStateTransitionDoesNotTryToAdvanceItemIfTrackEndedWithForbiddenErrorAndUserTriggered() {
        when(playQueueManager.getCurrentTrackSourceInfo()).thenReturn(new TrackSourceInfo(Screen.ACTIVITIES.get(),
                                                                                          true));
        assertThat(advancer.onPlayStateChanged(TestPlayStates.error(PlayStateReason.ERROR_FORBIDDEN))).isSameAs(
                PlayQueueAdvancer.Result.NO_OP);
        verify(playQueueManager, never()).moveToNextPlayableItem();
    }

    @Test
    public void onStateTransitionDoesNotTryToAdvanceItemIfTrackEndedWithFailedErrorAndNotUserTriggered() {
        when(playQueueManager.getCurrentTrackSourceInfo()).thenReturn(new TrackSourceInfo(Screen.ACTIVITIES.get(),
                                                                                          false));
        assertThat(advancer.onPlayStateChanged(TestPlayStates.error(PlayStateReason.ERROR_FAILED))).isSameAs(
                PlayQueueAdvancer.Result.NO_OP);
        verify(playQueueManager, never()).moveToNextPlayableItem();
    }

    @Test
    public void onStateTransitionDoesNotAdvanceTracksIfNotCurrentPlayQueueTrack() {
        when(playQueueManager.isCurrentItem(trackUrn)).thenReturn(false);
        assertThat(advancer.onPlayStateChanged(TestPlayStates.complete())).isSameAs(PlayQueueAdvancer.Result.NO_OP);
        verify(playQueueManager, never()).autoMoveToNextPlayableItem();
    }

    @Test
    public void onStateTransitionDoesNotAdvanceTracksIfCasting() {
        when(castConnectionHelper.isCasting()).thenReturn(true);

        assertThat(advancer.onPlayStateChanged(TestPlayStates.complete())).isSameAs(PlayQueueAdvancer.Result.NO_OP);
        verify(playQueueManager, never()).autoMoveToNextPlayableItem();
    }

    @Test
    public void onStateTransitionTriesToAdvanceItem() {
        advancer.onPlayStateChanged(TestPlayStates.complete());
        verify(playQueueManager).autoMoveToNextPlayableItem();
    }

    @Test
    public void onStateTransitionTriesToAdvanceWhenCurrentItemIsVideo() {
        final Urn videoUrn = Urn.forAd("dfp", "video-ad");
        when(playQueueManager.isCurrentItem(videoUrn)).thenReturn(true);
        when(playQueueManager.autoMoveToNextPlayableItem()).thenReturn(true);

        assertThat(advancer.onPlayStateChanged(TestPlayStates.complete(videoUrn))).isSameAs(PlayQueueAdvancer.Result.ADVANCED);
        verify(playQueueManager).autoMoveToNextPlayableItem();
    }

    @Test
    public void onStateTransitionTriesToReconfigureAd() {
        advancer.onPlayStateChanged(TestPlayStates.complete());
        verify(adsController).reconfigureAdForNextTrack();
        verify(adsController).publishAdDeliveryEventIfUpcoming();
    }

    @Test
    public void onStateTransitionDoesNotOpenCurrentTrackAfterFailingToAdvancePlayQueue() throws Exception {
        when(playQueueManager.moveToNextPlayableItem()).thenReturn(false);
        assertThat(advancer.onPlayStateChanged(TestPlayStates.complete())).isSameAs(PlayQueueAdvancer.Result.QUEUE_COMPLETE);
        verifyZeroInteractions(playSessionController);
    }

    @Test
    public void onStateTransitionReportsPlayQueueCompleteEventAfterFailingToAdvancePlayQueue() throws Exception {
        when(playQueueManager.moveToNextPlayableItem()).thenReturn(false);
        assertThat(advancer.onPlayStateChanged(TestPlayStates.complete())).isSameAs(PlayQueueAdvancer.Result.QUEUE_COMPLETE);
    }

    @Test
    public void onStateTransitionStopsPlaybackServiceAfterFailingToAdvancePlayQueue() throws Exception {
        when(playQueueManager.moveToNextPlayableItem()).thenReturn(false);
        advancer.onPlayStateChanged(TestPlayStates.complete());
        verify(serviceInitiator).stopPlaybackService();
    }
}
