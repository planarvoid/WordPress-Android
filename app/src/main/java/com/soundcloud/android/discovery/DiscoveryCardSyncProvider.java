package com.soundcloud.android.discovery;

import com.soundcloud.android.properties.FeatureFlags;
import com.soundcloud.android.properties.Flag;
import com.soundcloud.android.sync.Syncable;
import com.soundcloud.android.sync.SyncerRegistry;

import android.support.annotation.Nullable;

import javax.inject.Inject;
import javax.inject.Provider;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

public class DiscoveryCardSyncProvider extends SyncerRegistry.SyncProvider {

    private final FeatureFlags featureFlags;
    private final Provider<DiscoveryCardSyncer> discoveryCardSyncerProvider;

    @Inject
    DiscoveryCardSyncProvider(FeatureFlags featureFlags,
                              Provider<DiscoveryCardSyncer> discoveryCardSyncerProvider) {
        super(Syncable.DISCOVERY_CARDS);
        this.featureFlags = featureFlags;
        this.discoveryCardSyncerProvider = discoveryCardSyncerProvider;
    }

    @Override
    public Callable<Boolean> syncer(@Nullable String action, boolean isUiRequest) {
        return discoveryCardSyncerProvider.get();
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
        return featureFlags.isEnabled(Flag.DISCOVER_BACKEND);
    }
}