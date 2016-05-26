package com.soundcloud.android.collection;

import com.google.auto.value.AutoValue;
import com.soundcloud.android.model.Urn;

@AutoValue
public abstract class PlayHistoryRecord {

    private static final int CONTEXT_OTHER = 0;
    private static final int CONTEXT_PLAYLIST = 1;
    private static final int CONTEXT_STATION = 2;
    private static final int CONTEXT_ARTIST = 3;

    public static PlayHistoryRecord create(long timestamp, Urn trackUrn, Urn contextUrn) {
        return builder()
                .timestamp(timestamp)
                .trackUrn(trackUrn)
                .contextUrn(contextUrn)
                .build();
    }

    public static Builder builder() {
        return new AutoValue_PlayHistoryRecord.Builder();
    }

    public abstract long timestamp();

    public abstract Urn trackUrn();

    public abstract Urn contextUrn();

    public int getContextType() {
        if (contextUrn().isPlaylist()) {
            return CONTEXT_PLAYLIST;
        } else if (contextUrn().isStation()) {
            return CONTEXT_STATION;
        } else if (contextUrn().isUser()) {
            return CONTEXT_ARTIST;
        } else {
            return CONTEXT_OTHER;
        }
    }

    @AutoValue.Builder
    public abstract static class Builder {
        public abstract Builder timestamp(long timestamp);

        public abstract Builder trackUrn(Urn urn);

        public abstract Builder contextUrn(Urn urn);

        public abstract PlayHistoryRecord build();
    }
}
