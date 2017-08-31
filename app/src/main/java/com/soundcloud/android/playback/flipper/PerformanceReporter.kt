package com.soundcloud.android.playback.flipper

import com.soundcloud.android.accounts.AccountOperations
import com.soundcloud.android.events.EventQueue
import com.soundcloud.android.events.PlaybackPerformanceEvent
import com.soundcloud.android.events.PlayerType
import com.soundcloud.android.playback.PlaybackItem
import com.soundcloud.android.playback.PlaybackProtocol
import com.soundcloud.android.playback.PlaybackType
import com.soundcloud.android.utils.ConnectionHelper
import com.soundcloud.android.utils.OpenForTesting
import com.soundcloud.java.optional.Optional
import com.soundcloud.rx.eventbus.EventBusV2

import javax.inject.Inject

@OpenForTesting
class PerformanceReporter
@Inject
constructor(private val eventBus: EventBusV2,
            private val accountOperations: AccountOperations,
            private val connectionHelper: ConnectionHelper) {

    fun report(playbackItem: PlaybackItem, audioPerformanceEvent: AudioPerformanceEvent, playerType: PlayerType) {
        if (allowPerformanceMeasureEvent(playbackItem)) {
            reportPerformanceEvent(createPlaybackPerformanceEvent(playbackItem, audioPerformanceEvent, playerType))
        }
    }

    private fun allowPerformanceMeasureEvent(playbackItem: PlaybackItem) = !isAd(playbackItem)

    private fun isAd(playbackItem: PlaybackItem) = playbackItem.playbackType == PlaybackType.AUDIO_AD || playbackItem.playbackType == PlaybackType.VIDEO_AD

    private fun createPlaybackPerformanceEvent(playbackItem: PlaybackItem, event: AudioPerformanceEvent, playerType: PlayerType): PlaybackPerformanceEvent {
        val eventType = event.type
        val builder: PlaybackPerformanceEvent.Builder

        builder = when (eventType) {
            "play" -> PlaybackPerformanceEvent.timeToPlay(playbackItem.playbackType)
            "seek" -> PlaybackPerformanceEvent.timeToSeek()
            "cacheUsage" -> PlaybackPerformanceEvent.cacheUsagePercent()
            "playlist" -> PlaybackPerformanceEvent.timeToPlaylist()
            else -> throw IllegalArgumentException("Unexpected performance metric : $eventType")
        }

        return builder
                .metricValue(event.latency)
                .protocol(PlaybackProtocol.fromValue(event.protocol))
                .playerType(playerType)
                .connectionType(connectionHelper.currentConnectionType)
                .cdnHost(event.cdnHost)
                .format(event.format)
                .bitrate(event.bitRate)
                .userUrn(accountOperations.loggedInUserUrn)
                .details(Optional.fromNullable(event.detailsJson))
                .build()
    }

    private fun reportPerformanceEvent(event: PlaybackPerformanceEvent) {
        eventBus.publish(EventQueue.PLAYBACK_PERFORMANCE, event)
    }
}
