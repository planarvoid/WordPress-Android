package com.soundcloud.android.creators.record.jni;

import com.soundcloud.android.creators.record.PlaybackFilter;
import com.soundcloud.android.creators.record.SoundRecorder;
import org.jetbrains.annotations.Nullable;

public class EncoderOptions {

    /**
     * start of the encoding stream in msecs
     */
    public final long start;

    /**
     * end of the encoding stream in msecs, or -1 for end of stream
     */
    public final long end;

    /**
     * encoding quality: 0 - 1.0
     */
    public final float quality;

    /**
     * if not null, apply this filter to the source stream
     */
    @Nullable public final PlaybackFilter filter;

    /**
     * for reporting encoding progress
     */
    @Nullable public final ProgressListener listener;

    public static final EncoderOptions HI_Q = new EncoderOptions(1f, 0, -1, null, null);
    public static final EncoderOptions LO_Q = new EncoderOptions(.1f, 0, -1, null, null);
    public static final EncoderOptions MED_Q = new EncoderOptions(.5f, 0, -1, null, null);

    public static final EncoderOptions DEFAULT = SoundRecorder.hasFPUSupport() ? MED_Q : LO_Q;

    /**
     * @param quality
     * @param start    start in millisecs
     * @param end      end in millisecs, or -1 for whole file
     * @param listener optional progress listener
     * @param filter   optional audio filter
     */
    public EncoderOptions(float quality, long start, long end,
                          @Nullable ProgressListener listener,
                          @Nullable PlaybackFilter filter) {

        if (quality < 0 || quality > 1f) {
            throw new IllegalArgumentException("invalid quality: " + quality);
        }
        if (start < 0) {
            throw new IllegalArgumentException("invalid start: " + start);
        }
        if (end < -1) {
            throw new IllegalArgumentException("invalid end: " + end);
        }

        this.start = start;
        this.end = end;
        this.filter = filter;
        this.quality = quality;
        this.listener = listener;
    }


    @Override
    public String toString() {
        return "EncoderOptions{" +
                "start=" + start +
                ", end=" + end +
                ", quality=" + quality +
                ", filter=" + filter +
                ", listener=" + listener +
                '}';
    }
}
