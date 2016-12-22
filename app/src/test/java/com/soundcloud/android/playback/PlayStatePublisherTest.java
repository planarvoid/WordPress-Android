package com.soundcloud.android.playback;


import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

import com.soundcloud.android.ads.AdsController;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.testsupport.fixtures.TestPlayStates;
import com.soundcloud.android.testsupport.fixtures.TestPlayerTransitions;
import com.soundcloud.android.utils.UuidProvider;
import com.soundcloud.rx.eventbus.EventBus;
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
    @Mock private PlaySessionStateProvider sessionStateProvider;
    @Mock private PlaybackAnalyticsController analyticsController;
    @Mock private PlayQueueAdvancer playQueueAdvancer;
    @Mock private AdsController adsController;
    @Mock private UuidProvider uuidProvider;
    @Mock private EventBus eventBus;

    @Before
    public void setUp() throws Exception {
        publisher = new PlayStatePublisher(sessionStateProvider, analyticsController,
                                           playQueueAdvancer,
                                           adsController, eventBus);
        when(playQueueAdvancer.onPlayStateChanged(any(PlayStateEvent.class))).thenReturn(PlayQueueAdvancer.Result.NO_OP);
    }

    @Test
    public void publishesPlayStateForFirstPlayToControllers() {
        final PlaybackStateTransition stateTransition = TestPlayerTransitions.buffering();
        final AudioPlaybackItem playbackItem = getPlaybackItem();
        final PlayStateEvent playStateEvent = PlayStateEvent.create(stateTransition, playbackItem.getDuration(), true, PLAY_ID);
        when(sessionStateProvider.onPlayStateTransition(stateTransition, DURATION)).thenReturn(playStateEvent);

        publisher.publish(stateTransition, playbackItem, true);

        final InOrder inOrder = Mockito.inOrder(analyticsController, adsController, playQueueAdvancer, eventBus);
        inOrder.verify(analyticsController).onStateTransition(playbackItem, playStateEvent);
        inOrder.verify(adsController).onPlayStateChanged(playStateEvent);
        inOrder.verify(playQueueAdvancer).onPlayStateChanged(playStateEvent);
        inOrder.verify(eventBus).publish(EventQueue.PLAYBACK_STATE_CHANGED, playStateEvent);
    }

    @Test
    public void publishesPlayQueueCompleteState() {
        final PlaybackStateTransition stateTransition = TestPlayerTransitions.buffering();
        final AudioPlaybackItem playbackItem = getPlaybackItem();

        final PlayStateEvent playStateEvent = PlayStateEvent.create(stateTransition, playbackItem.getDuration(), false, PLAY_ID);
        when(sessionStateProvider.onPlayStateTransition(stateTransition, DURATION)).thenReturn(playStateEvent);
        when(playQueueAdvancer.onPlayStateChanged(playStateEvent)).thenReturn(PlayQueueAdvancer.Result.QUEUE_COMPLETE);

        publisher.publish(stateTransition, playbackItem, true);

        final InOrder inOrder = Mockito.inOrder(analyticsController, adsController, playQueueAdvancer, eventBus);
        inOrder.verify(analyticsController).onStateTransition(playbackItem, playStateEvent);
        inOrder.verify(adsController).onPlayStateChanged(playStateEvent);
        inOrder.verify(playQueueAdvancer).onPlayStateChanged(playStateEvent);
        inOrder.verify(eventBus).publish(EventQueue.PLAYBACK_STATE_CHANGED, PlayStateEvent.createPlayQueueCompleteEvent(playStateEvent));
    }

    @Test
    public void publishesPlayStateWithLastPlayIdIfNotFirstPlayEvent() {
        final PlaybackStateTransition stateTransition = TestPlayerTransitions.buffering();
        final AudioPlaybackItem playbackItem = getPlaybackItem();
        final PlayStateEvent playStateEvent = PlayStateEvent.create(stateTransition, playbackItem.getDuration(), false, "current-play-id");
        when(sessionStateProvider.onPlayStateTransition(stateTransition, DURATION)).thenReturn(playStateEvent);

        publisher.publish(stateTransition, playbackItem, true);


        final InOrder inOrder = Mockito.inOrder(analyticsController, adsController, eventBus);
        inOrder.verify(analyticsController).onStateTransition(playbackItem, playStateEvent);
        inOrder.verify(adsController).onPlayStateChanged(playStateEvent);
        inOrder.verify(eventBus).publish(EventQueue.PLAYBACK_STATE_CHANGED, playStateEvent);
    }

    @NonNull
    private AudioPlaybackItem getPlaybackItem() {
        return AudioPlaybackItem.create(TestPlayStates.URN, 0, DURATION, PlaybackType.AUDIO_DEFAULT);
    }
}
