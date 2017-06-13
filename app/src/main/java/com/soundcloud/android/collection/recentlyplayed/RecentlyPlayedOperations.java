package com.soundcloud.android.collection.recentlyplayed;

import static com.soundcloud.android.ApplicationModule.RX_HIGH_PRIORITY;

import com.soundcloud.android.rx.RxJava;
import com.soundcloud.android.sync.NewSyncOperations;
import com.soundcloud.android.sync.SyncResult;
import com.soundcloud.android.sync.Syncable;
import io.reactivex.Observable;
import io.reactivex.Scheduler;
import io.reactivex.Single;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.List;

public class RecentlyPlayedOperations {

    public static final int CAROUSEL_ITEMS = 10;
    static final int MAX_RECENTLY_PLAYED = 1000;

    private final Scheduler scheduler;
    private final NewSyncOperations syncOperations;
    private final RecentlyPlayedStorage recentlyPlayedStorage;
    private ClearRecentlyPlayedCommand clearRecentlyPlayedCommand;

    @Inject
    public RecentlyPlayedOperations(RecentlyPlayedStorage recentlyPlayedStorage,
                                    @Named(RX_HIGH_PRIORITY) Scheduler scheduler,
                                    NewSyncOperations syncOperations,
                                    ClearRecentlyPlayedCommand clearRecentlyPlayedCommand) {
        this.recentlyPlayedStorage = recentlyPlayedStorage;
        this.scheduler = scheduler;
        this.syncOperations = syncOperations;
        this.clearRecentlyPlayedCommand = clearRecentlyPlayedCommand;
    }

    Single<List<RecentlyPlayedPlayableItem>> recentlyPlayed() {
        return recentlyPlayed(MAX_RECENTLY_PLAYED);
    }

    public Single<List<RecentlyPlayedPlayableItem>> recentlyPlayed(int limit) {
        return syncOperations.lazySyncIfStale(Syncable.RECENTLY_PLAYED)
                             .observeOn(scheduler)
                             .onErrorResumeNext(Single.just(SyncResult.noOp()))
                             .flatMap(__ -> recentlyPlayedItems(limit));
    }

    Single<List<RecentlyPlayedPlayableItem>> refreshRecentlyPlayed() {
        return refreshRecentlyPlayed(MAX_RECENTLY_PLAYED);
    }

    public Single<List<RecentlyPlayedPlayableItem>> refreshRecentlyPlayed(int limit) {
        return syncOperations.failSafeSync(Syncable.RECENTLY_PLAYED)
                             .observeOn(scheduler)
                             .flatMap(__ -> recentlyPlayedItems(limit));
    }

    Observable<Boolean> clearHistory() {
        return RxJava.toV2Observable(clearRecentlyPlayedCommand.toObservable(null))
                                                .subscribeOn(scheduler);
    }

    private Single<List<RecentlyPlayedPlayableItem>> recentlyPlayedItems(int limit) {
        return recentlyPlayedStorage.loadContexts(limit);
    }

}
