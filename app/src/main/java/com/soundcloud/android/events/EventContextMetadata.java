package com.soundcloud.android.events;

import com.google.auto.value.AutoValue;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.TrackSourceInfo;

import android.support.annotation.Nullable;

@AutoValue
public abstract class EventContextMetadata {

    public static Builder builder() {
        return new AutoValue_EventContextMetadata.Builder()
                .isFromOverflow(false)
                .pageUrn(Urn.NOT_SET);
    }

    public abstract boolean isFromOverflow();

    public abstract Urn pageUrn();

    @Nullable
    public abstract String pageName();

    @Nullable
    public abstract String invokerScreen();

    @Nullable
    public abstract String contextScreen();

    @Nullable
    public abstract TrackSourceInfo trackSourceInfo();

    @AutoValue.Builder
    public abstract static class Builder {
        public abstract Builder invokerScreen(String invokerScreen);

        public abstract Builder contextScreen(String contextScreen);

        public abstract Builder pageName(String pageName);

        public abstract Builder pageUrn(Urn pageUrn);

        public abstract Builder trackSourceInfo(TrackSourceInfo sourceInfo);

        public abstract Builder isFromOverflow(boolean isFromOverflow);

        public abstract EventContextMetadata build();

    }

}
