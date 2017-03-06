package com.soundcloud.android.cast;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.playback.PlaySessionStateProvider;
import com.soundcloud.android.playback.PlayStateEvent;
import com.soundcloud.android.playback.PlayStateReason;
import com.soundcloud.android.playback.PlaybackStateTransition;
import com.soundcloud.android.testsupport.fixtures.TestPlaybackItem;
import com.soundcloud.android.testsupport.fixtures.TestPlayerTransitions;
import com.soundcloud.rx.eventbus.TestEventBus;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class CastPlayStatePublisherTest {

    @Mock private PlaySessionStateProvider sessionStateProvider;
    @Mock private CastConnectionHelper castConnectionHelper;

    private CastPlayStatePublisher publisher;
    private TestEventBus eventBus = new TestEventBus();

    @Before
    public void setUp() throws Exception {
        when(castConnectionHelper.isCasting()).thenReturn(true);

        publisher = new CastPlayStatePublisher(sessionStateProvider, eventBus, castConnectionHelper);
    }

    @Test
    public void ignorePublishingWhileNotCasting() {
        when(castConnectionHelper.isCasting()).thenReturn(false);

        publisher.publish(TestPlayerTransitions.playing(), TestPlaybackItem.audio());

        eventBus.verifyNoEventsOn(EventQueue.PLAYBACK_STATE_CHANGED);
    }

    @Test
    public void publishesPlayStateIntoEventBus() {
        PlaybackStateTransition stateTransition = TestPlayerTransitions.playing();
        final PlayStateEvent playStateEvent = PlayStateEvent.DEFAULT;
        when(sessionStateProvider.onPlayStateTransition(stateTransition, TestPlaybackItem.DURATION)).thenReturn(playStateEvent);

        publisher.publish(stateTransition, TestPlaybackItem.audio());

        assertThat(eventBus.lastEventOn(EventQueue.PLAYBACK_STATE_CHANGED)).isEqualTo(playStateEvent);
    }

    @Test
    public void publishesCastDisconnectionEvenThoughConnectionHelperSaysItIsNotCastingAnymore() {
        PlaybackStateTransition stateTransition = TestPlayerTransitions.idle(PlayStateReason.CAST_DISCONNECTED);
        final PlayStateEvent playStateEvent = PlayStateEvent.DEFAULT;
        when(sessionStateProvider.onPlayStateTransition(stateTransition, TestPlaybackItem.DURATION)).thenReturn(playStateEvent);

        publisher.publish(stateTransition, TestPlaybackItem.audio());

        assertThat(eventBus.lastEventOn(EventQueue.PLAYBACK_STATE_CHANGED)).isEqualTo(playStateEvent);
    }
}
