package com.soundcloud.android.discovery;

import com.soundcloud.android.sync.Syncable;
import com.soundcloud.android.sync.SyncerRegistry;

import javax.inject.Inject;
import javax.inject.Provider;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

public class ChartsSyncProvider extends SyncerRegistry.SyncProvider {
    private final Provider<ChartsSyncer> chartsSyncerProvider;

    @Inject
    protected ChartsSyncProvider(Provider<ChartsSyncer> chartsSyncerProvider) {
        super(Syncable.CHARTS);
        this.chartsSyncerProvider = chartsSyncerProvider;
    }

    @Override
    public Callable<Boolean> syncer(String action, boolean isUiRequest) {
        return chartsSyncerProvider.get();
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
