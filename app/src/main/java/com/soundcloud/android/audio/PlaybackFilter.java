package com.soundcloud.android.audio;

import android.os.Parcelable;

import java.nio.ByteBuffer;

public interface PlaybackFilter extends Parcelable {
    /**
     *
     * @param buffer the audio data
     * @param position where (byte offset) in relation to the total piece of audio does this buffer belong
     * @param length what is the total length (bytes) of the audio that this buffer belongs to
     * @return
     */
    ByteBuffer apply(ByteBuffer buffer, long position, long length);
}
