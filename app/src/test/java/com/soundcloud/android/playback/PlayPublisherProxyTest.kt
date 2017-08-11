package com.soundcloud.android.playback

import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.never
import com.nhaarman.mockito_kotlin.verify
import com.soundcloud.android.events.EventQueue
import com.soundcloud.android.model.Urn
import com.soundcloud.android.testsupport.fixtures.TestPlayStates
import com.soundcloud.rx.eventbus.TestEventBusV2
import dagger.Lazy
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner

@RunWith(MockitoJUnitRunner::class)
class PlayPublisherProxyTest {
    private val eventBus = TestEventBusV2()
    @Mock private lateinit var playPublisher: PlayPublisher

    private lateinit var subject: PlayPublisherProxy

    @Before
    fun setUp() {
        subject = PlayPublisherProxy(eventBus, Lazy { playPublisher })
        subject.subscribe()
    }

    @Test
    fun onPlaybackStateEvent() {
        val event = TestPlayStates.wrap(PlaybackStateTransition(PlaybackState.PLAYING,
                PlayStateReason.NONE,
                Urn.forTrack(23L), 0, 0))

        eventBus.publish(EventQueue.PLAYBACK_STATE_CHANGED, event)

        verify(playPublisher).onPlaybackStateChanged(event)
    }

    @Test
    fun bufferingEventDoesNotCausePlayPublishApiRequest() {
        eventBus.publish(EventQueue.PLAYBACK_STATE_CHANGED, TestPlayStates.wrap(PlaybackStateTransition(PlaybackState.BUFFERING,
                PlayStateReason.NONE,
                Urn.forTrack(23L), 0, 0)))

        verify(playPublisher, never()).onPlaybackStateChanged(any())
    }

    @Test
    fun idleEventDoesNotCausePlayPublishApiRequest() {
        eventBus.publish(EventQueue.PLAYBACK_STATE_CHANGED, TestPlayStates.wrap(PlaybackStateTransition(PlaybackState.IDLE,
                PlayStateReason.NONE,
                Urn.forTrack(23L), 0, 0)))

        verify(playPublisher, never()).onPlaybackStateChanged(any())
    }
}
