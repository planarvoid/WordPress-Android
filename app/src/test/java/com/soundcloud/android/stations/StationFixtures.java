package com.soundcloud.android.stations;

import static com.soundcloud.java.collections.Lists.transform;

import com.soundcloud.android.api.model.ApiTrack;
import com.soundcloud.android.api.model.ModelCollection;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import com.soundcloud.android.tracks.TrackItem;
import com.soundcloud.android.tracks.TrackRecord;
import com.soundcloud.java.functions.Function;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

public class StationFixtures {
    private static final Random random = new Random();

    private static Function<TrackRecord, StationInfoTrack> toStationInfoTrack = input -> StationInfoTrack.from((ApiTrack) input);

    public static ApiStation getApiStation() {
        return getApiStation(Urn.forTrackStation(random.nextLong()));
    }

    static ApiStation getApiStation(Urn station) {
        return getApiStation(station, 1);
    }

    static ApiStation getApiStation(Urn station, int size) {
        return getApiStation(station, ModelFixtures.create(ApiTrack.class, size));
    }

    static ApiStation getApiStation(Urn station, List<ApiTrack> tracks) {
        return new ApiStation(getApiStationMetadata(station),
                              new ModelCollection<>(tracks, null, "soundcloud:radio:123123123"));
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

    static StationWithTracks getStationWithTracks(Urn station) {
        final ApiStation apiStation = getApiStation(station);
        final List<StationInfoTrack> tracks = transform(apiStation.getTrackRecords(), toStationInfoTrack);

        return getStationWithTracks(apiStation, tracks, Stations.NEVER_PLAYED);
    }

    static StationWithTracks getStationWithTracks(Urn station, List<StationInfoTrack> tracks) {
        return getStationWithTracks(getApiStation(station), tracks, Stations.NEVER_PLAYED);
    }

    static StationWithTracks getStationWithTracks(Urn station, List<StationInfoTrack> tracks, int lastPlayedPosition) {
        return getStationWithTracks(getApiStation(station), tracks, lastPlayedPosition);
    }

    private static StationWithTracks getStationWithTracks(ApiStation apiStation,
                                                          List<StationInfoTrack> tracks, int lastPlayed) {
        return new StationWithTracks(
                apiStation.getUrn(),
                apiStation.getTitle(),
                apiStation.getType(),
                apiStation.getImageUrlTemplate(),
                apiStation.getPermalink(),
                tracks,
                lastPlayed,
                true);
    }

    public static StationRecord getStation(Urn urn) {
        return getStation(urn, 1);
    }

    public static StationRecord getStation(Urn urn, int size, int previouslyPlayedPosition) {
        return getStation(getApiStation(urn, size), previouslyPlayedPosition);
    }

    public static StationRecord getStation(Urn urn, int size) {
        return getStation(getApiStation(urn, size), Stations.NEVER_PLAYED);
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

    public static StationRecord getStation(ApiStation apiStation, int position) {
        return new Station(
                apiStation.getUrn(),
                apiStation.getTitle(),
                apiStation.getType(),
                apiStation.getTracks(),
                apiStation.getPermalink(),
                position,
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

    static StationInfoTrack createStationInfoTrack(int playCount, String artistName) {
        final TrackItem trackItem = ModelFixtures.trackItem(ModelFixtures.trackBuilder().playCount(playCount).creatorName(artistName).build());
        return StationInfoTrack.from(trackItem);
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
