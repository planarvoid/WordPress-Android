package com.soundcloud.android.stations;

import com.google.auto.value.AutoValue;
import rx.functions.Func1;

import java.util.List;

@AutoValue
abstract class StationInfoTracksBucket extends StationInfoItem {

    static final Func1<List<StationInfoTrack>, StationInfoItem> FROM_TRACK_ITEM_LIST = new Func1<List<StationInfoTrack>, StationInfoItem>() {
        @Override
        public StationInfoItem call(List<StationInfoTrack> trackItems) {
            return StationInfoTracksBucket.from(trackItems);
        }
    };

    public static StationInfoItem from(List<StationInfoTrack> stationTracks) {
        return new AutoValue_StationInfoTracksBucket(stationTracks);
    }

    StationInfoTracksBucket() {
        super(Kind.StationTracksBucket);
    }

    abstract List<StationInfoTrack> stationTracks();
}
