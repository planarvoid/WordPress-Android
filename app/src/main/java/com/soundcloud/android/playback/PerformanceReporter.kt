package com.soundcloud.android.playback

import com.soundcloud.android.events.ConnectionType
import com.soundcloud.android.events.EventQueue
import com.soundcloud.android.events.PlaybackPerformanceEvent
import com.soundcloud.android.events.PlayerType
import com.soundcloud.android.model.Urn
import com.soundcloud.android.utils.OpenForTesting
import com.soundcloud.java.optional.Optional
import com.soundcloud.rx.eventbus.EventBusV2

@OpenForTesting
abstract class PerformanceReporter
constructor(private val eventBus: EventBusV2) {

    fun report(playbackType: PlaybackType,
               audioPerformanceEvent: AudioPerformanceEvent,
               playerType: PlayerType,
               userUrn: Urn,
               connectionType: ConnectionType) {
        if (shouldReportPerformanceFor(playbackType, audioPerformanceEvent.metric)) {
            reportPerformanceEvent(createPlaybackPerformanceEvent(playbackType, audioPerformanceEvent, playerType, userUrn, connectionType))
        }
    }

    abstract fun shouldReportPerformanceFor(playbackType: PlaybackType, metric: PlaybackMetric): Boolean

    protected fun isAd(playbackType: PlaybackType) = playbackType == PlaybackType.AUDIO_AD || playbackType == PlaybackType.VIDEO_AD

    private fun createPlaybackPerformanceEvent(playbackType: PlaybackType,
                                               event: AudioPerformanceEvent,
                                               playerType: PlayerType,
                                               userUrn: Urn,
                                               connectionType: ConnectionType): PlaybackPerformanceEvent {
        val builder: PlaybackPerformanceEvent.Builder = when (event.metric) {
            PlaybackMetric.TIME_TO_PLAY -> PlaybackPerformanceEvent.timeToPlay(playbackType)
            PlaybackMetric.TIME_TO_SEEK -> PlaybackPerformanceEvent.timeToSeek()
            PlaybackMetric.CACHE_USAGE_PERCENT -> PlaybackPerformanceEvent.cacheUsagePercent()
            PlaybackMetric.TIME_TO_GET_PLAYLIST -> PlaybackPerformanceEvent.timeToPlaylist()
            PlaybackMetric.TIME_TO_BUFFER -> PlaybackPerformanceEvent.timeToBuffer()
            PlaybackMetric.FRAGMENT_DOWNLOAD_RATE -> PlaybackPerformanceEvent.fragmentDownloadRate()
            else -> throw IllegalArgumentException("Unexpected performance metric : $event.metric for player $playerType")
        }

        return populatePlaybackPerformanceBuilder(builder, event, playerType, userUrn, connectionType)
    }

    protected fun populatePlaybackPerformanceBuilder(builder: PlaybackPerformanceEvent.Builder,
                                                     event: AudioPerformanceEvent,
                                                     playerType: PlayerType,
                                                     userUrn: Urn,
                                                     connectionType: ConnectionType): PlaybackPerformanceEvent {
        return builder
                .metricValue(event.latency)
                .protocol(PlaybackProtocol.fromValue(event.protocol))
                .playerType(playerType)
                .connectionType(connectionType)
                .cdnHost(event.cdnHost)
                .format(event.format)
                .bitrate(event.bitRate)
                .userUrn(userUrn)
                .details(Optional.fromNullable(event.detailsJson))
                .build()
    }

    protected fun reportPerformanceEvent(event: PlaybackPerformanceEvent) {
        eventBus.publish(EventQueue.PLAYBACK_PERFORMANCE, event)
    }
}
