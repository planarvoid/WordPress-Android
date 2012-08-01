package com.soundcloud.android.jni;

import com.soundcloud.android.audio.AudioConfig;
import com.soundcloud.android.audio.PlaybackFilter;
import org.jetbrains.annotations.Nullable;

public class EncoderOptions {
    /** start of the encoding stream in msecs */
    public final long start;

    /** end of the encoding stream in msecs, or -1 for end of stream */
    public final long end;

    /** encoding quality: 0 - 1.0 */
    public final float quality;

    /** if not null, apply this filter to the source stream */
    public final @Nullable PlaybackFilter filter;

    /** for reporting encoding progress */
    public final @Nullable ProgressListener listener;

    public static final EncoderOptions DEFAULT = new EncoderOptions(AudioConfig.DEFAULT.quality, 0, -1, null, null);
    public static final EncoderOptions HI_Q = new EncoderOptions(1f, 0, -1, null, null);
    public static final EncoderOptions LO_Q = new EncoderOptions(.1f, 0, -1, null, null);
    public static final EncoderOptions MED_Q = new EncoderOptions(.5f, 0, -1, null, null);

    public EncoderOptions(float quality, long start, long end,
                          @Nullable ProgressListener listener,
                          @Nullable PlaybackFilter filter) {

        if (quality < 0 || quality > 1f) throw  new IllegalArgumentException("invalid quality: "+quality);
        if (start < 0) throw new IllegalArgumentException("invalid start: "+start);
        if (end < -1 ) throw new IllegalArgumentException("invalid end: "+end);

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
