package com.soundcloud.android.collection.recentlyplayed;

import static com.soundcloud.android.ApplicationModule.HIGH_PRIORITY;
import static com.soundcloud.android.rx.RxUtils.continueWith;

import com.soundcloud.android.sync.SyncOperations;
import com.soundcloud.android.sync.SyncOperations.Result;
import com.soundcloud.android.sync.Syncable;
import rx.Observable;
import rx.Scheduler;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.List;

public class RecentlyPlayedOperations {

    public static final int CAROUSEL_ITEMS = 10;
    static final int MAX_RECENTLY_PLAYED = 1000;

    private final Scheduler scheduler;
    private final SyncOperations syncOperations;
    private final RecentlyPlayedStorage recentlyPlayedStorage;
    private ClearRecentlyPlayedCommand clearRecentlyPlayedCommand;

    @Inject
    public RecentlyPlayedOperations(RecentlyPlayedStorage recentlyPlayedStorage,
                                    @Named(HIGH_PRIORITY) Scheduler scheduler,
                                    SyncOperations syncOperations,
                                    ClearRecentlyPlayedCommand clearRecentlyPlayedCommand) {
        this.recentlyPlayedStorage = recentlyPlayedStorage;
        this.scheduler = scheduler;
        this.syncOperations = syncOperations;
        this.clearRecentlyPlayedCommand = clearRecentlyPlayedCommand;
    }

    Observable<List<RecentlyPlayedPlayableItem>> recentlyPlayed() {
        return recentlyPlayed(MAX_RECENTLY_PLAYED);
    }

    public Observable<List<RecentlyPlayedPlayableItem>> recentlyPlayed(int limit) {
        return syncOperations.lazySyncIfStale(Syncable.RECENTLY_PLAYED)
                             .observeOn(scheduler)
                             .onErrorResumeNext(Observable.just(Result.NO_OP))
                             .flatMap(continueWith(recentlyPlayedItems(limit)));
    }

    Observable<List<RecentlyPlayedPlayableItem>> refreshRecentlyPlayed() {
        return refreshRecentlyPlayed(MAX_RECENTLY_PLAYED);
    }

    public Observable<List<RecentlyPlayedPlayableItem>> refreshRecentlyPlayed(int limit) {
        return syncOperations.failSafeSync(Syncable.RECENTLY_PLAYED)
                             .observeOn(scheduler)
                             .flatMap(continueWith(recentlyPlayedItems(limit)));
    }

    Observable<Boolean> clearHistory() {
        return clearRecentlyPlayedCommand.toObservable(null)
                                      .subscribeOn(scheduler);
    }

    private Observable<List<RecentlyPlayedPlayableItem>> recentlyPlayedItems(int limit) {
        return recentlyPlayedStorage.loadContexts(limit);
    }

}
