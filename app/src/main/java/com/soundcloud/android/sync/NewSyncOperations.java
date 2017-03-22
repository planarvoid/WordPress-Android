package com.soundcloud.android.sync;

import static com.soundcloud.android.rx.observers.DefaultDisposableCompletableObserver.fireAndForget;
import static io.reactivex.Observable.just;

import io.reactivex.Observable;

import javax.inject.Inject;

public class NewSyncOperations {

    private final SyncInitiator syncInitiator;
    private final SyncStateStorage syncStateStorage;
    private final SyncerRegistry syncerRegistry;

    @Inject
    public NewSyncOperations(SyncInitiator syncInitiator,
                             SyncStateStorage syncStateStorage,
                             SyncerRegistry syncerRegistry) {
        this.syncInitiator = syncInitiator;
        this.syncStateStorage = syncStateStorage;
        this.syncerRegistry = syncerRegistry;
    }

    public static <T> Observable<T> emptyResult(Result result) {
        return result == Result.ERROR ?
                Observable.error(new SyncFailedException()) :
                Observable.empty();
    }

    public Observable<Result> sync(Syncable syncable) {
        return syncInitiator.synchronise(syncable).map(o -> Result.SYNCED);
    }

    public Observable<Result> failSafeSync(Syncable syncable) {
        return sync(syncable).onErrorResumeNext(just(Result.ERROR));
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
                fireAndForget(syncInitiator.synchronise(syncable));
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
