package com.soundcloud.android.discovery.recommendedplaylists;

import com.soundcloud.android.sync.Syncable;
import com.soundcloud.android.sync.SyncerRegistry;

import javax.inject.Inject;
import javax.inject.Provider;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

public class RecommendedPlaylistsSyncProvider extends SyncerRegistry.SyncProvider {
    private final Provider<RecommendPlaylistsSyncer> recommendedPlaylistSyncerProvider;

    @Inject
    RecommendedPlaylistsSyncProvider(Provider<RecommendPlaylistsSyncer> recommendedPlaylistSyncerProvider) {
        super(Syncable.RECOMMENDED_PLAYLISTS);
        this.recommendedPlaylistSyncerProvider = recommendedPlaylistSyncerProvider;
    }

    @Override
    public Callable<Boolean> syncer(String action, boolean isUiRequest) {
        return recommendedPlaylistSyncerProvider.get();
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
