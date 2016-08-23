package com.soundcloud.android.stations;

import static java.lang.Math.min;

import com.google.auto.value.AutoValue;
import com.soundcloud.android.model.Urn;
import com.soundcloud.java.collections.MoreCollections;
import com.soundcloud.java.functions.Function;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;

@AutoValue
abstract class StationInfoTracksBucket extends StationInfoItem {

    private static final Comparator<StationInfoTrack> STATION_INFO_TRACK_COMPARATOR = new Comparator<StationInfoTrack>() {
        @Override
        public int compare(StationInfoTrack trackA, StationInfoTrack trackB) {
            return trackB.getTrack().getPlayCount() - trackA.getTrack().getPlayCount();
        }
    };

    private static final Function<StationInfoTrack, String> TO_CREATOR_NAME = new Function<StationInfoTrack, String>() {
        @Override
        public String apply(StationInfoTrack input) {
            return input.getTrack().getCreatorName();
        }
    };

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

    List<String> getMostPlayedArtists(int count) {
        final List<StationInfoTrack> sortedList = new ArrayList<>(stationTracks());
        Collections.sort(sortedList, STATION_INFO_TRACK_COMPARATOR);
        final Collection<String> artistNames = MoreCollections.transform(sortedList, TO_CREATOR_NAME);
        final LinkedHashSet<String> uniqueArtists = new LinkedHashSet<>(artistNames);

        return new ArrayList<>(uniqueArtists).subList(0, min(count, uniqueArtists.size()));
    }

    abstract List<StationInfoTrack> stationTracks();

    abstract int lastPlayedPosition();
}
