package com.soundcloud.android.stations;

import javax.inject.Inject;
import java.util.concurrent.Callable;

public class RecommendedStationsSyncer implements Callable<Boolean> {

    private final StationsApi api;
    private final WriteStationsRecommendationsCommand writeCollectionsCommand;

    @Inject
    public RecommendedStationsSyncer(StationsApi api,
                                     WriteStationsRecommendationsCommand writeCollectionsCommand) {
        this.api = api;
        this.writeCollectionsCommand = writeCollectionsCommand;
    }

    @Override
    public Boolean call() throws Exception {
        return writeCollectionsCommand.call(api.fetchStationRecommendations());
    }
}
