package com.soundcloud.android.collection.recentlyplayed;

import com.soundcloud.android.sync.Syncable;
import com.soundcloud.android.sync.SyncerRegistry;

import javax.inject.Inject;
import javax.inject.Provider;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;


public class RecentlyPlayedSyncProvider extends SyncerRegistry.SyncProvider {
    private final Provider<RecentlyPlayedSyncer> recentlyPlayedSyncerProvider;
    private final RecentlyPlayedStorage recentlyPlayedStorage;

    @Inject
    protected RecentlyPlayedSyncProvider(Provider<RecentlyPlayedSyncer> recentlyPlayedSyncerProvider,
                                         RecentlyPlayedStorage recentlyPlayedStorage) {
        super(Syncable.RECENTLY_PLAYED);
        this.recentlyPlayedSyncerProvider = recentlyPlayedSyncerProvider;
        this.recentlyPlayedStorage = recentlyPlayedStorage;
    }

    @Override
    public Callable<Boolean> syncer(String action, boolean isUiRequest) {
        return recentlyPlayedSyncerProvider.get();
    }

    @Override
    public Boolean isOutOfSync() {
        return recentlyPlayedStorage.hasPendingContextsToSync();
    }

    @Override
    public long staleTime() {
        return TimeUnit.MINUTES.toMillis(30);
    }

    @Override
    public boolean usePeriodicSync() {
        return false;
    }
}
