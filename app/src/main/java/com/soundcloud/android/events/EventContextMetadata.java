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
                .queryPosition(Optional.absent())
                .queryUrn(Optional.absent())
                .source(Optional.absent())
                .sourceUrn(Optional.absent())
                .sourceQueryUrn(Optional.absent())
                .sourceQueryPosition(Optional.absent())
                .pageUrn(Urn.NOT_SET);
    }

    public abstract boolean isFromOverflow();

    public abstract Urn pageUrn();

    @Nullable
    public abstract String pageName();

    @Nullable
    public abstract LinkType linkType();

    @Nullable
    public abstract AttributingActivity attributingActivity();

    @Nullable
    public abstract Module module();

    @Nullable
    public abstract TrackSourceInfo trackSourceInfo();

    public abstract Optional<Urn> queryUrn();

    public abstract Optional<Integer> queryPosition();

    public abstract Optional<String> source();

    public abstract Optional<Urn> sourceUrn();

    public abstract Optional<Urn> sourceQueryUrn();

    public abstract Optional<Integer> sourceQueryPosition();

    @AutoValue.Builder
    public abstract static class Builder {
        public abstract Builder pageName(String pageName);

        public abstract Builder pageUrn(Urn pageUrn);

        public abstract Builder trackSourceInfo(TrackSourceInfo sourceInfo);

        public abstract Builder linkType(LinkType linkType);

        public abstract Builder module(Module module);

        public abstract Builder attributingActivity(AttributingActivity attributingActivityType);

        public abstract Builder isFromOverflow(boolean isFromOverflow);

        public abstract Builder queryUrn(Optional<Urn> queryUrn);

        abstract Builder queryPosition(Optional<Integer> queryPosition);

        public Builder queryPosition(Integer queryPosition) {
            return queryPosition(Optional.of(queryPosition));
        }

        public abstract Builder source(Optional<String> source);

        public Builder source(String source) {
            return source(Optional.of(source));
        }

        public abstract Builder sourceUrn(Optional<Urn> sourceUrn);

        public Builder sourceUrn(Urn sourceUrn) {
            return sourceUrn(Optional.of(sourceUrn));
        }

        public abstract Builder sourceQueryUrn(Optional<Urn> sourceQueryUrn);

        abstract Builder sourceQueryPosition(Optional<Integer> sourcePosition);

        public Builder sourceQueryPosition(Integer sourcePosition) {
            return sourceQueryPosition(Optional.of(sourcePosition));
        }

        public abstract EventContextMetadata build();
    }
}
