package com.soundcloud.android.activities;

import com.soundcloud.android.sync.Syncable;
import com.soundcloud.android.sync.SyncerRegistry;
import com.soundcloud.android.sync.activities.ActivitiesSyncer.ActivitiesSyncerFactory;

import javax.inject.Inject;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

public class ActivitiesSyncProvider extends SyncerRegistry.SyncProvider {
    private final ActivitiesSyncerFactory syncerFactory;

    @Inject
    public ActivitiesSyncProvider(ActivitiesSyncerFactory syncerFactory) {
        super(Syncable.ACTIVITIES);
        this.syncerFactory = syncerFactory;
    }

    @Override
    public Callable<Boolean> syncer(String action, boolean isUiRequest) {
        return syncerFactory.create(action);
    }

    @Override
    public Boolean isOutOfSync() {
        return false;
    }

    @Override
    public long staleTime() {
        return TimeUnit.MINUTES.toMillis(10);
    }

    @Override
    public boolean usePeriodicSync() {
        return false;
    }
}
