package com.soundcloud.android.playback;

import static com.soundcloud.android.testsupport.fixtures.TestPlayStates.wrap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.ads.AdFixtures;
import com.soundcloud.android.ads.VideoAd;
import com.soundcloud.android.analytics.PromotedSourceInfo;
import com.soundcloud.android.events.PlaybackProgressEvent;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.fixtures.PlayableFixtures;
import com.soundcloud.java.optional.Optional;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InOrder;
import org.mockito.Mock;

import java.util.Collections;

public class PlaybackAnalyticsControllerTest extends AndroidUnitTest {

    private final Urn track = Urn.forTrack(123L);
    private PlaybackItem playbackItem = AudioPlaybackItem.create(PlayableFixtures.fromApiTrack(), 123L);

    @Mock private TrackSessionAnalyticsDispatcher trackSessionDispatcher;
    @Mock private AdSessionAnalyticsDispatcher adSessionDispatcher;
    @Mock private PlayQueueManager playQueueManager;

    private PlaybackAnalyticsController controller;

    @Before
    public void setUp() throws Exception {
        controller = new PlaybackAnalyticsController(trackSessionDispatcher, adSessionDispatcher, playQueueManager);
        when(playQueueManager.getCurrentPlaySessionSource()).thenReturn(new PlaySessionSource("stream"));
    }

    @Test
    public void onProgressForwardsAudioAdProgressEventToAdAnalyticsDispatcher() {
        playbackItem = AudioAdPlaybackItem.create(AdFixtures.getAudioAd(track));
        PlaybackProgressEvent progressEvent = PlaybackProgressEvent.create(PlaybackProgress.empty(), track);

        controller.onProgressEvent(playbackItem, progressEvent);

        verify(adSessionDispatcher).onProgressEvent(progressEvent);
        verify(trackSessionDispatcher, never()).onProgressEvent(any(PlaybackProgressEvent.class));
    }

    @Test
    public void onProgressForwardsCheckpointEventIfCheckpointIntervalHasPassed() {
        PlaybackStateTransition transition = new PlaybackStateTransition(PlaybackState.PLAYING,
                                                                         PlayStateReason.NONE,
                                                                         track, 0, 0);
        controller.onStateTransition(playbackItem, wrap(transition));

        final long position = TrackSessionAnalyticsDispatcher.CHECKPOINT_INTERVAL;
        final PlaybackProgressEvent progress = PlaybackProgressEvent.create(new PlaybackProgress(position, 90000L, track),
                                                                            track);
        controller.onProgressEvent(playbackItem, progress);

        InOrder inOrder = inOrder(trackSessionDispatcher);
        inOrder.verify(trackSessionDispatcher).onPlayTransition(wrap(transition), true);
        inOrder.verify(trackSessionDispatcher).onProgressCheckpoint(wrap(transition), progress);
    }

    @Test
    public void onProgressDoesntForwardCheckpointEventIfCheckpointIntervalHasNotPassed() {
        PlaybackStateTransition transition = new PlaybackStateTransition(PlaybackState.PLAYING,
                                                                         PlayStateReason.NONE,
                                                                         track, 0, 0);
        controller.onStateTransition(playbackItem, wrap(transition));

        final long position = TrackSessionAnalyticsDispatcher.CHECKPOINT_INTERVAL - 1;
        final PlaybackProgressEvent progress = PlaybackProgressEvent.create(new PlaybackProgress(position, 90000L, track),
                                                                            track);
        controller.onProgressEvent(playbackItem, progress);

        verify(trackSessionDispatcher, never()).onProgressCheckpoint(any(PlayStateEvent.class),
                                                                     any(PlaybackProgressEvent.class));
    }

    @Test
    public void onProgressForwardsCheckpointEventForAdsIfCheckpointIntervalHasPassed() {
        playbackItem = AudioAdPlaybackItem.create(AdFixtures.getAudioAd(track));
        PlaybackStateTransition transition = new PlaybackStateTransition(PlaybackState.PLAYING,
                                                                         PlayStateReason.NONE,
                                                                         playbackItem.getUrn(), 0, 0);
        controller.onStateTransition(playbackItem, wrap(transition));

        final long position = AdSessionAnalyticsDispatcher.CHECKPOINT_INTERVAL;
        final PlaybackProgressEvent progress = PlaybackProgressEvent.create(new PlaybackProgress(position, 90000L, track),
                                                                            playbackItem.getUrn());
        controller.onProgressEvent(playbackItem, progress);

        InOrder inOrder = inOrder(adSessionDispatcher);
        inOrder.verify(adSessionDispatcher).onPlayTransition(wrap(transition), true);
        inOrder.verify(adSessionDispatcher).onProgressCheckpoint(wrap(transition), progress);
    }

    @Test
    public void onProgressDoesntForwardCheckpointEventForAdsIfCheckpointIntervalHasNotPassed() {
        playbackItem = AudioAdPlaybackItem.create(AdFixtures.getAudioAd(track));
        PlaybackStateTransition transition = new PlaybackStateTransition(PlaybackState.PLAYING,
                                                                         PlayStateReason.NONE,
                                                                         playbackItem.getUrn(), 0, 0);
        controller.onStateTransition(playbackItem, wrap(transition));

        final long position = AdSessionAnalyticsDispatcher.CHECKPOINT_INTERVAL - 1;
        final PlaybackProgressEvent progress = PlaybackProgressEvent.create(new PlaybackProgress(position, 90000L, track),
                                                                            playbackItem.getUrn());
        controller.onProgressEvent(playbackItem, progress);

        verify(adSessionDispatcher, never()).onProgressCheckpoint(any(PlayStateEvent.class),
                                                                  any(PlaybackProgressEvent.class));
    }

