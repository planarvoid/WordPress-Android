package com.soundcloud.android.collection.playhistory;

import com.soundcloud.android.sync.Syncable;
import com.soundcloud.android.sync.SyncerRegistry;

import javax.inject.Inject;
import javax.inject.Provider;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;


public class PlayHistorySyncProvider extends SyncerRegistry.SyncProvider {
    private final Provider<PlayHistorySyncer> playHistorySyncerProvider;
    private final PlayHistoryStorage playHistoryStorage;

    @Inject
    protected PlayHistorySyncProvider(Provider<PlayHistorySyncer> playHistorySyncerProvider,
                                      PlayHistoryStorage playHistoryStorage) {
        super(Syncable.PLAY_HISTORY);
        this.playHistorySyncerProvider = playHistorySyncerProvider;
        this.playHistoryStorage = playHistoryStorage;
    }

    @Override
    public Callable<Boolean> syncer(String action, boolean isUiRequest) {
        return playHistorySyncerProvider.get();
    }

    @Override
    public Boolean isOutOfSync() {
        return playHistoryStorage.hasPendingTracksToSync();
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
