package com.soundcloud.android.discovery;

import com.soundcloud.android.model.Urn;
import com.soundcloud.android.tracks.TrackItem;
import com.soundcloud.java.functions.Function;

class RecommendationViewModel {
    private final TrackItem track;
    private final Urn seedUrn;
    private boolean isPlaying;

    RecommendationViewModel(TrackItem track, Urn seedUrn, boolean isPlaying) {
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

    public static final Function<RecommendationViewModel, Urn> TO_TRACK_URN = new Function<RecommendationViewModel, Urn>() {
        @Override
        public Urn apply(RecommendationViewModel item) {
            return item.getTrack().getUrn();
        }
    };
}
