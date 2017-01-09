package com.soundcloud.android.collection.playhistory;

import com.google.auto.value.AutoValue;
import com.soundcloud.android.model.Urn;
import com.soundcloud.java.functions.Function;

@AutoValue
public abstract class PlayHistoryRecord {

    static final Function<PlayHistoryRecord, Urn> TO_TRACK_URN = input -> input.trackUrn();

    public static final int CONTEXT_OTHER = 0;
    public static final int CONTEXT_PLAYLIST = 1;
    public static final int CONTEXT_TRACK_STATION = 2;
    public static final int CONTEXT_ARTIST_STATION = 3;
    public static final int CONTEXT_ARTIST = 4;

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
        } else if (contextUrn().isTrackStation()) {
            return CONTEXT_TRACK_STATION;
        } else if (contextUrn().isArtistStation()) {
            return CONTEXT_ARTIST_STATION;
        } else if (contextUrn().isUser()) {
            return CONTEXT_ARTIST;
        } else {
            return CONTEXT_OTHER;
        }
    }

    public static Urn contextUrnFor(int contextType, long contextId) {
        switch (contextType) {
            case CONTEXT_PLAYLIST:
                return Urn.forPlaylist(contextId);
            case CONTEXT_TRACK_STATION:
                return Urn.forTrackStation(contextId);
            case CONTEXT_ARTIST_STATION:
                return Urn.forArtistStation(contextId);
            case CONTEXT_ARTIST:
                return Urn.forUser(contextId);
            default:
                return Urn.NOT_SET;
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
