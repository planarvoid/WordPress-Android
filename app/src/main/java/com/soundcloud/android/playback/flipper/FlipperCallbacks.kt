package com.soundcloud.android.playback.flipper

import com.soundcloud.android.playback.AudioPerformanceEvent

interface FlipperCallbacks {
    fun onProgressChanged(event: ProgressChange)
    fun onStateChanged(event: StateChange)
    fun onBufferingChanged(event: StateChange)
    fun onSeekingStatusChanged(seekingStatusChange: SeekingStatusChange)
    fun onError(error: FlipperError)
    fun onPerformanceEvent(event: AudioPerformanceEvent)
}
