package com.soundcloud.android.stations;

import com.google.auto.value.AutoValue;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.presentation.ListItem;
import com.soundcloud.java.collections.PropertySet;
import com.soundcloud.java.optional.Optional;

@AutoValue
abstract class StationInfoTrack implements ListItem {

    static StationInfoTrack from(Urn urn, String title, String creator, Urn creatorUrn,
                                 Optional<String> imageUrlTemplate) {
        return new AutoValue_StationInfoTrack(urn, title, creator, creatorUrn, imageUrlTemplate);
    }

    @Override
    public ListItem update(PropertySet sourceSet) {
        // TODO:
        return null;
    }

    @Override
    public abstract Urn getUrn();

    public abstract String getTitle();

    public abstract String getCreator();

    public abstract Urn getCreatorUrn();

    @Override
    public abstract Optional<String> getImageUrlTemplate();

}
