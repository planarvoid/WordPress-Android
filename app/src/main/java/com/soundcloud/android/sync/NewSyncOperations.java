package com.soundcloud.android.sync;

import static com.soundcloud.android.rx.observers.DefaultDisposableCompletableObserver.fireAndForget;

import io.reactivex.Maybe;
import io.reactivex.Single;

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

    public static <T> Maybe<T> emptyResult(SyncResult result) {
        return result.isError() ?
               Maybe.error(new SyncFailedException()) :
               Maybe.empty();
    }

    public Single<SyncResult> sync(Syncable syncable) {
        return syncInitiator.sync(syncable)
                            .map(o -> SyncResult.synced())
                            .onErrorReturn(SyncResult::error);
    }

    public Single<SyncResult> failSafeSync(Syncable syncable) {
        return sync(syncable)
                .onErrorResumeNext(throwable -> Single.just(SyncResult.error(throwable)));
    }

    public Single<SyncResult> lazySyncIfStale(Syncable syncable) {
        if (syncStateStorage.hasSyncedBefore(syncable)) {
            if (isContentFresh(syncable)) {
                return Single.just(SyncResult.noOp());
            } else {
                fireAndForget(syncInitiator.sync(syncable).toObservable());
                return Single.just(SyncResult.syncing());
            }
        } else {
            return sync(syncable);
        }
    }

    public Single<SyncResult> syncIfStale(Syncable syncable) {
        if (syncStateStorage.hasSyncedBefore(syncable) && isContentFresh(syncable)) {
            return Single.just(SyncResult.noOp());
        } else {
            return sync(syncable);
        }
    }

    private boolean isContentFresh(Syncable syncable) {
        return syncStateStorage.hasSyncedWithin(syncable, syncerRegistry.get(syncable).staleTime());
    }
}
