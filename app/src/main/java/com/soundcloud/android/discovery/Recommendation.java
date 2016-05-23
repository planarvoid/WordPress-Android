package com.soundcloud.android.discovery;

import com.soundcloud.android.model.Urn;
import com.soundcloud.android.tracks.TrackItem;
import com.soundcloud.java.functions.Function;

class Recommendation {
    private final TrackItem track;
    private final Urn seedUrn;
    private boolean isPlaying;

    Recommendation(TrackItem track, Urn seedUrn, boolean isPlaying) {
        this.track = track;
        this.seedUrn = seedUrn;
        this.isPlaying = isPlaying;
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

    public static final Function<Recommendation, Urn> TO_TRACK_URN = new Function<Recommendation, Urn>() {
        @Override
        public Urn apply(Recommendation item) {
            return item.getTrack().getUrn();
        }
    };
}
