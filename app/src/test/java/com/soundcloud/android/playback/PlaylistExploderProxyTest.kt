package com.soundcloud.android.playback

import com.nhaarman.mockito_kotlin.verify
import com.soundcloud.android.events.CurrentPlayQueueItemEvent
import com.soundcloud.android.events.EventQueue
import com.soundcloud.android.events.PlayQueueEvent
import com.soundcloud.android.model.Urn
import com.soundcloud.android.testsupport.fixtures.TestPlayQueueItem
import com.soundcloud.rx.eventbus.TestEventBusV2
import dagger.Lazy
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner

@RunWith(MockitoJUnitRunner::class)
class PlaylistExploderProxyTest {
    private val eventBus = TestEventBusV2()
    @Mock private lateinit var exploder: PlaylistExploder

    private lateinit var subject: PlaylistExploderProxy

    @Before
    fun setUp() {
        subject = PlaylistExploderProxy(eventBus, Lazy { exploder })
        subject.subscribe()
    }

    @Test
    fun onPlayQueueItemEvent() {
        val event = CurrentPlayQueueItemEvent.fromNewQueue(TestPlayQueueItem.createTrack(Urn.forTrack(123L)), Urn.NOT_SET,0)
        eventBus.publish(EventQueue.CURRENT_PLAY_QUEUE_ITEM, event)

        verify(exploder).onCurrentPlayQueueItem(event)
    }

    @Test
    fun onPlayQueueEvent() {
        val event = PlayQueueEvent.fromQueueInsert(Urn.forTrack(123L))
        eventBus.publish(EventQueue.PLAY_QUEUE, event)

        verify(exploder).onPlayQueue(event)
    }
}
