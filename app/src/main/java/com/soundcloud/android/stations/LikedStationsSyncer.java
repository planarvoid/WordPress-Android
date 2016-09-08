package com.soundcloud.android.stations;

import static com.soundcloud.android.stations.LikedStationsPostBody.create;

import com.soundcloud.android.model.Urn;

import javax.inject.Inject;
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

        storage.setLikedStations(api.updateLikedStations(likedStationsPostBody).getCollection());
        return true;
    }

}
