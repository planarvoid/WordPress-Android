package com.soundcloud.android.stations;

import com.soundcloud.android.stations.WriteStationsCollectionsCommand.SyncCollectionsMetadata;
import com.soundcloud.android.utils.CurrentDateProvider;
import com.soundcloud.android.utils.DateProvider;

import javax.inject.Inject;
import java.util.concurrent.Callable;

class StationsSyncer implements Callable<Boolean> {
    private final StationsApi api;
    private final WriteStationsCollectionsCommand writeCollectionsCommand;
    private final DateProvider dateProvider;

    @Inject
    public StationsSyncer(StationsApi api,
                          WriteStationsCollectionsCommand writeCollectionsCommand,
                          CurrentDateProvider dateProvider) {
        this.api = api;
        this.writeCollectionsCommand = writeCollectionsCommand;
        this.dateProvider = dateProvider;
    }

    @Override
    public Boolean call() throws Exception {
        final long syncStartTime = dateProvider.getCurrentTime();
        final SyncCollectionsMetadata collections = new SyncCollectionsMetadata(
                syncStartTime,
                api.fetchStationsCollections());

        return writeCollectionsCommand.call(collections);
    }
}
