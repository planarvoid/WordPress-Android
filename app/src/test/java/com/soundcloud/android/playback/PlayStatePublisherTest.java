package com.soundcloud.android.playback;


import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.ads.AdsController;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.testsupport.fixtures.TestPlayStates;
import com.soundcloud.android.testsupport.fixtures.TestPlayerTransitions;
import com.soundcloud.android.utils.TestDateProvider;
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

    private static final boolean IS_FIRST_PLAY = true;
    private static final String PLAY_ID = "play-id";
    private static final Urn URN = TestPlayStates.URN;

    private PlayStatePublisher publisher;
    @Mock private PlaySessionStateProvider sessionStateProvider;
    @Mock private PlaybackAnalyticsController analyticsController;
    @Mock private AdsController adsController;
    @Mock private UuidProvider uuidProvider;
    @Mock private EventBus eventBus;

    @Before
    public void setUp() throws Exception {
        publisher = new PlayStatePublisher(sessionStateProvider, uuidProvider, analyticsController, adsController, eventBus);
        when(uuidProvider.getRandomUuid()).thenReturn(PLAY_ID);
    }

    @Test
    public void publishesPlayStateForFirstPlayToControllers() {
        when(sessionStateProvider.isLastPlayed(URN)).thenReturn(false);

        final PlaybackStateTransition stateTransition = TestPlayerTransitions.buffering();
        final AudioPlaybackItem playbackItem = getPlaybackItem();

        publisher.publish(stateTransition, playbackItem, true);

        final PlayStateEvent playStateEvent = PlayStateEvent.create(stateTransition, playbackItem.getDuration(), true, PLAY_ID);
        final InOrder inOrder = Mockito.inOrder(analyticsController, adsController, eventBus);
        inOrder.verify(analyticsController).onStateTransition(playbackItem, playStateEvent);
        inOrder.verify(adsController).onPlayStateChanged(playStateEvent);
        inOrder.verify(eventBus).publish(EventQueue.PLAYBACK_STATE_CHANGED, playStateEvent);
    }

    @Test
    public void publishesPlayStateWithCorrectedDuration() {
        final TestDateProvider dateProvider = new TestDateProvider();
        final PlaybackStateTransition stateTransition = TestPlayerTransitions.playing(123, 0, dateProvider);
        final AudioPlaybackItem playbackItem = getPlaybackItem();
        final PlayStateEvent playStateEvent = PlayStateEvent.create(stateTransition, 456, IS_FIRST_PLAY, PLAY_ID);

        publisher.publish(stateTransition, playbackItem, true);

        verify(analyticsController).onStateTransition(playbackItem, playStateEvent);
        verify(adsController).onPlayStateChanged(playStateEvent);
        verify(eventBus).publish(EventQueue.PLAYBACK_STATE_CHANGED, playStateEvent);
    }

    @Test
    public void publishesPlayStateWithLastPlayIdIfNotFirstPlayEvent() {
        when(sessionStateProvider.isLastPlayed(URN)).thenReturn(true);
        when(sessionStateProvider.getCurrentPlayId()).thenReturn("current-play-id");

        final PlaybackStateTransition stateTransition = TestPlayerTransitions.buffering();
        final AudioPlaybackItem playbackItem = getPlaybackItem();

        publisher.publish(stateTransition, playbackItem, true);

        final PlayStateEvent playStateEvent = PlayStateEvent.create(stateTransition, playbackItem.getDuration(), false, "current-play-id");
        final InOrder inOrder = Mockito.inOrder(analyticsController, adsController, eventBus);
        inOrder.verify(analyticsController).onStateTransition(playbackItem, playStateEvent);
        inOrder.verify(adsController).onPlayStateChanged(playStateEvent);
        inOrder.verify(eventBus).publish(EventQueue.PLAYBACK_STATE_CHANGED, playStateEvent);
    }

    @NonNull
    private AudioPlaybackItem getPlaybackItem() {
        return AudioPlaybackItem.create(TestPlayStates.URN, 0, 456, PlaybackType.AUDIO_DEFAULT);
    }
}
