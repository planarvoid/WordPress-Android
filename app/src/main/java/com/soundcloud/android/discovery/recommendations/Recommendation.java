package com.soundcloud.android.discovery.recommendations;

import com.soundcloud.android.model.Urn;
import com.soundcloud.android.tracks.TrackItem;
import com.soundcloud.java.functions.Function;

class Recommendation {
    private final TrackItem track;
    private final Urn seedUrn;
    private boolean isPlaying;
    private int queryPosition;
    private Urn queryUrn;

    Recommendation(TrackItem track, Urn seedUrn, boolean isPlaying, int queryPosition, Urn queryUrn) {
        this.track = track;
        this.seedUrn = seedUrn;
        this.isPlaying = isPlaying;
        this.queryPosition = queryPosition;
        this.queryUrn = queryUrn;
    }

    Urn getTrackUrn() {
        return track.getUrn();
    }

    TrackItem getTrack() {
        return track;
    }

    Urn getSeedUrn() {
        return seedUrn;
    }

    boolean isPlaying() {
        return isPlaying;
    }

    void setIsPlaying(boolean isPlaying) {
        this.isPlaying = isPlaying;
    }

    int getQueryPosition() {
        return queryPosition;
    }

    Urn getQueryUrn() {
        return queryUrn;
    }

    public static final Function<Recommendation, Urn> TO_TRACK_URN = item -> item.getTrack().getUrn();
}
