package com.soundcloud.android.sync;

import com.soundcloud.android.discovery.ChartsSyncProvider;
import com.soundcloud.android.discovery.RecommendedTracksSyncProvider;
import com.soundcloud.android.stations.RecentStationsSyncProvider;
import com.soundcloud.android.stations.RecommendedStationsSyncProvider;
import com.soundcloud.android.sync.likes.TrackLikesSyncProvider;

import javax.inject.Inject;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;

public class SyncerRegistry {

    private final Map<Syncable, SyncProvider> syncers;

    @Inject
    public SyncerRegistry(RecentStationsSyncProvider recentStationsSyncerProvider,
                          RecommendedStationsSyncProvider recommendedStationsSyncProvider,
                          RecommendedTracksSyncProvider recommendationsSyncProvider,
                          ChartsSyncProvider chartsSyncProvider,
                          TrackLikesSyncProvider trackLikesSyncProvider) {
        this.syncers = new HashMap<>();

        registerSyncer(recentStationsSyncerProvider);
        registerSyncer(recommendedStationsSyncProvider);
        registerSyncer(recommendationsSyncProvider);
        registerSyncer(chartsSyncProvider);
        registerSyncer(trackLikesSyncProvider);
    }

    public SyncProvider get(Syncable syncable) {
        return syncers.get(syncable);
    }

    private void registerSyncer(SyncProvider syncProvider) {
        syncers.put(syncProvider.syncable, syncProvider);
    }

    public static abstract class SyncProvider {
        private final Syncable syncable;

        protected SyncProvider(Syncable syncable) {
            this.syncable = syncable;
        }

        public String id() {
            return syncable.name();
        }

        public abstract Callable<Boolean> syncer();

        public abstract Boolean isOutOfSync();

        public abstract long staleTime();

        public abstract boolean usePeriodicSync();
    }
}
