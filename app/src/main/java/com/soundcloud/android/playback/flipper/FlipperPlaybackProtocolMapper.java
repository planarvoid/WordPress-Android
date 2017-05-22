package com.soundcloud.android.playback.flipper;

import com.soundcloud.android.playback.PlaybackProtocol;
import com.soundcloud.flippernative.api.StreamingProtocol;

final class FlipperPlaybackProtocolMapper {

    static PlaybackProtocol map(StreamingProtocol flipperProtocol) {
        if (StreamingProtocol.Hls.equals(flipperProtocol)) {
            return PlaybackProtocol.HLS;
        } else if (StreamingProtocol.EncryptedHls.equals(flipperProtocol)) {
            return PlaybackProtocol.ENCRYPTED_HLS;
        } else if (StreamingProtocol.File.equals(flipperProtocol)) {
            return PlaybackProtocol.FILE;
        }
        return PlaybackProtocol.UNKNOWN;
    }

    private FlipperPlaybackProtocolMapper() {}

}
