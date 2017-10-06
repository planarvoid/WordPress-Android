package com.soundcloud.android.playback.skippy

import com.soundcloud.android.playback.PlaybackMetric
import com.soundcloud.android.skippy.Skippy

fun Skippy.PlaybackMetric.map(): PlaybackMetric = when (this) {
    Skippy.PlaybackMetric.TIME_TO_PLAY -> PlaybackMetric.TIME_TO_PLAY
    Skippy.PlaybackMetric.TIME_TO_BUFFER -> PlaybackMetric.TIME_TO_BUFFER
    Skippy.PlaybackMetric.TIME_TO_GET_PLAYLIST -> PlaybackMetric.TIME_TO_GET_PLAYLIST
    Skippy.PlaybackMetric.TIME_TO_SEEK -> PlaybackMetric.TIME_TO_SEEK
    Skippy.PlaybackMetric.FRAGMENT_DOWNLOAD_RATE -> PlaybackMetric.FRAGMENT_DOWNLOAD_RATE
    Skippy.PlaybackMetric.TIME_TO_LOAD_LIBRARY -> PlaybackMetric.TIME_TO_LOAD_LIBRARY
    Skippy.PlaybackMetric.CACHE_USAGE_PERCENT -> PlaybackMetric.CACHE_USAGE_PERCENT
    else -> PlaybackMetric.UNKNOWN
}
