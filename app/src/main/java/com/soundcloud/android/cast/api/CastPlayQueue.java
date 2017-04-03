package com.soundcloud.android.cast.api;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import com.soundcloud.android.model.Urn;
import com.soundcloud.java.collections.Lists;
import com.soundcloud.java.optional.Optional;
import com.soundcloud.java.strings.Strings;

import java.util.List;

@AutoValue
public abstract class CastPlayQueue {

    private static final String CAST_PROTOCOL_VERSION = "1.0.0";

    @JsonProperty("revision")
    public abstract Optional<String> revision();

    @JsonProperty("queue")
    public abstract List<RemoteTrack> queue();

    @JsonProperty("current_index")
    public abstract int currentIndex();

    @JsonProperty("progress")
    public abstract long progress();

    @JsonProperty("source")
    public abstract String source();

    @JsonProperty("version")
    public abstract String version();

    @JsonProperty("credentials")
    public abstract Optional<CastCredentials> credentials();

    @JsonCreator
    public static CastPlayQueue deserialize(@JsonProperty("revision") String revision,
                                            @JsonProperty("queue") List<RemoteTrack> queue,
                                            @JsonProperty("current_index") int currentIndex,
                                            @JsonProperty("progress") long progress,
                                            @JsonProperty("source") String source,
                                            @JsonProperty("version") String version,
                                            @JsonProperty("credentials") CastCredentials credentials) {
        return new AutoValue_CastPlayQueue.Builder().revision(Optional.of(revision))
                                                    .queue(queue)
                                                    .currentIndex(currentIndex)
                                                    .progress(progress)
                                                    .source(source == null ? Strings.EMPTY : source)
                                                    .version(version)
                                                    .credentials(Optional.fromNullable(credentials))
                                                    .build();
    }

    public abstract Builder toBuilder();

    private static Builder builder(Urn currentUrn, List<Urn> tracks) {
        return new AutoValue_CastPlayQueue.Builder().revision(Optional.absent())
                                                    .queue(Lists.transform(tracks, RemoteTrack::create))
                                                    .currentIndex(tracks.indexOf(currentUrn))
                                                    .progress(0)
                                                    .source(Strings.EMPTY)
                                                    .version(CAST_PROTOCOL_VERSION)
                                                    .credentials(Optional.absent());
    }

    public static CastPlayQueue create(Urn currentUrn, List<Urn> tracks) {
        return builder(currentUrn, tracks).build();
    }

    public static CastPlayQueue create(Optional<String> revision, Urn currentUrn, List<Urn> tracks) {
        return builder(currentUrn, tracks).revision(revision).build();
    }

    public static CastPlayQueue forUpdate(Urn currentUrn, long progress, CastPlayQueue original) {
        return original.toBuilder().currentIndex(original.getQueueUrns().indexOf(currentUrn)).progress(progress).build();
    }

    public CastPlayQueue withCredentials(CastCredentials credentials) {
        return toBuilder().credentials(credentials).build();
    }

    @JsonIgnore
    public Urn getCurrentTrackUrn() {
        int currentTrackIndex = currentIndex();
        boolean isWithinRange = currentTrackIndex >= 0 && currentTrackIndex < queue().size();
        return isWithinRange ? queue().get(currentTrackIndex).urn() : Urn.NOT_SET;
    }

    @JsonIgnore
    public List<Urn> getQueueUrns() {
        return Lists.transform(queue(), RemoteTrack::urn);
    }

    @JsonIgnore
    public boolean contains(Urn urn) {
        return getQueueUrns().contains(urn);
    }

    @JsonIgnore
    public boolean isEmpty() {
        return queue().isEmpty();
    }

    @JsonIgnore
    public boolean hasSameTracks(List<Urn> tracks) {
        return !isEmpty() && tracks != null && tracks.equals(getQueueUrns());
    }

    @AutoValue.Builder
    static abstract class Builder {

        abstract Builder revision(Optional<String> revision);

        abstract Builder queue(List<RemoteTrack> queue);

        abstract Builder currentIndex(int currentIndex);

        abstract Builder progress(long progress);

        abstract Builder source(String source);

        abstract Builder version(String version);

        abstract Builder credentials(Optional<CastCredentials> credentials);

        Builder credentials(CastCredentials credentials) {
            return credentials(Optional.of(credentials));
        }

        abstract CastPlayQueue build();

    }
}
