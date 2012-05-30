package com.soundcloud.android.audio;

import android.util.Log;

import java.nio.ByteBuffer;

public class FadeFilter implements PlaybackFilter {

    private static final int FADE_LENGTH_MS = 3000;
    private static final int FADE_EXP_CURVE = 2;
    private final long fadeSize;

    public FadeFilter(AudioConfig config) {
        fadeSize = config.msToByte(FADE_LENGTH_MS);
    }

    @Override
    public ByteBuffer apply(ByteBuffer buffer, long position, long length) {
        final int remaining = buffer.remaining();
        if (position < fadeSize) {
            final int count = (int) Math.min(remaining, fadeSize - position);
            applyVolumeChangeToBuffer(buffer, position, 0, count, 0, false);
        }

        final long fadeOutIdx = length - fadeSize;
        if (position + buffer.remaining() > fadeOutIdx) {
            int start = (int) (position >= fadeOutIdx ? 0 : fadeOutIdx - position);
            applyVolumeChangeToBuffer(buffer, position, start, remaining - start, fadeOutIdx, true);

        }
        return buffer;
    }

    private void applyVolumeChangeToBuffer(ByteBuffer buffer, long position, int start, int count, long offset, boolean invert) {


        for (int i = Math.max(0, start - (start % 2)); i < count; i += 2) {
            final double v = Math.pow((position + i - offset) / ((double) fadeSize), FADE_EXP_CURVE);
            final short orig = buffer.getShort(i);
            final short faded = (short) (orig * (invert ? 1 - v : v));
            buffer.putShort(i, faded);
        }
    }
}
