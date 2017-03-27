package com.soundcloud.android.events;

import com.google.auto.value.AutoValue;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.TrackSourceInfo;
import com.soundcloud.java.optional.Optional;

import android.support.annotation.Nullable;

@AutoValue
public abstract class EventContextMetadata {

    public static Builder builder() {
        return new AutoValue_EventContextMetadata.Builder()
                .isFromOverflow(false)
                .clickSource(Optional.absent())
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
    public abstract LinkType linkType();

    @Nullable
    public abstract AttributingActivity attributingActivity();

    @Nullable
    public abstract Module module();

    @Nullable
    public abstract TrackSourceInfo trackSourceInfo();

    public abstract Optional<String> clickSource();

    @AutoValue.Builder
    public abstract static class Builder {
        public abstract Builder invokerScreen(String invokerScreen);

        public abstract Builder contextScreen(String contextScreen);

        public abstract Builder pageName(String pageName);

        public abstract Builder pageUrn(Urn pageUrn);

        public abstract Builder trackSourceInfo(TrackSourceInfo sourceInfo);

        public abstract Builder linkType(LinkType linkType);

        public abstract Builder module(Module module);

        public abstract Builder attributingActivity(AttributingActivity attributingActivityType);

        public abstract Builder isFromOverflow(boolean isFromOverflow);

        public abstract Builder clickSource(Optional<String> clickSource);

        public abstract EventContextMetadata build();
    }
}
