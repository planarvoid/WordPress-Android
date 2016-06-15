package com.soundcloud.android.sync;

import com.soundcloud.android.stations.RecentStationsSyncer;
import com.soundcloud.android.stations.RecommendedStationsSyncer;
import com.soundcloud.android.sync.charts.ChartsSyncer;

import javax.inject.Inject;
import javax.inject.Provider;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;

public class SyncerRegistry {

    private final Map<Syncable,SyncData> syncers;

    @Inject
    public SyncerRegistry(Provider<RecentStationsSyncer> recentStationsSyncerProvider,
                          Provider<RecommendedStationsSyncer> recommendedStationsSyncerProvider,
                          Provider<ChartsSyncer> chartsSyncerProvider) {
        this.syncers = new HashMap<>();

        registerSyncer(Syncable.RECENT_STATIONS, recentStationsSyncerProvider);
        registerSyncer(Syncable.RECOMMENDED_STATIONS, recommendedStationsSyncerProvider);
        registerSyncer(Syncable.CHARTS, chartsSyncerProvider);
    }

    public SyncData get(Syncable syncable) {
        return syncers.get(syncable);
    }

    private void registerSyncer(Syncable syncable, Provider<? extends Callable<Boolean>> syncerProvider) {
        syncers.put(syncable, new SyncData(syncable.name(), syncerProvider));
    }

    static class SyncData {

        final String id;
        final Provider<? extends Callable<Boolean>> syncer;

        SyncData(String id, Provider<? extends Callable<Boolean>> syncer) {
            this.id = id;
            this.syncer = syncer;
        }
    }
}
