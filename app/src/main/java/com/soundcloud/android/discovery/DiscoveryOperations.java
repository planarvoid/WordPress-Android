package com.soundcloud.android.discovery;

import com.soundcloud.android.ApplicationModule;
import com.soundcloud.android.sync.NewSyncOperations;
import com.soundcloud.android.sync.SyncResult;
import com.soundcloud.android.sync.Syncable;
import com.soundcloud.android.view.ViewError;
import com.soundcloud.java.collections.Lists;
import io.reactivex.Scheduler;
import io.reactivex.Single;

import android.support.annotation.CheckResult;

import javax.inject.Inject;
import javax.inject.Named;

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

    @CheckResult
    Single<DiscoveryResult> discoveryCards() {
        return syncOperations.lazySyncIfStale(Syncable.DISCOVERY_CARDS)
                             .flatMap(this::cardsFromStorage)
                             .subscribeOn(scheduler);
    }

    @CheckResult
    Single<DiscoveryResult> refreshDiscoveryCards() {
        return syncOperations.sync(Syncable.DISCOVERY_CARDS)
                             .flatMap(this::cardsFromStorage)
                             .subscribeOn(scheduler);
    }

    private Single<DiscoveryResult> cardsFromStorage(SyncResult syncResult) {
        return storage.discoveryCards()
                      .toSingle(Lists.newArrayList(DiscoveryCard.EmptyCard.create(syncResult.throwable())))
                      .map(cards -> DiscoveryResult.create(cards, syncResult.throwable().transform(ViewError::from)));
    }
}
