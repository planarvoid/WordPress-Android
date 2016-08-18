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
    private static final int MAX_RECENTLY_PLAYED = 500;

    private final Scheduler scheduler;
    private final SyncOperations syncOperations;
    private final RecentlyPlayedStorage recentlyPlayedStorage;

    @Inject
    public RecentlyPlayedOperations(RecentlyPlayedStorage recentlyPlayedStorage,
                                    @Named(HIGH_PRIORITY) Scheduler scheduler,
                                    SyncOperations syncOperations) {
        this.recentlyPlayedStorage = recentlyPlayedStorage;
        this.scheduler = scheduler;
        this.syncOperations = syncOperations;
    }

    public Observable<List<RecentlyPlayedItem>> recentlyPlayed() {
        return recentlyPlayed(MAX_RECENTLY_PLAYED);
    }

    public Observable<List<RecentlyPlayedItem>> recentlyPlayed(int limit) {
        return syncOperations.lazySyncIfStale(Syncable.RECENTLY_PLAYED)
                             .observeOn(scheduler)
                             .onErrorResumeNext(Observable.just(Result.NO_OP))
                             .flatMap(continueWith(recentlyPlayedItems(limit)));
    }

    public Observable<List<RecentlyPlayedItem>> refreshRecentlyPlayed() {
        return refreshRecentlyPlayed(MAX_RECENTLY_PLAYED);
    }

    public Observable<List<RecentlyPlayedItem>> refreshRecentlyPlayed(int limit) {
        return syncOperations.sync(Syncable.RECENTLY_PLAYED)
                             .observeOn(scheduler)
                             .onErrorResumeNext(Observable.just(Result.NO_OP))
                             .flatMap(continueWith(recentlyPlayedItems(limit)));
    }

    private Observable<List<RecentlyPlayedItem>> recentlyPlayedItems(int limit) {
        return recentlyPlayedStorage.loadContexts(limit).toList();
    }

}
