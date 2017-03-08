package com.soundcloud.android.stations;

import com.google.auto.value.AutoValue;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.tracks.TrackItem;

import java.util.ArrayList;
import java.util.List;

@AutoValue
abstract class StationInfoTracksBucket extends StationInfoItem {

    public static StationInfoTracksBucket from(StationWithTracks station) {
        return new AutoValue_StationInfoTracksBucket(station.getStationInfoTracks(), station.getPreviousPosition());
    }

    public static StationInfoTracksBucket from(StationWithTracks station, Urn nowPlaying) {
        final ArrayList<StationInfoTrack> updatedStationInfoTracks = updateWithNowPlaying(station, nowPlaying);
        return new AutoValue_StationInfoTracksBucket(updatedStationInfoTracks, station.getPreviousPosition());
    }

    private static ArrayList<StationInfoTrack> updateWithNowPlaying(StationWithTracks station, Urn nowPlaying) {
        final ArrayList<StationInfoTrack> updatedStationInfoTracks = new ArrayList<>(station.getStationInfoTracks().size());
        for (StationInfoTrack stationInfo : station.getStationInfoTracks()) {
            final boolean isPlaying = nowPlaying.equals(stationInfo.getUrn());
            final TrackItem trackItem = stationInfo.getTrack().withPlayingState(isPlaying);
            updatedStationInfoTracks.add(StationInfoTrack.from(trackItem));
        }
        return updatedStationInfoTracks;
    }

    StationInfoTracksBucket() {
        super(Kind.StationTracksBucket);
    }

    abstract List<StationInfoTrack> stationTracks();

    abstract int lastPlayedPosition();
}
