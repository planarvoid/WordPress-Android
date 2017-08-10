package com.soundcloud.android.playback.flipper;

import com.soundcloud.android.playback.PlaybackProtocol;
import com.soundcloud.flippernative.api.StreamingProtocol;
import static org.assertj.core.api.Java6Assertions.assertThat;
import org.junit.Test;

public class FlipperPlaybackProtocolMapperTest {

    @Test
    public void testFlipperPlaybackProtocolMapping() {
        assertThat(FlipperPlaybackProtocolMapper.map(StreamingProtocol.Hls)).isEqualTo(PlaybackProtocol.HLS);
        assertThat(FlipperPlaybackProtocolMapper.map(StreamingProtocol.EncryptedHls)).isEqualTo(PlaybackProtocol.ENCRYPTED_HLS);
        assertThat(FlipperPlaybackProtocolMapper.map(StreamingProtocol.File)).isEqualTo(PlaybackProtocol.FILE);
        assertThat(FlipperPlaybackProtocolMapper.map(StreamingProtocol.Unknown)).isEqualTo(PlaybackProtocol.UNKNOWN);
    }
}
