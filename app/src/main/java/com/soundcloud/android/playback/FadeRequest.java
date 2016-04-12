package com.soundcloud.android.playback;

import com.google.auto.value.AutoValue;
import com.soundcloud.annotations.VisibleForTesting;

@AutoValue
public abstract class FadeRequest {

    public static Builder builder() {
        return new AutoValue_FadeRequest.Builder();
    }

    @VisibleForTesting
    public static FadeRequest create(long duration, long offset, float startValue, float endValue) {
        return builder()
                .duration(duration)
                .offset(offset)
                .startValue(startValue)
                .endValue(endValue)
                .build();
    }

    public abstract long duration();

    public abstract long offset();

    public abstract float startValue();

    public abstract float endValue();

    @AutoValue.Builder
    public abstract static class Builder {
        public abstract Builder duration(long duration);

        public abstract Builder offset(long offset);

        public abstract Builder startValue(float startValue);

        public abstract Builder endValue(float endValue);

        public abstract FadeRequest build();
    }
}
