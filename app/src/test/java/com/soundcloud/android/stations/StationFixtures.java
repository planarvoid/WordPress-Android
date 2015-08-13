package com.soundcloud.android.stations;

import static com.soundcloud.java.collections.Lists.transform;

import com.soundcloud.android.api.model.ApiStation;
import com.soundcloud.android.api.model.ApiStationInfo;
import com.soundcloud.android.api.model.ApiTrack;
import com.soundcloud.android.api.model.ModelCollection;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import com.soundcloud.android.tracks.TrackRecord;
import com.soundcloud.java.functions.Function;

import java.util.Random;

public class StationFixtures {
    private static final Function<TrackRecord, Urn> toUrn = new Function<TrackRecord, Urn>() {
        @Override
        public Urn apply(TrackRecord track) {
            return track.getUrn();
        }
    };
    private static final Random random = new Random();

    public static ApiStation getApiStation() {
        return getApiStation(Urn.forTrackStation(random.nextLong()));
    }

    public static ApiStation getApiStation(Urn station) {
        final ModelCollection<ApiTrack> tracks = new ModelCollection<>(ModelFixtures.create(ApiTrack.class, 1));
        return new ApiStation(getApiStationInfo(station), tracks);
    }

    private static ApiStationInfo getApiStationInfo(Urn station) {
        final ApiTrack seedTrack = ModelFixtures.create(ApiTrack.class);
        return new ApiStationInfo(station, "station " + System.currentTimeMillis(), "fixture-stations", seedTrack);
    }

    public static Station getStation(ApiStation apiStation) {
        return new Station(apiStation.getInfo().getUrn(), apiStation.getInfo().getTitle(), transform(apiStation.getTracks().getCollection(), toUrn), 0);
    }
}