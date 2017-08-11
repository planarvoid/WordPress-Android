package com.soundcloud.android.playback

import com.nhaarman.mockito_kotlin.verify
import com.soundcloud.android.events.CurrentPlayQueueItemEvent
import com.soundcloud.android.events.EventQueue
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
class PlaySessionControllerProxyTest {
    private val eventBus = TestEventBusV2()
    @Mock private lateinit var controller: PlaySessionController

    private lateinit var subject: PlaySessionControllerProxy

    @Before
    fun setUp() {
        subject = PlaySessionControllerProxy(eventBus, Lazy { controller })
        subject.subscribe()
    }

    @Test
    fun onPlayQueueItemEvent() {
        val event = CurrentPlayQueueItemEvent.fromNewQueue(TestPlayQueueItem.createTrack(Urn.forTrack(123L)), Urn.NOT_SET,0)
        eventBus.publish(EventQueue.CURRENT_PLAY_QUEUE_ITEM, event)

        verify(controller).onPlayQueueItemEvent(event)
    }
}
