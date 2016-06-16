package com.soundcloud.android.stations;

import com.soundcloud.android.sync.Syncable;
import com.soundcloud.android.sync.SyncerRegistry;

import javax.inject.Inject;
import javax.inject.Provider;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

public class RecentStationsSyncProvider extends SyncerRegistry.SyncProvider {
    private final Provider<RecentStationsSyncer> syncerProvider;
    private final StationsStorage stationsStorage;

    @Inject
    public RecentStationsSyncProvider(Provider<RecentStationsSyncer> syncerProvider, StationsStorage stationsStorage) {
        super(Syncable.RECENT_STATIONS);
        this.syncerProvider = syncerProvider;
        this.stationsStorage = stationsStorage;
    }

    @Override
    public Callable<Boolean> syncer() {
        return syncerProvider.get();
    }

    @Override
    public Boolean isOutOfSync() {
        return !stationsStorage.getRecentStationsToSync().isEmpty();
    }

    @Override
    public long staleTime() {
        return TimeUnit.HOURS.toMillis(24);
    }

    @Override
    public boolean usePeriodicSync() {
        return false;
    }
}
