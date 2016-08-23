package com.soundcloud.android.stations;

import com.google.auto.value.AutoValue;
import com.soundcloud.android.image.ImageResource;
import com.soundcloud.android.model.Entity;
import com.soundcloud.android.model.Urn;
import com.soundcloud.java.optional.Optional;

import java.util.List;

@AutoValue
abstract class StationInfoHeader extends StationInfoItem implements ImageResource, Entity {

    public static StationInfoHeader from(StationRecord record, List<String> artists) {
        return new AutoValue_StationInfoHeader(record.getUrn(),
                                               record.getType(),
                                               record.getTitle(),
                                               artists,
                                               record.getImageUrlTemplate());
    }

    StationInfoHeader() {
        super(Kind.StationHeader);
    }

    @Override
    public abstract Urn getUrn();

    public abstract String getType();

    public abstract String getTitle();

    public abstract List<String> getMostPlayedArtists();

    @Override
    public abstract Optional<String> getImageUrlTemplate();

    public boolean isLiked() {
        // TODO: another story
        return false;
    }
}
