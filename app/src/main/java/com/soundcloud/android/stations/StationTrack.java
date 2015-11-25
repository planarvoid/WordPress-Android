package com.soundcloud.android.stations;

import com.google.auto.value.AutoValue;
import com.soundcloud.android.model.Urn;
import com.soundcloud.java.functions.Function;

@AutoValue
public abstract class StationTrack {
    public static Function<StationTrack, Urn> TO_URN = new Function<StationTrack, Urn>() {
        public Urn apply(StationTrack track) {
            return track.getTrackUrn();
        }
    };

    public static StationTrack create(Urn trackUrn, Urn queryUrn) {
        return new AutoValue_StationTrack(trackUrn, queryUrn);
    }

    public abstract Urn getTrackUrn();
    public abstract Urn getQueryUrn();
}
