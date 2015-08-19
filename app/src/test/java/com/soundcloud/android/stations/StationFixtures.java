package com.soundcloud.android.stations;

import static com.soundcloud.java.collections.Lists.transform;

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
        return new ApiStation(getApiStationMetadata(station), tracks);
    }

    private static ApiStationMetadata getApiStationMetadata(Urn station) {
        return new ApiStationMetadata(
                station,
                "station " + System.currentTimeMillis(),
                "http://artwork",
                "fixture-stations"
        );
    }

    public static Station getStation(Urn urn) {
        return getStation(getApiStation(urn));
    }

    public static Station getStation(ApiStation apiStation) {
        return new Station(
                apiStation.getUrn(),
                apiStation.getTitle(),
                apiStation.getType(),
                transform(apiStation.getTracks().getCollection(), toUrn),
                0
        );
    }
}