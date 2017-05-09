package com.soundcloud.android.olddiscovery.newforyou;

import com.soundcloud.android.ApplicationModule;
import com.soundcloud.android.rx.RxJava;
import com.soundcloud.android.sync.SyncOperations;
import com.soundcloud.android.sync.Syncable;
import io.reactivex.Scheduler;
import io.reactivex.Single;

import javax.inject.Inject;
import javax.inject.Named;

public class NewForYouOperations {

    private final SyncOperations syncOperations;
    private final NewForYouStorage storage;
    private final Scheduler scheduler;

    @Inject
    NewForYouOperations(SyncOperations syncOperations,
                        NewForYouStorage storage,
                        @Named(ApplicationModule.RX_HIGH_PRIORITY) Scheduler scheduler) {
        this.syncOperations = syncOperations;
        this.storage = storage;
        this.scheduler = scheduler;
    }

    public Single<NewForYou> newForYou() {
        return RxJava.toV2Single(syncOperations.lazySyncIfStale(Syncable.NEW_FOR_YOU).flatMap(read -> storage.newForYou()))
                     .subscribeOn(scheduler);
    }

    public Single<NewForYou> refreshNewForYou() {
        return RxJava.toV2Single(syncOperations.failSafeSync(Syncable.NEW_FOR_YOU).flatMap(read -> storage.newForYou()))
                     .subscribeOn(scheduler);
    }
}
