package com.soundcloud.android.discovery;

import com.soundcloud.android.sync.Syncable;
import com.soundcloud.android.sync.SyncerRegistry;

import javax.inject.Inject;
import javax.inject.Provider;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

public class ChartGenresSyncProvider extends SyncerRegistry.SyncProvider {
    private final Provider<ChartGenresSyncer> genresSyncerProvider;

    @Inject
    protected ChartGenresSyncProvider(Provider<ChartGenresSyncer> genresSyncerProvider) {
        super(Syncable.CHART_GENRES);
        this.genresSyncerProvider = genresSyncerProvider;
    }

    @Override
    public Callable<Boolean> syncer(String action, boolean isUiRequest) {
        return genresSyncerProvider.get();
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
