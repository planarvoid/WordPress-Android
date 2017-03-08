package com.soundcloud.android.discovery.recommendations;

import com.google.auto.value.AutoValue;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.tracks.TrackItem;
import com.soundcloud.java.functions.Function;

@AutoValue
abstract class Recommendation {

    static final Function<Recommendation, Urn> TO_TRACK_URN = item -> item.getTrack().getUrn();

    abstract TrackItem getTrack();

    abstract Urn getSeedUrn();

    abstract boolean isPlaying();

    abstract int getQueryPosition();

    abstract Urn getQueryUrn();

    Urn getTrackUrn() {
        return getTrack().getUrn();
    }

    public static Recommendation create(TrackItem newTrack, Urn newSeedUrn, boolean newPlaying, int newQueryPosition, Urn newQueryUrn) {
        return builder()
                .setTrack(newTrack)
                .setSeedUrn(newSeedUrn)
                .setPlaying(newPlaying)
                .setQueryPosition(newQueryPosition)
                .setQueryUrn(newQueryUrn)
                .build();
    }

    public abstract Builder toBuilder();

    public static Builder builder() {
        return new AutoValue_Recommendation.Builder();
    }

    @AutoValue.Builder
    public abstract static class Builder {
        public abstract Builder setTrack(TrackItem newTrack);

        public abstract Builder setSeedUrn(Urn newSeedUrn);

        public abstract Builder setPlaying(boolean newPlaying);

        public abstract Builder setQueryPosition(int newQueryPosition);

        public abstract Builder setQueryUrn(Urn newQueryUrn);

        public abstract Recommendation build();
    }
}
