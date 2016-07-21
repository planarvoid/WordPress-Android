package com.soundcloud.android.stations;

import com.google.auto.value.AutoValue;
import com.soundcloud.android.model.Urn;
import com.soundcloud.java.functions.Function;

@AutoValue
public abstract class StationTrack {
    public static Function<StationTrack, Long> TO_TRACK_IDS = new Function<StationTrack, Long>() {
        public Long apply(StationTrack track) {
            return track.getTrackUrn().getNumericId();
        }
    };

    public static StationTrack create(Urn trackUrn, Urn queryUrn) {
        return new AutoValue_StationTrack(trackUrn, queryUrn);
    }

    public abstract Urn getTrackUrn();

    public abstract Urn getQueryUrn();
}
