package com.soundcloud.android.stations;

import static com.soundcloud.android.stations.ApiStationMetadata.TO_URN;
import static com.soundcloud.android.stations.LikedStationsPostBody.create;

import com.soundcloud.android.model.Urn;
import com.soundcloud.android.utils.DiffUtils;
import com.soundcloud.java.collections.Lists;

import javax.inject.Inject;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;

class LikedStationsSyncer implements Callable<Boolean> {

    private final StationsApi api;
    private final StationsStorage storage;

    @Inject
    public LikedStationsSyncer(StationsApi api,
                               StationsStorage storage) {
        this.api = api;
        this.storage = storage;
    }

    @Override
    public Boolean call() throws Exception {

        final List<Urn> likedStations = storage.getLocalLikedStations();
        final List<Urn> unlikedStations = storage.getLocalUnlikedStations();
        final LikedStationsPostBody likedStationsPostBody = create(unlikedStations, likedStations);

        final List<Urn> knownStations = storage.getStations();
        final List<Urn> remoteLikedStations = api.updateLikedStations(likedStationsPostBody).getCollection();
        final List<Urn> toSync = DiffUtils.minus(remoteLikedStations, knownStations);

        final List<ApiStationMetadata> stationsFromApi = toSync.isEmpty() ?
                                                         Collections.emptyList() :
                                                         api.fetchStations(toSync);

        final List<Urn> likedStationsUrns = removeUrnsOfStationsWithoutMetadata(remoteLikedStations,
                                                                                toSync,
                                                                                stationsFromApi);

        storage.setLikedStationsAndAddNewMetaData(likedStationsUrns, stationsFromApi);
        return true;
    }

    private List<Urn> removeUrnsOfStationsWithoutMetadata(List<Urn> remoteLikedStations,
                                                          List<Urn> toSync,
                                                          List<ApiStationMetadata> stationsFromApi) {

        final List<Urn> stationsWithKnownMetadata = Lists.transform(stationsFromApi, TO_URN);
        final List<Urn> urnsOfStationsMissingMetadata = DiffUtils.minus(toSync, stationsWithKnownMetadata);
        return DiffUtils.minus(remoteLikedStations, urnsOfStationsMissingMetadata);
    }

}
