package com.soundcloud.android.audio.filter;

import com.soundcloud.android.audio.AudioConfig;
import com.soundcloud.android.audio.AudioReader;
import com.soundcloud.android.audio.PlaybackFilter;
import com.soundcloud.android.audio.PlaybackStream;

import android.os.Parcel;
import android.os.Parcelable;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class FadeFilter implements PlaybackFilter {

    public static final int FADE_TYPE_BOTH = 0;
    public static final int FADE_TYPE_BEGINNING = 1;
    public static final int FADE_TYPE_END = 2;

    private static final int FADE_LENGTH_MS = 3000;
    private static final int FADE_EXP_CURVE = 2;

    private final long mFadeSize;
    private final int mFadeType;
    private final int mFadeExpCurve;

    public FadeFilter(AudioConfig config) {
        this(config, FADE_TYPE_BOTH);
    }

    public FadeFilter(AudioConfig config, int fadeType) {
        this(fadeType,config.bytesToMs(FADE_LENGTH_MS));
    }

    public FadeFilter(int fadeType, long fadeSizeInBytes) {
        this(fadeType,fadeSizeInBytes,FADE_EXP_CURVE);
    }

    public FadeFilter(int fadeType, long fadeSizeInBytes, int fadeExpCurve) {
        mFadeSize = fadeSizeInBytes;
        mFadeType = fadeType;
        mFadeExpCurve = fadeExpCurve;
    }


    /**
     *
     * @param buffer the audio data
     * @param position where (byte offset) in relation to the total piece of audio does this buffer belong
     * @param length what is the total length (bytes) of the audio that this buffer belongs to
     * @return
     */
    @Override
    public ByteBuffer apply(ByteBuffer buffer, long position, long length) {
        final int remaining = (int) Math.min(length - position, buffer.remaining());
        if (position < mFadeSize && (mFadeType == FADE_TYPE_BEGINNING || mFadeType == FADE_TYPE_BOTH)) {
            final int count = (int) Math.min(remaining, mFadeSize - position);
            applyVolumeChangeToBuffer(buffer, position, 0, count, 0, false);
        }

        final long fadeOutIdx = length - mFadeSize;

        if (position + buffer.remaining() > fadeOutIdx && (mFadeType == FADE_TYPE_END || mFadeType == FADE_TYPE_BOTH)) {
            int start = (int) (position >= fadeOutIdx ? 0 : fadeOutIdx - position);
            applyVolumeChangeToBuffer(buffer, position, start, remaining - start, fadeOutIdx, true);

        }
        return buffer;
    }

    /**
     * Apply the volume change
     * @param buffer the audio data
     * @param position where (byte offset) in relation to the total piece of audio does this buffer belong
     * @param start where (byte offset) in the buffer to start the fade
     * @param count how many bytes to process
     * @param fadeOffset where (byte offset) the fade is positioned
     * @param invert is this a reverse fade?
     */
    private void applyVolumeChangeToBuffer(ByteBuffer buffer, long position, int start, int count, long fadeOffset, boolean invert) {
        start = Math.max(0, start - (start % 2)); //validate short
        for (int i = start; i < start + count; i += 2) {
            final double x = (position + i - fadeOffset) / ((double) mFadeSize);
            final double v = Math.pow(x, mFadeExpCurve);
            final short orig = buffer.getShort(i);
            final short faded = (short) (orig * (invert ? 1 - v : v));
            buffer.putShort(i, faded);
        }
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeLong(mFadeType);
        dest.writeLong(mFadeSize);
        dest.writeLong(mFadeExpCurve);

    }

    public static final Parcelable.Creator<FadeFilter> CREATOR = new Parcelable.Creator<FadeFilter>() {
        public FadeFilter createFromParcel(Parcel in) {
            return new FadeFilter(in.readInt(), in.readLong(), in.readInt());
        }

        public FadeFilter[] newArray(int size) {
            return new FadeFilter[size];
        }
    };
}
