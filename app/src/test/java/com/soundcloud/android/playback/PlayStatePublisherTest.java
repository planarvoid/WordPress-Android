package com.soundcloud.android.playback;


import static org.mockito.Mockito.verify;

import com.soundcloud.android.ads.AdsController;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.testsupport.fixtures.TestPlayStates;
import com.soundcloud.android.utils.TestDateProvider;
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

    private PlayStatePublisher publisher;
    @Mock private PlaybackAnalyticsController analyticsController;
    @Mock private AdsController adsController;
    @Mock private EventBus eventBus;

    @Before
    public void setUp() throws Exception {
        publisher = new PlayStatePublisher(analyticsController, adsController, eventBus);
    }

    @Test
    public void publishesPlayStateToControllersInOrder() {
        final PlaybackStateTransition stateTransition = TestPlayStates.buffering();
        final AudioPlaybackItem playbackItem = getPlaybackItem();

        publisher.publish(stateTransition, playbackItem);

        final InOrder inOrder = Mockito.inOrder(analyticsController, adsController, eventBus);
        inOrder.verify(analyticsController).onStateTransition(playbackItem, stateTransition);
        inOrder.verify(adsController).onPlayStateTransition(stateTransition);
        inOrder.verify(eventBus).publish(EventQueue.PLAYBACK_STATE_CHANGED, stateTransition);
    }

    @Test
    public void publishesPlayStateWithCorrectedDuration() {
        final TestDateProvider dateProvider = new TestDateProvider();

        final AudioPlaybackItem playbackItem = getPlaybackItem();
        publisher.publish(TestPlayStates.playing(123, 0, dateProvider), playbackItem);

        PlaybackStateTransition correctedState = TestPlayStates.playing(123, 456, dateProvider);
        verify(analyticsController).onStateTransition(playbackItem, correctedState);
        verify(adsController).onPlayStateTransition(correctedState);
        verify(eventBus).publish(EventQueue.PLAYBACK_STATE_CHANGED, correctedState);
    }

    @NonNull
    private AudioPlaybackItem getPlaybackItem() {
        return AudioPlaybackItem.create(TestPlayStates.URN, 0, 456, PlaybackType.AUDIO_DEFAULT);
    }
}
