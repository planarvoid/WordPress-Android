package com.soundcloud.android.discovery;

import com.soundcloud.android.ApplicationModule;
import com.soundcloud.android.sync.NewSyncOperations;
import com.soundcloud.android.sync.Syncable;
import com.soundcloud.java.collections.Lists;
import io.reactivex.Scheduler;
import io.reactivex.Single;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.List;

class DiscoveryOperations {

    private final NewSyncOperations syncOperations;
    private final DiscoveryReadableStorage storage;
    private final Scheduler scheduler;

    @Inject
    DiscoveryOperations(NewSyncOperations syncOperations,
                        DiscoveryReadableStorage storage,
                        @Named(ApplicationModule.RX_HIGH_PRIORITY) Scheduler scheduler) {
        this.syncOperations = syncOperations;
        this.storage = storage;
        this.scheduler = scheduler;
    }

    Single<List<DiscoveryCard>> discoveryCards() {
        return syncOperations.lazySyncIfStale(Syncable.DISCOVERY_CARDS)
                             .flatMap(syncResult -> storage.discoveryCards().toSingle(Lists.newArrayList(DiscoveryCard.EmptyCard.create(syncResult.throwable()))))
                             .subscribeOn(scheduler);
    }

    Single<List<DiscoveryCard>> refreshDiscoveryCards() {
        return syncOperations.sync(Syncable.DISCOVERY_CARDS)
                             .flatMap(syncResult -> storage.discoveryCards().toSingle(Lists.newArrayList(DiscoveryCard.EmptyCard.create(syncResult.throwable()))))
                             .subscribeOn(scheduler);
    }
}
