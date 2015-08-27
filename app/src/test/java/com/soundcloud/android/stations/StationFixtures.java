package com.soundcloud.android.stations;

import com.soundcloud.android.api.model.ApiTrack;
import com.soundcloud.android.api.model.ModelCollection;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

public class StationFixtures {
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
                apiStation.getTracks(),
                apiStation.getPermalink(),
                0
        );
    }

    public static ApiStationsCollections collections() {
        return collections(
                Arrays.asList(Urn.forTrackStation(1L), Urn.forTrackStation(2L)),
                Arrays.asList(Urn.forTrackStation(3L), Urn.forTrackStation(4L)),
                Arrays.asList(Urn.forTrackStation(5L), Urn.forTrackStation(6L)),
                Arrays.asList(Urn.forTrackStation(7L), Urn.forTrackStation(8L)),
                Arrays.asList(Urn.forTrackStation(9L), Urn.forTrackStation(10L))
        );
    }

    public static ApiStationsCollections collections(List<Urn> recents,
                                                     List<Urn> saved,
                                                     List<Urn> trackRecommendations,
                                                     List<Urn> genreRecommendations,
                                                     List<Urn> curatorRecommendations) {
        return ApiStationsCollections.create(
                createStationsCollection(recents),
                createStationsCollection(saved),
                createStationsCollection(trackRecommendations),
                createStationsCollection(genreRecommendations),
                createStationsCollection(curatorRecommendations)
        );
    }

    private static ModelCollection<ApiStationMetadata> createStationsCollection(List<Urn> stations) {
        final ModelCollection<ApiStationMetadata> collection = new ModelCollection<>();
        List<ApiStationMetadata> stationsCollection = new ArrayList<>();

        for (Urn station : stations) {
            stationsCollection.add(getApiStation(station).getMetadata());
        }

        collection.setCollection(stationsCollection);
        return collection;
    }
}