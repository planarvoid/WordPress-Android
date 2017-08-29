package com.soundcloud.android.ads

import android.app.Activity
import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.times
import com.nhaarman.mockito_kotlin.verify
import com.soundcloud.android.events.*
import com.soundcloud.android.model.Urn
import com.soundcloud.android.testsupport.AndroidUnitTest
import com.soundcloud.android.testsupport.fixtures.TestPlayQueueItem
import com.soundcloud.android.testsupport.fixtures.TestPlayStates
import com.soundcloud.rx.eventbus.TestEventBusV2
import dagger.Lazy
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito

class PlayerAdsControllerProxyTest : AndroidUnitTest() {
    private val eventBus = TestEventBusV2()
    @Mock private lateinit var controller: PlayerAdsController

    private lateinit var subject: PlayerAdsControllerProxy

    @Before
    fun setUp() {
        subject = PlayerAdsControllerProxy(eventBus, Lazy { controller })
        subject.subscribe()
    }

    @Test
    fun onActivityLifeCycle() {
        val event = ActivityLifeCycleEvent.forOnCreate(Mockito.mock(Activity::class.java))
        eventBus.publish(EventQueue.ACTIVITY_LIFE_CYCLE, event)
        verify(controller).onActivityLifeCycleEvent(event)
    }

    @Test
    fun onPlayerUi() {
        val event = PlayerUIEvent.fromPlayerExpanded()
        eventBus.publish(EventQueue.PLAYER_UI, event)
        verify(controller).onPlayerState(event)
    }

    @Test
    fun onCurrentPlayQueueItem() {
        val event = CurrentPlayQueueItemEvent.fromNewQueue(TestPlayQueueItem.createTrack(Urn.forTrack(123L)), Urn.NOT_SET,0)
        eventBus.publish(EventQueue.CURRENT_PLAY_QUEUE_ITEM, event)
        verify(controller).onCurrentPlayQueueItem(event)
    }

    @Test
    fun onPlaybackStateChanged() {
        val event = TestPlayStates.playing()
        eventBus.publish(EventQueue.PLAYBACK_STATE_CHANGED, event)
        verify(controller).onPlaybackStateChanged(event)
    }

    @Test
    fun onPLayerUiChanged() {
        val playerUiEvent = PlayerUIEvent.fromPlayerExpanded()
        eventBus.publish(EventQueue.PLAYER_UI, playerUiEvent)
        verify(controller).onPlayerState(playerUiEvent)
    }

    @Test
    fun onQueueChangeForAd() {
        val currentPlayQueueEvent = CurrentPlayQueueItemEvent.fromNewQueue(TestPlayQueueItem.createTrack(Urn.forTrack(123L)), Urn.NOT_SET,0)
        val playQueueEvent = PlayQueueEvent.fromAdsRemoved(Urn.forTrack(123L))
        val queueUpdateEvent = PlayQueueEvent.fromQueueUpdate(Urn.forTrack(222L))
        eventBus.publish(EventQueue.CURRENT_PLAY_QUEUE_ITEM, currentPlayQueueEvent)
        eventBus.publish(EventQueue.PLAY_QUEUE, playQueueEvent) // only updates should trigger the update so this one shouldn't have any effect
        eventBus.publish(EventQueue.PLAY_QUEUE, queueUpdateEvent)

        verify(controller, times(2)).onQueueChangeForAd()
    }

    @Test
    fun adOverlayImpression() {
        val lifecycleEvent = ActivityLifeCycleEvent.forOnCreate(Mockito.mock(Activity::class.java))
        val playerUiEvent = PlayerUIEvent.fromPlayerExpanded()

        val leaveBehind = AdFixtures.getLeaveBehindAd(Urn.forTrack(123L))
        val adOverlayEvent = AdOverlayEvent.shown(Urn.forTrack(123), leaveBehind, null)

        eventBus.publish(EventQueue.ACTIVITY_LIFE_CYCLE, lifecycleEvent)
        eventBus.publish(EventQueue.PLAYER_UI, playerUiEvent)
        eventBus.publish(EventQueue.AD_OVERLAY, adOverlayEvent)

        verify(controller).onAdOverlayEvent(any())
    }

    @Test
    fun visualAdImpression() {
        val lifecycleEvent = ActivityLifeCycleEvent.forOnCreate(Mockito.mock(Activity::class.java))
        val playerUiEvent = PlayerUIEvent.fromPlayerExpanded()
        val currentPlayQueueEvent = CurrentPlayQueueItemEvent.fromNewQueue(TestPlayQueueItem.createAudioAd(AdFixtures.getAudioAd(Urn.forTrack(123L))), Urn.NOT_SET,0)

        val leaveBehind = AdFixtures.getLeaveBehindAd(Urn.forTrack(123L))
        val adOverlayEvent = AdOverlayEvent.shown(Urn.forTrack(123), leaveBehind, null)

        eventBus.publish(EventQueue.ACTIVITY_LIFE_CYCLE, lifecycleEvent)
        eventBus.publish(EventQueue.PLAYER_UI, playerUiEvent)
        eventBus.publish(EventQueue.CURRENT_PLAY_QUEUE_ITEM, currentPlayQueueEvent)

        verify(controller).onVisualAdImpressionState(any())
    }
}
