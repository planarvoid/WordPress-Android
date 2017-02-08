package com.soundcloud.android.stations;

import com.google.auto.value.AutoValue;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.model.UrnHolder;
import com.soundcloud.java.optional.Optional;

import java.util.Collections;
import java.util.List;

@AutoValue
abstract class StationWithTrackUrns implements UrnHolder {

    public abstract String type();
    public abstract String title();
    public abstract Optional<String> permalink();
    public abstract List<Urn> trackUrns();
    public abstract Optional<String> imageUrlTemplate();
    public abstract int lastPlayedTrackPosition();
    public abstract boolean liked();

    public static StationWithTrackUrns create(Urn urn, String type, String title, Optional<String> permalink, Optional<String> imageUrlTemplate, int lastPlayedTrackPosition, boolean liked) {
        return new AutoValue_StationWithTrackUrns(urn, type, title, permalink, Collections.emptyList(), imageUrlTemplate, lastPlayedTrackPosition, liked);
    }

    public StationWithTrackUrns copyWithTrackUrns(List<Urn> trackUrns) {
        return new AutoValue_StationWithTrackUrns(urn(), type(), title(), permalink(), trackUrns, imageUrlTemplate(), lastPlayedTrackPosition(), liked());
    }
}
