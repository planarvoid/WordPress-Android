package com.soundcloud.android.stations;

import com.google.auto.value.AutoValue;
import com.soundcloud.android.model.Urn;

import java.util.List;

@AutoValue
abstract class StationInfoTracksBucket extends StationInfoItem {

    public static StationInfoTracksBucket from(StationWithTracks station) {
        return new AutoValue_StationInfoTracksBucket(station.getStationInfoTracks(), station.getPreviousPosition());
    }

    public static StationInfoTracksBucket from(StationWithTracks station, Urn nowPlaying) {
        for (StationInfoTrack stationInfo : station.getStationInfoTracks()) {
            stationInfo.getTrack().setIsPlaying(nowPlaying.equals(stationInfo.getUrn()));
        }
        return new AutoValue_StationInfoTracksBucket(station.getStationInfoTracks(), station.getPreviousPosition());
    }

    StationInfoTracksBucket() {
        super(Kind.StationTracksBucket);
    }

    abstract List<StationInfoTrack> stationTracks();

    abstract int lastPlayedPosition();
}
