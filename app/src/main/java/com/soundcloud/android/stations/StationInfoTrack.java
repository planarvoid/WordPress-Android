package com.soundcloud.android.stations;

import com.google.auto.value.AutoValue;
import com.soundcloud.android.api.model.ApiTrack;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.presentation.ListItem;
import com.soundcloud.android.presentation.UpdatableItem;
import com.soundcloud.android.tracks.TrackItem;
import com.soundcloud.java.collections.PropertySet;
import com.soundcloud.java.optional.Optional;

@AutoValue
abstract class StationInfoTrack implements ListItem, UpdatableItem {

    public static StationInfoTrack from(ApiTrack track) {
        return new AutoValue_StationInfoTrack(TrackItem.from(track));
    }

    public static StationInfoTrack from(PropertySet track) {
        return new AutoValue_StationInfoTrack(TrackItem.from(track));
    }

    public static StationInfoTrack from(TrackItem trackItem) {
        return new AutoValue_StationInfoTrack(trackItem);
    }

    public abstract TrackItem getTrack();

    @Override
    public StationInfoTrack updated(PropertySet sourceSet) {
        return new AutoValue_StationInfoTrack(getTrack().updated(sourceSet));
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
