package com.soundcloud.android.sync.me;

import com.soundcloud.android.sync.Syncable;
import com.soundcloud.android.sync.SyncerRegistry;

import android.support.annotation.Nullable;

import javax.inject.Inject;
import javax.inject.Provider;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

public class MeSyncerProvider extends SyncerRegistry.SyncProvider {

    private final Provider<MeSyncer> meSyncerProvider;

    @Inject
    public MeSyncerProvider(Provider<MeSyncer> meSyncerProvider) {
        super(Syncable.ME);
        this.meSyncerProvider = meSyncerProvider;
    }

    @Override
    public Callable<Boolean> syncer(@Nullable String action, boolean isUiRequest) {
        return meSyncerProvider.get();
    }

    @Override
    public Boolean isOutOfSync() {
        return false;
    }

    @Override
    public long staleTime() {
        return TimeUnit.HOURS.toMillis(24);
    }

    @Override
    public boolean usePeriodicSync() {
        return true;
    }
}
