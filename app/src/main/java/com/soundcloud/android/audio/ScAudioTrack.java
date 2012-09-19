package com.soundcloud.android.audio;

import android.media.AudioManager;
import android.media.AudioTrack;

import java.nio.ByteBuffer;

public class ScAudioTrack extends AudioTrack {
    private final byte[] mAudioData;

    public ScAudioTrack(AudioConfig config, int bufferSize) {
        super(AudioManager.STREAM_MUSIC,
                config.sampleRate,
                config.getChannelConfig(false),
                config.getFormat(),
                bufferSize,
                AudioTrack.MODE_STREAM);

        // performance: pre-allocate buffer
        mAudioData = new byte[bufferSize];
    }

    public int write(ByteBuffer buffer, int length) {
        buffer.get(mAudioData, 0, length);
        return write(mAudioData, 0, length);
    }
}
