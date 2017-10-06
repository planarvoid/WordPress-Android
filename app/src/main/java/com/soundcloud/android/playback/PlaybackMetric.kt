package com.soundcloud.android.playback

enum class PlaybackMetric(val key: String) {
    TIME_TO_PLAY("play"),
    TIME_TO_GET_PLAYLIST("playlist"),
    TIME_TO_BUFFER("buffer"), // Skippy only
    TIME_TO_SEEK("seek"),
    FRAGMENT_DOWNLOAD_RATE("fragmentDownloadRate"), // Skippy only
    TIME_TO_LOAD_LIBRARY("loadLibrary"), // Skippy only
    CACHE_USAGE_PERCENT("cacheUsage"),
    UNINTERRUPTED_PLAYTIME("uninterruptedPlaytime"), // Skippy only
    UNKNOWN("unknownMetric");

    companion object {
        fun from(key: String) = values().firstOrNull { it.key == key } ?: UNKNOWN
    }
}
