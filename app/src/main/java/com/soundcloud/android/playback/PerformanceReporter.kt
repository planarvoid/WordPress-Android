package com.soundcloud.android.playback

import com.soundcloud.android.events.EventQueue
import com.soundcloud.android.events.PlaybackPerformanceEvent
import com.soundcloud.android.events.PlayerType
import com.soundcloud.android.utils.OpenForTesting
import com.soundcloud.java.optional.Optional
import com.soundcloud.rx.eventbus.EventBusV2

@OpenForTesting
abstract class PerformanceReporter
constructor(private val eventBus: EventBusV2) {

    fun report(playbackType: PlaybackType, audioPerformanceEvent: AudioPerformanceEvent, playerType: PlayerType) {
        if (shouldReportPerformanceFor(playbackType, audioPerformanceEvent.metric)) {
            reportPerformanceEvent(createPlaybackPerformanceEvent(audioPerformanceEvent, playerType))
        }
    }

    abstract fun shouldReportPerformanceFor(playbackType: PlaybackType, metric: PlaybackMetric): Boolean

    protected fun isAd(playbackType: PlaybackType) = playbackType == PlaybackType.AUDIO_AD || playbackType == PlaybackType.VIDEO_AD

    private fun createPlaybackPerformanceEvent(event: AudioPerformanceEvent, playerType: PlayerType): PlaybackPerformanceEvent {
        val builder: PlaybackPerformanceEvent.Builder = when (event.metric) {
            PlaybackMetric.TIME_TO_PLAY -> PlaybackPerformanceEvent.timeToPlay()
            PlaybackMetric.TIME_TO_SEEK -> PlaybackPerformanceEvent.timeToSeek()
            PlaybackMetric.CACHE_USAGE_PERCENT -> PlaybackPerformanceEvent.cacheUsagePercent()
            PlaybackMetric.TIME_TO_GET_PLAYLIST -> PlaybackPerformanceEvent.timeToPlaylist()
            PlaybackMetric.TIME_TO_BUFFER -> PlaybackPerformanceEvent.timeToBuffer()
            PlaybackMetric.FRAGMENT_DOWNLOAD_RATE -> PlaybackPerformanceEvent.fragmentDownloadRate()
            else -> throw IllegalArgumentException("Unexpected performance metric : $event.metric for player $playerType")
        }

        return populatePlaybackPerformanceBuilder(builder, event, playerType)
    }

    protected fun populatePlaybackPerformanceBuilder(builder: PlaybackPerformanceEvent.Builder,
                                                     event: AudioPerformanceEvent,
                                                     playerType: PlayerType): PlaybackPerformanceEvent {
        return builder
                .metricValue(event.latency)
                .playbackProtocol(event.protocol)
                .playerType(playerType.value)
                .cdnHost(event.cdnHost)
                .format(event.format)
                .bitrate(event.bitRate)
                .details(Optional.fromNullable(event.detailsJson))
                .build()
    }

    protected fun reportPerformanceEvent(event: PlaybackPerformanceEvent) {
        eventBus.publish(EventQueue.PLAYBACK_PERFORMANCE, event)
    }
}
