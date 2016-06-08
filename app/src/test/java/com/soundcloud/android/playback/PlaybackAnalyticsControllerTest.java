package com.soundcloud.android.playback;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.soundcloud.android.ads.AdFixtures;
import com.soundcloud.android.ads.VideoAd;
import com.soundcloud.android.events.PlaybackProgressEvent;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.fixtures.TestPropertySets;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InOrder;
import org.mockito.Mock;

public class PlaybackAnalyticsControllerTest extends AndroidUnitTest {

    private PlaybackItem playbackItem = AudioPlaybackItem.create(TestPropertySets.fromApiTrack(), 123L);

    @Mock private TrackSessionAnalyticsDispatcher trackSessionDispatcher;
    @Mock private AdSessionAnalyticsDispatcher adSessionDispatcher;

    private PlaybackAnalyticsController dispatcher;

    @Before
    public void setUp() throws Exception {
        dispatcher = new PlaybackAnalyticsController(trackSessionDispatcher, adSessionDispatcher);
    }

    @Test
    public void onProgressForwardsAudioAdProgressEventToAdAnalyticsDispatcher() {
        playbackItem = AudioAdPlaybackItem.create(TestPropertySets.fromApiTrack(), AdFixtures.getAudioAd(Urn.forTrack(123L)));
        PlaybackProgressEvent progressEvent = PlaybackProgressEvent.create(PlaybackProgress.empty(), Urn.forTrack(123L));

        dispatcher.onProgressEvent(playbackItem, progressEvent);

        verify(adSessionDispatcher).onProgressEvent(progressEvent);
        verify(trackSessionDispatcher, never()).onProgressEvent(any(PlaybackProgressEvent.class));
    }

    @Test
    public void onPlaystateChangedForwardsPlayTransitionToDispatcher() {
        PlaybackStateTransition transition = new PlaybackStateTransition(PlaybackState.PLAYING, PlayStateReason.NONE, Urn.forTrack(123L));

        dispatcher.onStateTransition(playbackItem, transition);

        verify(trackSessionDispatcher).onPlayTransition(transition, true);
        verify(adSessionDispatcher, never()).onPlayTransition(any(PlaybackStateTransition.class), anyBoolean());
    }

    @Test
    public void onPlaystateChangedForwardsAdPlayTransitionToDispatcher() {
        final VideoAd videoAd = AdFixtures.getVideoAd(Urn.forTrack(123));
        playbackItem = VideoAdPlaybackItem.create(videoAd, 0L);
        PlaybackStateTransition transition = new PlaybackStateTransition(PlaybackState.PLAYING, PlayStateReason.NONE, Urn.forTrack(123L));

        dispatcher.onStateTransition(playbackItem, transition);

        verify(adSessionDispatcher).onPlayTransition(transition, true);
        verify(trackSessionDispatcher, never()).onPlayTransition(any(PlaybackStateTransition.class), anyBoolean());
    }

    @Test
    public void onPlayTransitionSetsFlagForSameTrack()  {
        PlaybackStateTransition transition1 = new PlaybackStateTransition(PlaybackState.PLAYING, PlayStateReason.NONE, Urn.forTrack(123L));
        PlaybackStateTransition transition2 = new PlaybackStateTransition(PlaybackState.PLAYING, PlayStateReason.NONE, Urn.forTrack(123L));

        dispatcher.onStateTransition(playbackItem, transition1);
        dispatcher.onStateTransition(playbackItem, transition2);

        InOrder inOrder = inOrder(trackSessionDispatcher);
        inOrder.verify(trackSessionDispatcher).onPlayTransition(transition1, true);
        inOrder.verify(trackSessionDispatcher).onPlayTransition(transition2, false);
    }

    @Test
    public void onPlaystateChangedForwardsStopTransitionToDispatcher() {
        PlaybackStateTransition transition = new PlaybackStateTransition(PlaybackState.IDLE, PlayStateReason.NONE, Urn.forTrack(123L));

        dispatcher.onStateTransition(playbackItem, transition);

        verify(trackSessionDispatcher).onStopTransition(transition);
        verify(adSessionDispatcher, never()).onStopTransition(any(PlaybackStateTransition.class));
    }

    @Test
    public void onPlaystateChangedForwardsSkipTransitionToDispatcher() {
        final VideoAd videoAd = AdFixtures.getVideoAd(Urn.forTrack(123));
        PlaybackItem playbackItem2 = VideoAdPlaybackItem.create(videoAd, 0L);
        PlaybackStateTransition transition1 = new PlaybackStateTransition(PlaybackState.PLAYING, PlayStateReason.NONE, Urn.forTrack(123L));
        PlaybackStateTransition transition2 = new PlaybackStateTransition(PlaybackState.PLAYING, PlayStateReason.NONE, Urn.forAd("dfp", "321-123"));

        dispatcher.onStateTransition(playbackItem, transition1);
        dispatcher.onStateTransition(playbackItem2, transition2);

        InOrder inOrder = inOrder(trackSessionDispatcher, adSessionDispatcher);
        inOrder.verify(trackSessionDispatcher).onPlayTransition(transition1, true);
        inOrder.verify(trackSessionDispatcher).onSkipTransition(transition1);
        inOrder.verify(adSessionDispatcher).onPlayTransition(transition2, true);
    }

}
