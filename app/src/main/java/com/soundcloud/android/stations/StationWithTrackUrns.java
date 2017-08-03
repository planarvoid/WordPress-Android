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

    public static StationWithTrackUrns.Builder builder() {
        return new AutoValue_StationWithTrackUrns.Builder()
                .trackUrns(Collections.emptyList());
    }

    abstract Builder toBuilder();

    @AutoValue.Builder
    public abstract static class Builder {

        public abstract StationWithTrackUrns.Builder urn(Urn urn);

        public abstract StationWithTrackUrns.Builder type(String type);

        public abstract StationWithTrackUrns.Builder title(String title);

        public abstract StationWithTrackUrns.Builder permalink(Optional<String> permalink);

        public abstract StationWithTrackUrns.Builder imageUrlTemplate(Optional<String> imageUrlTemplate);

        public abstract StationWithTrackUrns.Builder lastPlayedTrackPosition(int lastPlayedTrackPosition);

        public abstract StationWithTrackUrns.Builder liked(boolean liked);

        public abstract StationWithTrackUrns.Builder trackUrns(List<Urn> trackUrns);

        public abstract StationWithTrackUrns build();
    }
}
