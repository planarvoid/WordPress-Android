package com.soundcloud.android.sync;

import com.soundcloud.android.discovery.ChartGenresSyncProvider;
import com.soundcloud.android.discovery.ChartsSyncProvider;
import com.soundcloud.android.discovery.RecommendedTracksSyncProvider;
import com.soundcloud.android.stations.RecentStationsSyncProvider;
import com.soundcloud.android.stations.RecommendedStationsSyncProvider;
import com.soundcloud.android.sync.likes.PlaylistLikesSyncProvider;
import com.soundcloud.android.sync.likes.TrackLikesSyncProvider;
import com.soundcloud.android.sync.posts.TrackPostsSyncProvider;

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
                          TrackPostsSyncProvider trackPostsSyncProvider,
                          TrackLikesSyncProvider trackLikesSyncProvider,
                          PlaylistLikesSyncProvider playlistLikesSyncProvider,
                          ChartGenresSyncProvider chartGenresSyncProvider) {
        this.syncers = new HashMap<>();

        registerSyncer(recentStationsSyncerProvider);
        registerSyncer(recommendedStationsSyncProvider);
        registerSyncer(recommendationsSyncProvider);
        registerSyncer(chartsSyncProvider);
        registerSyncer(trackPostsSyncProvider);
        registerSyncer(trackLikesSyncProvider);
        registerSyncer(playlistLikesSyncProvider);
        registerSyncer(chartGenresSyncProvider);
    }

    public SyncProvider get(Syncable syncable) {
        return syncers.get(syncable);
    }

    private void registerSyncer(SyncProvider syncProvider) {
        syncers.put(syncProvider.syncable, syncProvider);
    }

    /**
     * A SyncProvider is an abstraction consisting of setup methods
     * for the data syncing job.
     */
    public static abstract class SyncProvider {
        private final Syncable syncable;

        protected SyncProvider(Syncable syncable) {
            this.syncable = syncable;
        }

        public String id() {
            return syncable.name();
        }

        /**
         * Provides a {@link Callable} that will perform the syncing task.
         */
        public abstract Callable<Boolean> syncer();

        /**
         * Provides a value that tells the syncing service when the data
         * is out of synchronization in order to schedule a sync job.
         */
        public abstract Boolean isOutOfSync();

        /**
         * Returns a value that tells the syncer how often the data should be synced.
         * It has no effect if {@link SyncProvider#usePeriodicSync()} is False.
         */
        public abstract long staleTime();

        /**
         * Provides a value that tells the syncing service whether should
         * use periodic sync schedules: syncing time configuration is
         * returned by {@link SyncProvider#staleTime()} method.
         */
        public abstract boolean usePeriodicSync();
    }
}
