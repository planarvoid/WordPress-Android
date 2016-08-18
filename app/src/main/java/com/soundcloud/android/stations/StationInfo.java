package com.soundcloud.android.stations;

import com.google.auto.value.AutoValue;
import com.soundcloud.android.image.ImageResource;
import com.soundcloud.android.model.Entity;
import com.soundcloud.android.model.Urn;
import com.soundcloud.java.optional.Optional;
import rx.functions.Func1;

@AutoValue
abstract class StationInfo extends StationInfoItem implements ImageResource, Entity {

    static final Func1<StationRecord, StationInfoItem> FROM_STATION_RECORD = new Func1<StationRecord, StationInfoItem>() {
        @Override
        public StationInfoItem call(StationRecord stationRecord) {
            return StationInfo.from(stationRecord);
        }
    };

    public static StationInfo from(StationRecord record) {
        return new AutoValue_StationInfo(record.getUrn(),
                                         record.getType(),
                                         record.getTitle(),
                                         record.getImageUrlTemplate());
    }

    StationInfo() {
        super(Kind.StationHeader);
    }

    @Override
    public abstract Urn getUrn();

    public abstract String getType();

    public abstract String getTitle();

    @Override
    public abstract Optional<String> getImageUrlTemplate();

    public boolean isLiked() {
        // TODO: another story
        return false;
    }
}
