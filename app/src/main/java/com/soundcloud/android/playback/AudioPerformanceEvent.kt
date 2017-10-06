package com.soundcloud.android.playback

data class AudioPerformanceEvent(val metric: PlaybackMetric,
                                 val latency: Long,
                                 val protocol: String,
                                 val cdnHost: String,
                                 val format: String,
                                 val bitRate: Int,
                                 val detailsJson: String?)
