package com.soundcloud.android.discovery;

import com.soundcloud.android.ApplicationModule;
import com.soundcloud.android.sync.NewSyncOperations;
import com.soundcloud.android.sync.SyncResult;
import com.soundcloud.android.sync.Syncable;
import com.soundcloud.android.view.ViewError;
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

    Single<DiscoveryResult> discoveryCards() {
        return syncOperations.lazySyncIfStale(Syncable.DISCOVERY_CARDS)
                             .flatMap(this::cardsFromStorage)
                             .flatMap(this::mitigateDatabaseUpgradeIssue)
                             .subscribeOn(scheduler);
    }

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

    /*
     * It's possible that that database has been wiped during a migration but the syncer thinks we should have valid and recently synced data.
     *
     * We can mitigate this by doing a full sync if the lazy sync was successful but there are no cards in the database.
     */
    private Single<DiscoveryResult> mitigateDatabaseUpgradeIssue(DiscoveryResult discoveryResult) {
        if (!discoveryResult.syncError().isPresent() && hasEmptyCard(discoveryResult.cards())) {
            return refreshDiscoveryCards();
        } else {
            return Single.just(discoveryResult);
        }
    }

    private boolean hasEmptyCard(List<DiscoveryCard> discoveryCards) {
        if (discoveryCards.isEmpty()) {
            return true;
        }
        for (DiscoveryCard discoveryCard : discoveryCards) {
            if (discoveryCard.isEmpty()) {
                return true;
            }
        }
        return false;
    }
}
