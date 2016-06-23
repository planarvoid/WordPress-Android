package com.soundcloud.android.creators.record;

import android.media.AudioManager;
import android.media.AudioTrack;

import java.nio.ByteBuffer;

public class ScAudioTrack extends AudioTrack {
    private final byte[] audioData;

    public ScAudioTrack(AudioConfig config, int bufferSize) {
        super(AudioManager.STREAM_MUSIC,
              config.sampleRate,
              config.getChannelConfig(false),
              config.getFormat(),
              bufferSize,
              AudioTrack.MODE_STREAM);

        // performance: pre-allocate buffer
        audioData = new byte[bufferSize];
    }

    public int write(ByteBuffer buffer, int length) {
        buffer.get(audioData, 0, length);
        return write(audioData, 0, length);
    }
}