    @Test
    public void onPlaystateChangedForwardsPlayTransitionToDispatcher() {
        PlaybackStateTransition transition = new PlaybackStateTransition(PlaybackState.PLAYING,
                                                                         PlayStateReason.NONE,
                                                                         track, 0, 0);

        controller.onStateTransition(playbackItem, wrap(transition));

        verify(trackSessionDispatcher).onPlayTransition(wrap(transition), true);
        verify(adSessionDispatcher, never()).onPlayTransition(any(PlayStateEvent.class), anyBoolean());
    }

    @Test
    public void onPlaystateChangedForwardsAdPlayTransitionToDispatcher() {
        final VideoAd videoAd = AdFixtures.getVideoAd(track);
        playbackItem = VideoAdPlaybackItem.create(videoAd, 0L);
        PlaybackStateTransition transition = new PlaybackStateTransition(PlaybackState.PLAYING,
                                                                         PlayStateReason.NONE,
                                                                         track, 0, 0);

        controller.onStateTransition(playbackItem, wrap(transition));

        verify(adSessionDispatcher).onPlayTransition(wrap(transition), true);
        verify(trackSessionDispatcher, never()).onPlayTransition(any(PlayStateEvent.class), anyBoolean());
    }

    @Test
    public void onPlayTransitionSetsFlagForSameTrack() {
        PlaybackStateTransition transition1 = new PlaybackStateTransition(PlaybackState.PLAYING,
                                                                          PlayStateReason.NONE,
                                                                          track, 0, 0);
        PlaybackStateTransition transition2 = new PlaybackStateTransition(PlaybackState.PLAYING,
                                                                          PlayStateReason.NONE,
                                                                          track, 0, 0);

        controller.onStateTransition(playbackItem, wrap(transition1));
        controller.onStateTransition(playbackItem, wrap(transition2));

        InOrder inOrder = inOrder(trackSessionDispatcher);
        inOrder.verify(trackSessionDispatcher).onPlayTransition(wrap(transition1), true);
        inOrder.verify(trackSessionDispatcher).onPlayTransition(wrap(transition2), false);
    }

    @Test
    public void onPlaystateChangedForwardsStopTransitionToDispatcher() {
        PlaybackStateTransition transition = new PlaybackStateTransition(PlaybackState.IDLE,
                                                                         PlayStateReason.NONE,
                                                                         track, 0, 0);

        controller.onStateTransition(playbackItem, wrap(transition));

        verify(trackSessionDispatcher).onStopTransition(wrap(transition), true);
        verify(adSessionDispatcher, never()).onStopTransition(any(PlayStateEvent.class), anyBoolean());
    }

    @Test
    public void onPlaystateChangedForwardsSkipTransitionToDispatcher() {
        final VideoAd videoAd = AdFixtures.getVideoAd(Urn.forTrack(123));
        PlaybackItem playbackItem2 = VideoAdPlaybackItem.create(videoAd, 0L);
        PlaybackStateTransition transition1 = new PlaybackStateTransition(PlaybackState.PLAYING,
                                                                          PlayStateReason.NONE,
                                                                          track, 0, 0);
        PlaybackStateTransition transition2 = new PlaybackStateTransition(PlaybackState.PLAYING,
                                                                          PlayStateReason.NONE,
                                                                          Urn.forAd("dfp", "321-123"), 0, 0);

        controller.onStateTransition(playbackItem, wrap(transition1));
        controller.onStateTransition(playbackItem2, wrap(transition2));

        InOrder inOrder = inOrder(trackSessionDispatcher, adSessionDispatcher);
        inOrder.verify(trackSessionDispatcher).onPlayTransition(wrap(transition1), true);
        inOrder.verify(trackSessionDispatcher).onSkipTransition(wrap(transition1));
        inOrder.verify(adSessionDispatcher).onPlayTransition(wrap(transition2), true);
    }

    @Test
    public void onPlaystateChangedForNewItemResetsPromotedSourceInfo() {
        PlaybackStateTransition transition = new PlaybackStateTransition(PlaybackState.PLAYING,
                                                                         PlayStateReason.NONE,
                                                                         track, 0, 0);
        PromotedSourceInfo promotedInfo = new PromotedSourceInfo("ad:123",
                                                                 track,
                                                                 Optional.<Urn>absent(),
                                                                 Collections.EMPTY_LIST);
        promotedInfo.setPlaybackStarted();
        PlaySessionSource sessionSource = new PlaySessionSource("stream");
        sessionSource.setPromotedSourceInfo(promotedInfo);
        when(playQueueManager.getCurrentPlaySessionSource()).thenReturn(sessionSource);
        assertThat(promotedInfo.isPlaybackStarted()).isTrue();

        controller.onStateTransition(playbackItem, wrap(transition));

        assertThat(promotedInfo.isPlaybackStarted()).isFalse();
    }

}
