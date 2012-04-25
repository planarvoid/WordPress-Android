package com.soundcloud.android.audio;

import android.media.AudioManager;
import android.media.AudioTrack;

import java.nio.ByteBuffer;

public class ScAudioTrack extends AudioTrack {

    public ScAudioTrack(AudioConfig config, int bufferSize) {
        super(AudioManager.STREAM_MUSIC,
                config.sampleRate,
                config.getChannelConfig(),
                config.getFormat(),
                bufferSize,
                AudioTrack.MODE_STREAM);
    }

    public int write(ByteBuffer buffer, int length) {
        byte[] buf = new byte[length];
        buffer.get(buf, 0, length);

        return write(buf, 0, length);
    }
}
