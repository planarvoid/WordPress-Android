package com.soundcloud.android.stations;

import com.soundcloud.android.api.model.ApiTrack;
import com.soundcloud.android.api.model.ModelCollection;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import com.soundcloud.java.collections.PropertySet;

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
        return getApiStation(station, 1);
    }

    public static ApiStation getApiStation(Urn station, int size) {
        final ModelCollection<ApiTrack> tracks = new ModelCollection<>(ModelFixtures.create(ApiTrack.class, size));
        return new ApiStation(getApiStationMetadata(station), tracks);
    }

    public static List<PropertySet> getRecentStationsToSync() {
        return getRecentStationsToSync(stationProperties());
    }

    public static List<PropertySet> getRecentStationsToSync(PropertySet... stations) {
        return Arrays.asList(stations);
    }

    public static PropertySet stationProperties() {
        return PropertySet.from(
                StationProperty.URN.bind(Urn.forTrackStation(123L)),
                StationProperty.UPDATED_LOCALLY_AT.bind(12345L),
                StationProperty.POSITION.bind(0)
        );
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
        return getStation(urn, 1);
    }

    public static Station getStation(Urn urn, int size) {
        return getStation(getApiStation(urn, size));
    }

    public static Station getStation(ApiStation apiStation) {
        return new Station(
                apiStation.getUrn(),
                apiStation.getTitle(),
                apiStation.getType(),
                apiStation.getTracks(),
                apiStation.getPermalink(),
                apiStation.getPreviousPosition()
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