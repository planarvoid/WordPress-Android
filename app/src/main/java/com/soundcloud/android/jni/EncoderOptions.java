package com.soundcloud.android.jni;

import com.soundcloud.android.audio.AudioConfig;
import com.soundcloud.android.audio.PlaybackFilter;
import org.jetbrains.annotations.Nullable;

public class EncoderOptions {

    public final long start;
    public final long end;
    public final float quality;
    public final @Nullable PlaybackFilter filter;
    public final @Nullable ProgressListener listener;

    public static final EncoderOptions DEFAULT = new EncoderOptions(AudioConfig.DEFAULT.quality, 0, -1, null, null);
    public static final EncoderOptions HI_Q = new EncoderOptions(1f, 0, -1, null, null);
    public static final EncoderOptions LO_Q = new EncoderOptions(.1f, 0, -1, null, null);
    public static final EncoderOptions MED_Q = new EncoderOptions(.5f, 0, -1, null, null);

    public EncoderOptions(float quality, long start, long end,
                          @Nullable ProgressListener listener,
                          @Nullable PlaybackFilter filter) {
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
