package com.soundcloud.android.home;

import com.soundcloud.android.ApplicationModule;
import com.soundcloud.android.sync.NewSyncOperations;
import com.soundcloud.android.sync.Syncable;
import com.soundcloud.java.collections.Lists;
import io.reactivex.Observable;
import io.reactivex.Scheduler;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.List;

class DiscoveryOperations {

    private final NewSyncOperations syncOperations;
    private final DiscoveryStorage storage;
    private final Scheduler scheduler;

    @Inject
    DiscoveryOperations(NewSyncOperations syncOperations,
                        DiscoveryStorage storage,
                        @Named(ApplicationModule.RX_HIGH_PRIORITY) Scheduler scheduler) {
        this.syncOperations = syncOperations;
        this.storage = storage;
        this.scheduler = scheduler;
    }

    Observable<List<DiscoveryCard>> discoveryCards() {
        return syncOperations.lazySyncIfStale(Syncable.DISCOVERY_CARDS)
                             .flatMap(read -> storage.discoveryCards())
                             .map((apiModel) -> Lists.transform(apiModel, DiscoveryCardMapper::map))
                             .subscribeOn(scheduler);
    }

    Observable<List<DiscoveryCard>> refreshDiscoveryCards() {
        return syncOperations.failSafeSync(Syncable.DISCOVERY_CARDS)
                             .flatMap(read -> storage.discoveryCards())
                             .map((apiModel) -> Lists.transform(apiModel, DiscoveryCardMapper::map))
                             .subscribeOn(scheduler);
    }
}
