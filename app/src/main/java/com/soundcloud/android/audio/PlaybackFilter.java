package com.soundcloud.android.audio;

import android.os.Parcelable;

import java.nio.ByteBuffer;

public interface PlaybackFilter extends Parcelable {
    ByteBuffer apply(ByteBuffer buffer, long position, long length);
}
