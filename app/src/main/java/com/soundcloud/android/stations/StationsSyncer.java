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
    private final StationsStorage storage;

    @Inject
    public StationsSyncer(StationsApi api,
                          WriteStationsCollectionsCommand writeCollectionsCommand,
                          CurrentDateProvider dateProvider,
                          StationsStorage storage) {
        this.api = api;
        this.writeCollectionsCommand = writeCollectionsCommand;
        this.dateProvider = dateProvider;
        this.storage = storage;
    }

    @Override
    public Boolean call() throws Exception {
        final long syncStartTime = dateProvider.getCurrentTime();
        final SyncCollectionsMetadata collections = new SyncCollectionsMetadata(
                syncStartTime,
                api.syncStationsCollections(storage.getRecentStationsToSync()));

        return writeCollectionsCommand.call(collections);
    }
}
