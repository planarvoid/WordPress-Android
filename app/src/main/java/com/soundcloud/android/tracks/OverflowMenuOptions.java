package com.soundcloud.android.tracks;

import com.google.auto.value.AutoValue;

@AutoValue
public abstract class OverflowMenuOptions {

    public static Builder builder() {
        return new AutoValue_OverflowMenuOptions
                .Builder()
                .showOffline(false);
    }

    public abstract boolean showOffline();

    @AutoValue.Builder
    public abstract static class Builder {
        public abstract Builder showOffline(boolean enabled);
        public abstract OverflowMenuOptions build();
    }
}
