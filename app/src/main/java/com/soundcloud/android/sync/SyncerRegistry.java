package com.soundcloud.android.sync;

import com.soundcloud.android.stations.RecentStationsSyncer;
import com.soundcloud.android.stations.RecommendedStationsSyncer;
import com.soundcloud.android.sync.charts.ChartsSyncer;
import com.soundcloud.android.sync.recommendations.RecommendationsSyncer;

import javax.inject.Inject;
import javax.inject.Provider;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

public class SyncerRegistry {

    private static final long ONE_DAY = TimeUnit.DAYS.toMillis(1);
    private final Map<Syncable,SyncData> syncers;

    @Inject
    public SyncerRegistry(Provider<RecentStationsSyncer> recentStationsSyncerProvider,
                          Provider<RecommendedStationsSyncer> recommendedStationsSyncerProvider,
                          Provider<RecommendationsSyncer> recommendationsSyncerProvider,
                          Provider<ChartsSyncer> chartsSyncerProvider) {
        this.syncers = new HashMap<>();

        registerSyncer(Syncable.RECENT_STATIONS, recentStationsSyncerProvider, ONE_DAY);
        registerSyncer(Syncable.RECOMMENDED_STATIONS, recommendedStationsSyncerProvider, ONE_DAY);
        registerSyncer(Syncable.RECOMMENDED_TRACKS, recommendationsSyncerProvider, ONE_DAY);
        registerSyncer(Syncable.CHARTS, chartsSyncerProvider, ONE_DAY);
    }

    public SyncData get(Syncable syncable) {
        return syncers.get(syncable);
    }

    private void registerSyncer(Syncable syncable,
                                Provider<? extends Callable<Boolean>> syncerProvider,
                                long staleTime) {
        syncers.put(syncable, new SyncData(syncable.name(), syncerProvider, staleTime));
    }

    static class SyncData {

        final String id;
        final Provider<? extends Callable<Boolean>> syncer;
        final long staleTime;

        SyncData(String id, Provider<? extends Callable<Boolean>> syncer, long staleTime) {
            this.id = id;
            this.syncer = syncer;
            this.staleTime = staleTime;
        }
    }
}
