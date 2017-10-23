package com.soundcloud.android.playback

enum class PlaybackMetric(val key: String) {
    TIME_TO_PLAY("play"),
    TIME_TO_GET_PLAYLIST("playlist"), // never with ads
    TIME_TO_BUFFER("buffer"), // Skippy only (and ignored) + never with ads
    TIME_TO_SEEK("seek"), // never with ads
    FRAGMENT_DOWNLOAD_RATE("fragmentDownloadRate"), // Skippy only
    TIME_TO_LOAD_LIBRARY("loadLibrary"), // Skippy only
    CACHE_USAGE_PERCENT("cacheUsage"),
    UNINTERRUPTED_PLAYTIME("uninterruptedPlaytime"), // Skippy only (and ignored)
    UNKNOWN("unknownMetric");

    companion object {
        fun from(key: String) = values().firstOrNull { it.key == key } ?: UNKNOWN
    }
}
