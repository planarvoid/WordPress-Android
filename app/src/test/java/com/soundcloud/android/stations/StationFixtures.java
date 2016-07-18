package com.soundcloud.android.stations;

import static com.soundcloud.android.testsupport.fixtures.ModelFixtures.trackItems;
import static com.soundcloud.java.collections.Iterables.transform;
import static com.soundcloud.java.collections.Lists.newArrayList;

import com.soundcloud.android.api.model.ApiTrack;
import com.soundcloud.android.api.model.ModelCollection;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import com.soundcloud.android.tracks.TrackItem;
import com.soundcloud.java.collections.PropertySet;
import com.soundcloud.java.functions.Function;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

public class StationFixtures {
    private static final Random random = new Random();

    public static ApiStation getApiStation() {
        return getApiStation(Urn.forTrackStation(random.nextLong()));
    }

    static ApiStation getApiStation(Urn station) {
        return getApiStation(station, 1);
    }

    static ApiStation getApiStation(Urn station, int size) {
        return getApiStation(station, ModelFixtures.create(ApiTrack.class, size));
    }

    private static ApiStation getApiStation(Urn station, List<ApiTrack> tracks) {
        return new ApiStation(getApiStationMetadata(station),
                              new ModelCollection<>(tracks, null, "soundcloud:radio:123123123"));
    }

    static List<PropertySet> getRecentStationsToSync(PropertySet... stations) {
        return Arrays.asList(stations);
    }

    static PropertySet stationProperties() {
        return PropertySet.from(
                StationProperty.URN.bind(Urn.forTrackStation(123L)),
                StationProperty.UPDATED_LOCALLY_AT.bind(12345L),
                StationProperty.POSITION.bind(0)
        );
    }

    static List<StationInfoTrack> getStationTracks(int size) {
        return newArrayList(transform(trackItems(size), new Function<TrackItem, StationInfoTrack>() {
            @Override
            public StationInfoTrack apply(TrackItem trackItem) {
                return StationInfoTrack.from(trackItem.getUrn(),
                                            trackItem.getTitle(),
                                            trackItem.getCreatorName(),
                                            trackItem.getCreatorUrn(),
                                            trackItem.getImageUrlTemplate());
            }
        }));
    }

    private static ApiStationMetadata getApiStationMetadata(Urn station) {
        return new ApiStationMetadata(
                station,
                "stationWithSeed " + System.currentTimeMillis(),
                "http://permalink",
                getStationType(station),
                "https://i1.sndcdn.com/artworks-000056536728-bjjprz-{size}.jpg"
        );
    }

    private static String getStationType(Urn urn) {
        if (urn.isTrackStation()) {
            return "track";
        } else if (urn.isArtistStation()) {
            return "artist";
        }

        return "fixture-stations";
    }

    public static StationRecord getStation(Urn urn) {
        return getStation(urn, 1);
    }

    public static StationRecord getStation(Urn urn, int size) {
        return getStation(getApiStation(urn, size));
    }

    public static StationRecord getStation(ApiStation apiStation) {
        return new Station(
                apiStation.getUrn(),
                apiStation.getTitle(),
                apiStation.getType(),
                apiStation.getTracks(),
                apiStation.getPermalink(),
                apiStation.getPreviousPosition(),
                apiStation.getImageUrlTemplate());
    }

    public static ApiStationsCollections collections() {
        return collections(
                Arrays.asList(Urn.forTrackStation(1L), Urn.forTrackStation(2L))
        );
    }

    public static ApiStationsCollections collections(List<Urn> recents) {
        return ApiStationsCollections.create(createStationsCollection(recents));
    }

    static ModelCollection<ApiStationMetadata> createStationsCollection(List<Urn> stations) {
        final List<ApiStationMetadata> stationsCollection = new ArrayList<>();

        for (Urn station : stations) {
            stationsCollection.add(getApiStation(station).getMetadata());
        }
        return new ModelCollection<>(
                stationsCollection,
                null,
                "soundcloud:radio:123123123");
    }
}
