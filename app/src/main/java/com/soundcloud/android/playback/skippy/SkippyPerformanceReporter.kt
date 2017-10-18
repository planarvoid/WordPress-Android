package com.soundcloud.android.playback.skippy

import com.soundcloud.android.events.PlaybackPerformanceEvent
import com.soundcloud.android.events.PlayerType
import com.soundcloud.android.playback.AudioPerformanceEvent
import com.soundcloud.android.playback.PerformanceReporter
import com.soundcloud.android.playback.PlaybackMetric
import com.soundcloud.android.playback.PlaybackType
import com.soundcloud.android.utils.OpenForTesting
import com.soundcloud.rx.eventbus.EventBusV2

@OpenForTesting
class SkippyPerformanceReporter(eventBus: EventBusV2) : PerformanceReporter(eventBus) {
    override fun shouldReportPerformanceFor(playbackType: PlaybackType, metric: PlaybackMetric): Boolean {
        if (metric == PlaybackMetric.TIME_TO_BUFFER || metric == PlaybackMetric.UNINTERRUPTED_PLAYTIME) {
            return false
        }

        return metric == PlaybackMetric.CACHE_USAGE_PERCENT
                || metric == PlaybackMetric.TIME_TO_PLAY
                || !isAd(playbackType)
    }

    fun reportTimeToLoadLibrary(event: AudioPerformanceEvent, playerType: PlayerType) {
        val playbackPerformanceEvent = populatePlaybackPerformanceBuilder(PlaybackPerformanceEvent.timeToLoad(), event, playerType)
        reportPerformanceEvent(playbackPerformanceEvent)
    }
}
