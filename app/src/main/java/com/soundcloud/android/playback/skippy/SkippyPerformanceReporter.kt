package com.soundcloud.android.playback.skippy

import com.soundcloud.android.events.PlaybackPerformanceEvent
import com.soundcloud.android.playback.AudioPerformanceEvent
import com.soundcloud.android.playback.PerformanceReporter
import com.soundcloud.android.playback.PlaybackMetric
import com.soundcloud.android.utils.OpenForTesting
import com.soundcloud.rx.eventbus.EventBusV2

@OpenForTesting
class SkippyPerformanceReporter(eventBus: EventBusV2) : PerformanceReporter(eventBus) {
    override fun shouldReportPerformanceFor(metric: PlaybackMetric) = metric != PlaybackMetric.TIME_TO_BUFFER && metric != PlaybackMetric.UNINTERRUPTED_PLAYTIME

    fun reportTimeToLoadLibrary(event: AudioPerformanceEvent) {
        val playbackPerformanceEvent = populatePlaybackPerformanceBuilder(PlaybackPerformanceEvent.timeToLoad(), event)
        reportPerformanceEvent(playbackPerformanceEvent)
    }
}
