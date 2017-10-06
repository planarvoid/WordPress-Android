package com.soundcloud.android.playback.flipper

import com.soundcloud.android.playback.AudioPerformanceEvent
import com.soundcloud.android.playback.PlaybackMetric
import com.soundcloud.android.utils.OpenForTesting
import com.soundcloud.flippernative.api.PlayerListener
import com.soundcloud.flippernative.api.audio_performance
import com.soundcloud.flippernative.api.error_message
import com.soundcloud.flippernative.api.state_change

@OpenForTesting
internal class FlipperWrapper
constructor(private val flipperCallbacks: FlipperCallbacks, flipperFactory: FlipperFactory)
    : PlayerListener() {

    private val flipper = flipperFactory.create(this)

    fun prefetch(mediaUri: String) = flipper.prefetch(mediaUri)
    fun play() = flipper.play()
    fun pause() = flipper.pause()
    fun seek(positionMs: Long) = flipper.seek(positionMs)
    fun destroy() = flipper.destroy()
    fun open(mediaUri: String, positionMs: Long) = flipper.open(mediaUri, positionMs)
    fun openEncrypted(mediaUri: String, key: ByteArray, initVector: ByteArray, positionMs: Long) = flipper.openEncrypted(mediaUri, key, initVector, positionMs)

    var volume: Double
        get() = flipper.volume
        set(level) {
            flipper.volume = level
        }

    override fun onProgressChanged(event: state_change) {
        flipperCallbacks.onProgressChanged(ProgressChange(event.uri, event.position, event.duration))
    }

    override fun onPerformanceEvent(event: audio_performance) {
        flipperCallbacks.onPerformanceEvent(AudioPerformanceEvent(PlaybackMetric.from(event.type.const_get_value()), event.latency.const_get_value(), event.protocol.const_get_value(),
                                                                  event.host.const_get_value(), event.format.const_get_value(), event.bitrate.const_get_value().toInt(),
                                                                  event.details._value.toJson()))
    }

    override fun onStateChanged(event: state_change) {
        flipperCallbacks.onStateChanged(mapToStateChange(event))
    }

    override fun onBufferingChanged(event: state_change) {
        flipperCallbacks.onBufferingChanged(mapToStateChange(event))
    }

    private fun mapToStateChange(event: state_change): StateChange {
        return StateChange(event.uri, event.state,
                           event.reason, event.buffering,
                           event.position, event.duration, event.streamingProtocol)
    }

    override fun onDurationChanged(event: state_change) {
        // FIXME DO NOT CALL SUPER AS IT WILL CRASH THE APP WHILE SEEKING
        // FIXME Check JIRA: PLAYBACK-2706
    }

    override fun onSeekingStatusChanged(stateChangeEvent: state_change) {
        flipperCallbacks.onSeekingStatusChanged(SeekingStatusChange(stateChangeEvent.uri, stateChangeEvent.seekingInProgress))
    }

    override fun onError(error: error_message) {
        flipperCallbacks.onError(FlipperError(error.category, error.sourceFile, error.line,
                                              error.errorMessage, error.streamingProtocol,
                                              error.cdn, error.format, error.bitRate))
    }
}
