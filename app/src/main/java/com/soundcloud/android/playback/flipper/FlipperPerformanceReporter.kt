package com.soundcloud.android.playback.flipper

import com.soundcloud.android.playback.PerformanceReporter
import com.soundcloud.android.playback.PlaybackMetric
import com.soundcloud.android.utils.OpenForTesting
import com.soundcloud.rx.eventbus.EventBusV2

@OpenForTesting
class FlipperPerformanceReporter(eventBus: EventBusV2) : PerformanceReporter(eventBus) {
    override fun shouldReportPerformanceFor(metric: PlaybackMetric) = true
}
