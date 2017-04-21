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

class HomeOperations {

    private final NewSyncOperations syncOperations;
    private final HomeStorage storage;
    private final Scheduler scheduler;

    @Inject
    HomeOperations(NewSyncOperations syncOperations,
                   HomeStorage storage,
                   @Named(ApplicationModule.RX_HIGH_PRIORITY) Scheduler scheduler) {
        this.syncOperations = syncOperations;
        this.storage = storage;
        this.scheduler = scheduler;
    }

    Observable<List<HomeCard>> homeCards() {
        return syncOperations.lazySyncIfStale(Syncable.HOME)
                             .flatMap(read -> storage.homeCards())
                             .map((apiModel) -> Lists.transform(apiModel, HomeMapper::map))
                             .subscribeOn(scheduler);
    }

    Observable<List<HomeCard>> refreshHomeCards() {
        return syncOperations.failSafeSync(Syncable.HOME)
                             .flatMap(read -> storage.homeCards())
                             .map((apiModel) -> Lists.transform(apiModel, HomeMapper::map))
                             .subscribeOn(scheduler);
    }
}
