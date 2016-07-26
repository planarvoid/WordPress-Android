package com.soundcloud.android.stations;

import com.google.auto.value.AutoValue;
import com.soundcloud.android.model.Urn;

import java.util.List;

@AutoValue
abstract class StationInfoTracksBucket extends StationInfoItem {

    public static StationInfoTracksBucket from(List<StationInfoTrack> tracks, int lastPlayed) {
        return new AutoValue_StationInfoTracksBucket(tracks, lastPlayed);
    }

    public static StationInfoTracksBucket from(List<StationInfoTrack> tracks, int lastPlayed, Urn nowPlaying) {
        for (StationInfoTrack stationInfo : tracks) {
            stationInfo.getTrack().setIsPlaying(nowPlaying.equals(stationInfo.getUrn()));
        }
        return new AutoValue_StationInfoTracksBucket(tracks, lastPlayed);
    }

    StationInfoTracksBucket() {
        super(Kind.StationTracksBucket);
    }

    abstract List<StationInfoTrack> stationTracks();

    abstract int lastPlayedPosition();
}
