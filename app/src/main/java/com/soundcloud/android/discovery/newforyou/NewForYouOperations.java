package com.soundcloud.android.discovery.newforyou;

import com.soundcloud.android.sync.SyncOperations;
import com.soundcloud.android.sync.Syncable;
import rx.Observable;

import javax.inject.Inject;

public class NewForYouOperations {

    private final SyncOperations syncOperations;
    private final NewForYouStorage storage;

    @Inject
    NewForYouOperations(SyncOperations syncOperations,
                        NewForYouStorage storage) {
        this.syncOperations = syncOperations;
        this.storage = storage;
    }

    public Observable<NewForYou> newForYou() {
        return syncOperations.lazySyncIfStale(Syncable.NEW_FOR_YOU)
                             .flatMap(read -> storage.newForYou());
    }

    public Observable<NewForYou> refreshNewForYou() {
        return syncOperations.failSafeSync(Syncable.NEW_FOR_YOU)
                             .flatMap(read -> storage.newForYou());
    }
}
