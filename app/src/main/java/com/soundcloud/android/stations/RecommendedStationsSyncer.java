package com.soundcloud.android.stations;

import com.soundcloud.android.api.model.ModelCollection;
import com.soundcloud.android.sync.SyncStateStorage;

import javax.inject.Inject;
import java.util.concurrent.Callable;

class RecommendedStationsSyncer implements Callable<Boolean> {

    private final SyncStateStorage syncStateStorage;
    private final StationsApi api;
    private final WriteStationsRecommendationsCommand writeCollectionsCommand;

    @Inject
    public RecommendedStationsSyncer(SyncStateStorage syncStateStorage,
                                     StationsApi api,
                                     WriteStationsRecommendationsCommand writeCollectionsCommand) {
        this.syncStateStorage = syncStateStorage;
        this.api = api;
        this.writeCollectionsCommand = writeCollectionsCommand;
    }

    @Override
    public Boolean call() throws Exception {
        ModelCollection<ApiStationMetadata> collection = api.fetchStationRecommendations();

        if (writeCollectionsCommand.call(collection)) {
            syncStateStorage.synced(StationsSyncInitiator.RECOMMENDATIONS);
            return true;
        } else {
            return false;
        }
    }
}
