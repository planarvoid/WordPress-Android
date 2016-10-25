package com.soundcloud.android.sync;

import com.soundcloud.android.activities.ActivitiesSyncProvider;
import com.soundcloud.android.associations.MyFollowingsSyncProvider;
import com.soundcloud.android.collection.playhistory.PlayHistorySyncProvider;
import com.soundcloud.android.collection.recentlyplayed.RecentlyPlayedSyncProvider;
import com.soundcloud.android.discovery.charts.ChartGenresSyncProvider;
import com.soundcloud.android.discovery.charts.ChartsSyncProvider;
import com.soundcloud.android.discovery.recommendations.RecommendedTracksSyncProvider;
import com.soundcloud.android.stations.LikedStationsSyncProvider;
import com.soundcloud.android.stations.RecommendedStationsSyncProvider;
import com.soundcloud.android.stream.SoundStreamSyncProvider;
import com.soundcloud.android.suggestedcreators.SuggestedCreatorsSyncProvider;
import com.soundcloud.android.sync.likes.PlaylistLikesSyncProvider;
import com.soundcloud.android.sync.likes.TrackLikesSyncProvider;
import com.soundcloud.android.sync.me.MeSyncerProvider;
import com.soundcloud.android.sync.playlists.MyPlaylistsSyncProvider;
import com.soundcloud.android.sync.posts.TrackPostsSyncProvider;

import android.support.annotation.Nullable;

import javax.inject.Inject;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;

public class SyncerRegistry {

    private final Map<Syncable, SyncProvider> syncers;

    @Inject
    public SyncerRegistry(SoundStreamSyncProvider soundStreamSyncProvider,
                          ActivitiesSyncProvider activitiesSyncProvider,
                          LikedStationsSyncProvider likedStationsSyncProvider,
                          RecommendedStationsSyncProvider recommendedStationsSyncProvider,
                          RecommendedTracksSyncProvider recommendationsSyncProvider,
                          ChartsSyncProvider chartsSyncProvider,
                          TrackPostsSyncProvider trackPostsSyncProvider,
                          TrackLikesSyncProvider trackLikesSyncProvider,
                          PlaylistLikesSyncProvider playlistLikesSyncProvider,
                          MyPlaylistsSyncProvider myPlaylistsSyncProvider,
                          MyFollowingsSyncProvider myFollowingsSyncProvider,
                          MeSyncerProvider meSyncerProvider,
                          ChartGenresSyncProvider chartGenresSyncProvider,
                          PlayHistorySyncProvider playHistorySyncProvider,
                          RecentlyPlayedSyncProvider recentlyPlayedSyncProvider,
                          SuggestedCreatorsSyncProvider suggestedCreatorsSyncProvider) {
        this.syncers = new HashMap<>();

        registerSyncer(soundStreamSyncProvider);
        registerSyncer(activitiesSyncProvider);
        registerSyncer(likedStationsSyncProvider);
        registerSyncer(recommendedStationsSyncProvider);
        registerSyncer(recommendationsSyncProvider);
        registerSyncer(chartsSyncProvider);
        registerSyncer(trackPostsSyncProvider);
        registerSyncer(trackLikesSyncProvider);
        registerSyncer(playlistLikesSyncProvider);
        registerSyncer(myPlaylistsSyncProvider);
        registerSyncer(myFollowingsSyncProvider);
        registerSyncer(meSyncerProvider);
        registerSyncer(chartGenresSyncProvider);
        registerSyncer(playHistorySyncProvider);
        registerSyncer(recentlyPlayedSyncProvider);
        registerSyncer(suggestedCreatorsSyncProvider);
    }

    @Nullable
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
         *
         * @param action a specific action for this sync. If a syncer can perform multiple types of syncs,
         *               this action specify that type. See {@link com.soundcloud.android.sync.stream.SoundStreamSyncer}
         * @param isUiRequest
         * @return The return value indicates that there were actual updates performed.
         * A return of true will reset the periodic sync time to the actual stale time
         * if periodic sync is used. A return of false will increase the delay until
         * the next sync, so we are not frequently syncing collections that do not change.
         */
        public abstract Callable<Boolean> syncer(@Nullable String action, boolean isUiRequest);

        /**
         * Tells the service whether a sync should be performed outside of the normal scheduling.
         * An example would be when we have some local state to push.
         */
        public abstract Boolean isOutOfSync();

        /**
         * Returns a value that tells the syncer how often the data should be synced.
         */
        public abstract long staleTime();

        /**
         * Makes this content eligible for background syncing.
         * It will be synced periodically by the value determined by staleTime(),
         * but will also sync more infrequently if the syncer indicates the
         * collection does not change.
         */
        public abstract boolean usePeriodicSync();
    }
}
