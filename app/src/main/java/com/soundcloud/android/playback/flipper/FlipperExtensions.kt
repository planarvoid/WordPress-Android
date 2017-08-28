package com.soundcloud.android.playback.flipper

import com.soundcloud.android.playback.PlayStateReason
import com.soundcloud.android.playback.PlaybackProtocol
import com.soundcloud.android.playback.PlaybackState
import com.soundcloud.flippernative.api.ErrorReason
import com.soundcloud.flippernative.api.PlayerState
import com.soundcloud.flippernative.api.StreamingProtocol

fun StateChange.playbackState(): PlaybackState = when (state) {
    PlayerState.Preparing, PlayerState.Prepared -> PlaybackState.BUFFERING
    PlayerState.Playing -> if (buffering) PlaybackState.BUFFERING else PlaybackState.PLAYING
    else -> PlaybackState.IDLE
}

fun StateChange.playStateReason(): PlayStateReason = when (state) {
    PlayerState.Error -> {
        when (errorReason) {
            ErrorReason.NotFound -> PlayStateReason.ERROR_NOT_FOUND
            ErrorReason.Forbidden -> PlayStateReason.ERROR_FORBIDDEN
            else -> PlayStateReason.ERROR_FAILED
        }
    }
    PlayerState.Completed -> PlayStateReason.PLAYBACK_COMPLETE
    else -> PlayStateReason.NONE
}

fun StreamingProtocol.playbackProtocol(): PlaybackProtocol = when (this) {
    StreamingProtocol.Hls -> PlaybackProtocol.HLS
    StreamingProtocol.EncryptedHls -> PlaybackProtocol.ENCRYPTED_HLS
    StreamingProtocol.File -> PlaybackProtocol.FILE
    else -> PlaybackProtocol.UNKNOWN
}
