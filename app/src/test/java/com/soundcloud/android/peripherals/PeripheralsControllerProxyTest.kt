package com.soundcloud.android.peripherals

import com.nhaarman.mockito_kotlin.verify
import com.soundcloud.android.events.CurrentPlayQueueItemEvent
import com.soundcloud.android.events.CurrentUserChangedEvent
import com.soundcloud.android.events.EventQueue
import com.soundcloud.android.model.Urn
import com.soundcloud.android.testsupport.fixtures.TestPlayQueueItem
import com.soundcloud.android.testsupport.fixtures.TestPlayStates
import com.soundcloud.rx.eventbus.TestEventBusV2
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import javax.inject.Provider

@RunWith(MockitoJUnitRunner::class)
class PeripheralsControllerProxyTest {

    private val eventBus = TestEventBusV2()

    @Mock private lateinit var controller: PeripheralsController
    private lateinit var subject: PeripheralsControllerProxy

    @Before
    fun setUp() {
        subject = PeripheralsControllerProxy(eventBus, Provider { controller })
        subject.subscribe()
    }

    @Test
    fun userChange() {
        val event = CurrentUserChangedEvent.forLogout()

        eventBus.publish(EventQueue.CURRENT_USER_CHANGED, event)

        verify(controller).onCurrentUserChanged(event)
    }

    @Test
    fun playbackStateChange() {
        val event = TestPlayStates.playing()

        eventBus.publish(EventQueue.PLAYBACK_STATE_CHANGED, event)

        verify(controller).onPlayStateEvent(event)
    }

    @Test
    fun playQueueChange() {
        val event = CurrentPlayQueueItemEvent.fromNewQueue(TestPlayQueueItem.createTrack(Urn.forTrack(123L)), Urn.NOT_SET,0)

        eventBus.publish(EventQueue.CURRENT_PLAY_QUEUE_ITEM, event)

        verify(controller).onCurrentPlayQueueItem(event)
    }
}
