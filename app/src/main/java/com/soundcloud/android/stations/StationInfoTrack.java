package com.soundcloud.android.stations;

import com.google.auto.value.AutoValue;
import com.soundcloud.android.api.model.ApiTrack;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.presentation.ListItem;
import com.soundcloud.android.presentation.UpdatableTrackItem;
import com.soundcloud.android.tracks.Track;
import com.soundcloud.android.tracks.TrackItem;
import com.soundcloud.java.optional.Optional;

@AutoValue
abstract class StationInfoTrack implements ListItem, UpdatableTrackItem {

    public static StationInfoTrack from(ApiTrack track) {
        return new AutoValue_StationInfoTrack(TrackItem.from(track));
    }

    public static StationInfoTrack from(TrackItem trackItem) {
        return new AutoValue_StationInfoTrack(trackItem);
    }

    public abstract TrackItem getTrack();

    @Override
    public StationInfoTrack updatedWithTrack(Track track) {
        return new AutoValue_StationInfoTrack(getTrack().toBuilder().track(track).build());
    }

    @Override
    public Optional<String> getImageUrlTemplate() {
        return getTrack().getImageUrlTemplate();
    }

    @Override
    public Urn getUrn() {
        return getTrack().getUrn();
    }
}
