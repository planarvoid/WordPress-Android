package com.soundcloud.android.home;

import com.soundcloud.android.sync.Syncable;
import com.soundcloud.android.sync.SyncerRegistry;

import android.support.annotation.Nullable;

import javax.inject.Inject;
import javax.inject.Provider;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

public class HomeSyncProvider extends SyncerRegistry.SyncProvider {

    private final Provider<HomeSyncer> homeSyncerProvider;

    @Inject
    HomeSyncProvider(Provider<HomeSyncer> homeSyncerProvider) {
        super(Syncable.HOME);
        this.homeSyncerProvider = homeSyncerProvider;
    }

    @Override
    public Callable<Boolean> syncer(@Nullable String action, boolean isUiRequest) {
        return homeSyncerProvider.get();
    }

    @Override
    public Boolean isOutOfSync() {
        return false;
    }

    @Override
    public long staleTime() {
        return TimeUnit.HOURS.toMillis(12);
    }

    @Override
    public boolean usePeriodicSync() {
        return true;
    }
}
