package com.soundcloud.android.creators.record.filter;

import com.soundcloud.android.creators.record.AudioConfig;
import com.soundcloud.android.creators.record.PlaybackFilter;

import android.os.Parcel;
import android.os.Parcelable;

import java.nio.ByteBuffer;

public class FadeFilter implements PlaybackFilter {

    public static final int FADE_TYPE_BOTH = 0;
    public static final int FADE_TYPE_BEGINNING = 1;
    public static final int FADE_TYPE_END = 2;
    public static final Parcelable.Creator<FadeFilter> CREATOR = new Parcelable.Creator<FadeFilter>() {
        public FadeFilter createFromParcel(Parcel in) {
            return new FadeFilter(in.readInt(), in.readLong(), in.readInt());
        }

        public FadeFilter[] newArray(int size) {
            return new FadeFilter[size];
        }
    };
    private static final int FADE_LENGTH_MS = 1000;
    private static final int FADE_EXP_CURVE = 2;
    private final long fadeSize;
    private final int fadeType;
    private final int fadeExpCurve;

    public FadeFilter(AudioConfig config) {
        this(config, FADE_TYPE_BOTH);
    }

    public FadeFilter(AudioConfig config, int fadeType) {
        this(fadeType, config.msToByte(FADE_LENGTH_MS));
    }

    public FadeFilter(int fadeType, long fadeSizeInBytes) {
        this(fadeType, fadeSizeInBytes, FADE_EXP_CURVE);
    }

    public FadeFilter(int fadeType, long fadeSizeInBytes, int fadeExpCurve) {
        fadeSize = fadeSizeInBytes;
        this.fadeType = fadeType;
        this.fadeExpCurve = fadeExpCurve;
    }

    @Override
    public ByteBuffer apply(ByteBuffer buffer, long position, long length) {
        final int remaining = (int) Math.min(length - position, buffer.remaining());
        if (position < fadeSize && (fadeType == FADE_TYPE_BEGINNING || fadeType == FADE_TYPE_BOTH)) {
            final int count = (int) Math.min(remaining, fadeSize - position);
            applyVolumeChangeToBuffer(buffer, position, 0, count, 0, false);
        }

        final long fadeOutIdx = length - fadeSize;

        if (position + buffer.remaining() > fadeOutIdx && (fadeType == FADE_TYPE_END || fadeType == FADE_TYPE_BOTH)) {
            int start = (int) (position >= fadeOutIdx ? 0 : fadeOutIdx - position);
            applyVolumeChangeToBuffer(buffer, position, start, remaining - start, fadeOutIdx, true);
        }
        return buffer;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(fadeType);
        dest.writeLong(fadeSize);
        dest.writeInt(fadeExpCurve);
    }

    @Override
    public String toString() {
        return "FadeFilter{" +
                "fadeSize=" + fadeSize +
                ", fadeType=" + fadeType +
                ", fadeExpCurve=" + fadeExpCurve +
                '}';
    }

    /**
     * Apply the volume change
     *
     * @param buffer     the audio data
     * @param position   where (byte offset) in relation to the total piece of audio does this buffer belong
     * @param start      where (byte offset) in the buffer to start the fade
     * @param count      how many bytes to process
     * @param fadeOffset where (byte offset) the fade is positioned
     * @param invert     is this a reverse fade?
     */
    private void applyVolumeChangeToBuffer(ByteBuffer buffer,
                                           long position,
                                           int start,
                                           int count,
                                           long fadeOffset,
                                           boolean invert) {
        start = Math.max(0, start - (start % 2)); //validate short
        for (int i = start; i < start + count; i += 2) {
            final double x = (position + i - fadeOffset) / ((double) fadeSize);
            final double v = Math.pow(x, fadeExpCurve);
            final short orig = buffer.getShort(i);
            final short faded = (short) (orig * (invert ? 1 - v : v));
            buffer.putShort(i, faded);
        }
    }
}
