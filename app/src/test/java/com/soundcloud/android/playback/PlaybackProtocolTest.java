package com.soundcloud.android.playback;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.Test;

public class PlaybackProtocolTest {

    @Test
    public void testPlaybackProtocolValues() {
        final PlaybackProtocol hlsProtocol = PlaybackProtocol.fromValue("hls");
        final PlaybackProtocol encryptedHlsProtocol = PlaybackProtocol.fromValue("encrypted-hls");
        final PlaybackProtocol fileProtocol = PlaybackProtocol.fromValue("file");
        final PlaybackProtocol unknownProtocol = PlaybackProtocol.fromValue("unknown");
        final PlaybackProtocol invalidProtocol = PlaybackProtocol.fromValue("invalid");

        assertThat(hlsProtocol).isEqualTo(PlaybackProtocol.HLS);
        assertThat(encryptedHlsProtocol).isEqualTo(PlaybackProtocol.ENCRYPTED_HLS);
        assertThat(fileProtocol).isEqualTo(PlaybackProtocol.FILE);
        assertThat(unknownProtocol).isEqualTo(PlaybackProtocol.UNKNOWN);
        assertThat(invalidProtocol).isEqualTo(PlaybackProtocol.UNKNOWN);
    }

}
