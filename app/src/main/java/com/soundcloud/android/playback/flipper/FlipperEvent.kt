package com.soundcloud.android.playback.flipper

import com.soundcloud.flippernative.api.ErrorReason
import com.soundcloud.flippernative.api.PlayerState
import com.soundcloud.flippernative.api.StreamingProtocol

data class ProgressChange(val uri: String, val position: Long, val duration: Long)

data class SeekingStatusChange(val uri: String, val seekInProgress: Boolean)

data class StateChange(val uri: String,
                       val state: PlayerState,
                       val errorReason: ErrorReason,
                       val buffering: Boolean,
                       val position: Long,
                       val duration: Long,
                       val streamingProtocol: StreamingProtocol)

data class AudioPerformanceEvent(val type: String,
                                 val latency: Long,
                                 val protocol: String,
                                 val cdnHost: String,
                                 val format: String,
                                 val bitRate: Int,
                                 val detailsJson: String?)

data class FlipperError(val category: String,
                        val sourceFile: String,
                        val line: Int,
                        val message: String,
                        val streamingProtocol: StreamingProtocol,
                        val cdn: String,
                        val format: String,
                        val bitrate: Int)
