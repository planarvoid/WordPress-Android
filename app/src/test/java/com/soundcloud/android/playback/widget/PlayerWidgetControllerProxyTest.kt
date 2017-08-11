package com.soundcloud.android.playback.widget

import com.nhaarman.mockito_kotlin.verify
import com.soundcloud.android.api.model.ApiTrack
import com.soundcloud.android.events.*
import com.soundcloud.android.model.Urn
import com.soundcloud.android.testsupport.fixtures.ModelFixtures
import com.soundcloud.android.testsupport.fixtures.TestPlayQueueItem
import com.soundcloud.android.testsupport.fixtures.TestPlayStates
import com.soundcloud.android.tracks.Track
import com.soundcloud.rx.eventbus.TestEventBusV2
import dagger.Lazy
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner

@RunWith(MockitoJUnitRunner::class)
class PlayerWidgetControllerProxyTest {
    private val eventBus = TestEventBusV2()
    @Mock private lateinit var controller: PlayerWidgetController

    private lateinit var subject: PlayerWidgetControllerProxy

    @Before
    fun setUp() {
        subject = PlayerWidgetControllerProxy(eventBus, Lazy { controller })
        subject.subscribe()
    }

    @Test
    fun onTrackChange() {
        val apiTrack = ModelFixtures.create(ApiTrack::class.java)
        val event = TrackChangedEvent.forUpdate(Track.from(apiTrack))
        eventBus.publish(EventQueue.TRACK_CHANGED, event)
        verify(controller).onTrackMetadataChange(event)
    }

    @Test
    fun onLikeChanged() {
        val event = LikesStatusEvent.create(Urn.forTrack(123L), true, 1);
        eventBus.publish(EventQueue.LIKE_CHANGED, event)
        verify(controller).onTrackLikeChange(event)
    }

    @Test
    fun onRepostChanged() {
        val event = RepostsStatusEvent.createReposted(Urn.forTrack(123L))
        eventBus.publish(EventQueue.REPOST_CHANGED, event)
        verify(controller).onTrackRepostChange(event)
    }

    @Test
    fun onUserChanged() {
        val event = CurrentUserChangedEvent.forLogout()
        eventBus.publish(EventQueue.CURRENT_USER_CHANGED, event)
        verify(controller).onCurrentUserChanged(event)
    }

    @Test
    fun onPlaybackStateChanged() {
        val event = TestPlayStates.complete()
        eventBus.publish(EventQueue.PLAYBACK_STATE_CHANGED, event)
        verify(controller).onPlaybackStateUpdate(event)
    }

    @Test
    fun onPlayQueueItem() {
        val event = CurrentPlayQueueItemEvent.fromNewQueue(TestPlayQueueItem.createTrack(Urn.forTrack(123L)), Urn.NOT_SET,0)
        eventBus.publish(EventQueue.CURRENT_PLAY_QUEUE_ITEM, event)
        verify(controller).onCurrentItemChange(event)
    }
}
