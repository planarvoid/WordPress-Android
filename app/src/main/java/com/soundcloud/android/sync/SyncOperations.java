package com.soundcloud.android.sync;

import static com.soundcloud.android.rx.RxUtils.returning;
import static com.soundcloud.android.rx.observers.DefaultSubscriber.fireAndForget;
import static rx.Observable.just;

import rx.Observable;

import javax.inject.Inject;

public class SyncOperations {

    private final SyncInitiator syncInitiator;
    private final SyncStateStorage syncStateStorage;
    private final SyncerRegistry syncerRegistry;

    @Inject
    public SyncOperations(SyncInitiator syncInitiator,
                          SyncStateStorage syncStateStorage,
                          SyncerRegistry syncerRegistry) {
        this.syncInitiator = syncInitiator;
        this.syncStateStorage = syncStateStorage;
        this.syncerRegistry = syncerRegistry;
    }

    public Observable<Result> sync(Syncable syncable) {
        return syncInitiator.sync(syncable).map(returning(Result.SYNCED));
    }

    public Observable<Result> failSafeSync(Syncable syncable) {
        return sync(syncable).onErrorResumeNext(Observable.just(Result.ERROR));
    }

    Observable<Result> syncIfStale(Syncable syncable) {
        if (isContentStale(syncable)) {
            return just(Result.NO_OP);
        } else {
            return sync(syncable);
        }
    }

    public Observable<Result> lazySyncIfStale(Syncable syncable) {
        if (syncStateStorage.hasSyncedBefore(syncable)) {
            if (!isContentStale(syncable)) {
                fireAndForget(syncInitiator.sync(syncable));
                return just(Result.SYNCING);
            } else {
                return just(Result.NO_OP);
            }
        } else {
            return sync(syncable);
        }
    }

    private boolean isContentStale(Syncable syncable) {
        return syncStateStorage.hasSyncedWithin(syncable, syncerRegistry.get(syncable).staleTime());
    }

    public enum Result {
        SYNCED, SYNCING, NO_OP, ERROR
    }
}
