package com.soundcloud.android.stations;

import com.soundcloud.android.sync.Syncable;
import com.soundcloud.android.sync.SyncerRegistry;

import javax.inject.Inject;
import javax.inject.Provider;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

public class RecommendedStationsSyncProvider extends SyncerRegistry.SyncProvider {

    private final Provider<RecommendedStationsSyncer> syncerProvider;

    @Inject
    public RecommendedStationsSyncProvider(Provider<RecommendedStationsSyncer> syncerProvider) {
        super(Syncable.RECOMMENDED_STATIONS);
        this.syncerProvider = syncerProvider;
    }

    @Override
    public Callable<Boolean> syncer(String action, boolean isUiRequest) {
        return syncerProvider.get();
    }

    @Override
    public Boolean isOutOfSync() {
        return false;
    }

    @Override
    public long staleTime() {
        return TimeUnit.DAYS.toMillis(1);
    }

    @Override
    public boolean usePeriodicSync() {
        return false;
    }
}
