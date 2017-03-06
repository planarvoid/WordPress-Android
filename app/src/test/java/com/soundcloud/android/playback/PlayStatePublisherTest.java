package com.soundcloud.android.playback;


import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.ads.AdsController;
import com.soundcloud.android.cast.CastConnectionHelper;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.testsupport.fixtures.TestPlayStates;
import com.soundcloud.android.testsupport.fixtures.TestPlaybackItem;
import com.soundcloud.android.testsupport.fixtures.TestPlayerTransitions;
import com.soundcloud.android.utils.UuidProvider;
import com.soundcloud.rx.eventbus.TestEventBus;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

import android.support.annotation.NonNull;

@RunWith(MockitoJUnitRunner.class)
public class PlayStatePublisherTest {

    private static final String PLAY_ID = "play-id";
    private static final int DURATION = 456;

    private PlayStatePublisher publisher;
    private TestEventBus eventBus = new TestEventBus();

    @Mock private PlaySessionStateProvider sessionStateProvider;
    @Mock private PlaybackAnalyticsController analyticsController;
    @Mock private PlayQueueAdvancer playQueueAdvancer;
    @Mock private AdsController adsController;
    @Mock private UuidProvider uuidProvider;
    @Mock private CastConnectionHelper castConnectionHelper;

    @Before
    public void setUp() throws Exception {
        when(playQueueAdvancer.onPlayStateChanged(any(PlayStateEvent.class))).thenReturn(PlayQueueAdvancer.Result.NO_OP);
        when(castConnectionHelper.isCasting()).thenReturn(false);

        publisher = new PlayStatePublisher(sessionStateProvider,
                                           analyticsController,
                                           playQueueAdvancer,
                                           adsController,
                                           eventBus,
                                           castConnectionHelper);
    }

    @Test
    public void publishesPlayStateForFirstPlayToControllers() {
        final PlaybackStateTransition stateTransition = TestPlayerTransitions.buffering();
        final AudioPlaybackItem playbackItem = TestPlaybackItem.audio();
        final PlayStateEvent playStateEvent = PlayStateEvent.create(stateTransition, playbackItem.getDuration(), true, PLAY_ID);
        when(sessionStateProvider.onPlayStateTransition(stateTransition, DURATION)).thenReturn(playStateEvent);

        publisher.publish(stateTransition, playbackItem);

        final InOrder inOrder = Mockito.inOrder(analyticsController, adsController, playQueueAdvancer);
        inOrder.verify(analyticsController).onStateTransition(playbackItem, playStateEvent);
        inOrder.verify(adsController).onPlayStateChanged(playStateEvent);
        inOrder.verify(playQueueAdvancer).onPlayStateChanged(playStateEvent);
        assertThat(eventBus.lastEventOn(EventQueue.PLAYBACK_STATE_CHANGED)).isEqualTo(playStateEvent);
    }

    @Test
    public void publishesPlayQueueCompleteState() {
        final PlaybackStateTransition stateTransition = TestPlayerTransitions.buffering();
        final AudioPlaybackItem playbackItem = TestPlaybackItem.audio();

        final PlayStateEvent playStateEvent = PlayStateEvent.create(stateTransition, playbackItem.getDuration(), false, PLAY_ID);
        when(sessionStateProvider.onPlayStateTransition(stateTransition, DURATION)).thenReturn(playStateEvent);
        when(playQueueAdvancer.onPlayStateChanged(playStateEvent)).thenReturn(PlayQueueAdvancer.Result.QUEUE_COMPLETE);

        publisher.publish(stateTransition, playbackItem);

        final InOrder inOrder = Mockito.inOrder(analyticsController, adsController, playQueueAdvancer);
        inOrder.verify(analyticsController).onStateTransition(playbackItem, playStateEvent);
        inOrder.verify(adsController).onPlayStateChanged(playStateEvent);
        inOrder.verify(playQueueAdvancer).onPlayStateChanged(playStateEvent);
        assertThat(eventBus.lastEventOn(EventQueue.PLAYBACK_STATE_CHANGED)).isEqualTo(PlayStateEvent.createPlayQueueCompleteEvent(playStateEvent));
    }

    @Test
    public void publishesPlayStateWithLastPlayIdIfNotFirstPlayEvent() {
        final PlaybackStateTransition stateTransition = TestPlayerTransitions.buffering();
        final AudioPlaybackItem playbackItem = TestPlaybackItem.audio();
        final PlayStateEvent playStateEvent = PlayStateEvent.create(stateTransition, playbackItem.getDuration(), false, "current-play-id");
        when(sessionStateProvider.onPlayStateTransition(stateTransition, DURATION)).thenReturn(playStateEvent);

        publisher.publish(stateTransition, playbackItem);


        final InOrder inOrder = Mockito.inOrder(analyticsController, adsController);
        inOrder.verify(analyticsController).onStateTransition(playbackItem, playStateEvent);
        inOrder.verify(adsController).onPlayStateChanged(playStateEvent);
        assertThat(eventBus.lastEventOn(EventQueue.PLAYBACK_STATE_CHANGED)).isEqualTo(playStateEvent);
    }

    @Test
    public void ignorePublishingWhileCasting() {
        when(castConnectionHelper.isCasting()).thenReturn(true);

        publisher.publish(TestPlayerTransitions.playing(), TestPlaybackItem.audio());

        eventBus.verifyNoEventsOn(EventQueue.PLAYBACK_STATE_CHANGED);
    }

    @Test
    public void transitionAdsAndAnalyticsEvenWhileCasting() {
        AudioPlaybackItem playbackItem = TestPlaybackItem.audio();
        PlaybackStateTransition stateTransition = TestPlayerTransitions.playing();
        when(castConnectionHelper.isCasting()).thenReturn(true);
        final PlayStateEvent playStateEvent = PlayStateEvent.create(stateTransition, playbackItem.getDuration(), true, PLAY_ID);
        when(sessionStateProvider.onPlayStateTransition(stateTransition, DURATION)).thenReturn(playStateEvent);

        publisher.publish(stateTransition, playbackItem);

        verify(analyticsController).onStateTransition(playbackItem, playStateEvent);
        verify(adsController).onPlayStateChanged(playStateEvent);
    }
}
