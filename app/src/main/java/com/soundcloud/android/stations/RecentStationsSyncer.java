package com.soundcloud.android.stations;

import com.soundcloud.android.stations.WriteRecentStationsCollectionsCommand.SyncCollectionsMetadata;
import com.soundcloud.android.sync.SyncStateStorage;
import com.soundcloud.android.sync.Syncable;
import com.soundcloud.android.utils.CurrentDateProvider;
import com.soundcloud.android.utils.DateProvider;

import javax.inject.Inject;
import java.util.concurrent.Callable;

public class RecentStationsSyncer implements Callable<Boolean> {

    private final SyncStateStorage syncStateStorage;
    private final StationsApi api;
    private final WriteRecentStationsCollectionsCommand writeCollectionsCommand;
    private final DateProvider dateProvider;
    private final StationsStorage storage;

    @Inject
    public RecentStationsSyncer(SyncStateStorage syncStateStorage,
                                StationsApi api,
                                WriteRecentStationsCollectionsCommand writeCollectionsCommand,
                                CurrentDateProvider dateProvider,
                                StationsStorage storage) {
        this.syncStateStorage = syncStateStorage;
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

        if (writeCollectionsCommand.call(collections)) {
            syncStateStorage.synced(Syncable.RECENT_STATIONS);
            return true;
        } else {
            return false;
        }
    }
}
