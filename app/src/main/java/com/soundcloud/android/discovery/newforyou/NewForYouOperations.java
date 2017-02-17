package com.soundcloud.android.discovery.newforyou;

import com.soundcloud.android.ApplicationModule;
import com.soundcloud.android.sync.SyncOperations;
import com.soundcloud.android.sync.Syncable;
import rx.Observable;
import rx.Scheduler;

import javax.inject.Inject;
import javax.inject.Named;

public class NewForYouOperations {

    private final SyncOperations syncOperations;
    private final NewForYouStorage storage;
    private final Scheduler scheduler;

    @Inject
    NewForYouOperations(SyncOperations syncOperations,
                        NewForYouStorage storage,
                        @Named(ApplicationModule.HIGH_PRIORITY) Scheduler scheduler) {
        this.syncOperations = syncOperations;
        this.storage = storage;
        this.scheduler = scheduler;
    }

    public Observable<NewForYou> newForYou() {
        return syncOperations.lazySyncIfStale(Syncable.NEW_FOR_YOU)
                             .flatMap(read -> storage.newForYou())
                             .subscribeOn(scheduler);
    }

    public Observable<NewForYou> refreshNewForYou() {
        return syncOperations.failSafeSync(Syncable.NEW_FOR_YOU)
                             .flatMap(read -> storage.newForYou())
                             .subscribeOn(scheduler);
    }
}
