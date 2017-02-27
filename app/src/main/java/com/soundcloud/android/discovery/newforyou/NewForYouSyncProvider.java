package com.soundcloud.android.discovery.newforyou;

import com.soundcloud.android.sync.Syncable;
import com.soundcloud.android.sync.SyncerRegistry;

import android.support.annotation.Nullable;

import javax.inject.Inject;
import javax.inject.Provider;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

public class NewForYouSyncProvider extends SyncerRegistry.SyncProvider {

    private final Provider<NewForYouSyncer> newForYouSyncerProvider;

    @Inject
    public NewForYouSyncProvider(Provider<NewForYouSyncer> newForYouSyncerProvider) {
        super(Syncable.NEW_FOR_YOU);
        this.newForYouSyncerProvider = newForYouSyncerProvider;
    }

    @Override
    public Callable<Boolean> syncer(@Nullable String action, boolean isUiRequest) {
        return newForYouSyncerProvider.get();
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
