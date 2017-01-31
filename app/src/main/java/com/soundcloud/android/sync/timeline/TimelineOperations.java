package com.soundcloud.android.sync.timeline;

import static com.soundcloud.android.rx.RxUtils.IS_TRUE;

import com.soundcloud.android.ApplicationModule;
import com.soundcloud.android.Consts;
import com.soundcloud.android.sync.ApiSyncService;
import com.soundcloud.android.sync.SyncInitiator;
import com.soundcloud.android.sync.SyncJobResult;
import com.soundcloud.android.sync.SyncStateStorage;
import com.soundcloud.android.sync.Syncable;
import com.soundcloud.android.utils.Log;
import com.soundcloud.java.optional.Optional;
import com.soundcloud.rx.Pager;
import rx.Observable;
import rx.Scheduler;

import android.support.annotation.VisibleForTesting;

import javax.inject.Named;
import java.util.Collections;
import java.util.Date;
import java.util.List;

public abstract class TimelineOperations<ViewModel, StorageModel> {

    private static final long INITIAL_TIMESTAMP = Long.MAX_VALUE;
    private static final String TAG = "Timeline";

    @VisibleForTesting
    static final int PAGE_SIZE = Consts.LIST_PAGE_SIZE;

    private final Syncable syncable;
    private final TimelineStorage<StorageModel> storage;
    private final SyncInitiator syncInitiator;
    private final Scheduler scheduler;
    private final SyncStateStorage syncStateStorage;
    private final List<ViewModel> noMorePagesSentinel = Collections.emptyList();

    public TimelineOperations(Syncable syncable,
                              TimelineStorage<StorageModel> storage,
                              SyncInitiator syncInitiator,
                              @Named(ApplicationModule.HIGH_PRIORITY) Scheduler scheduler,
                              SyncStateStorage syncStateStorage) {
        this.syncable = syncable;
        this.storage = storage;
        this.syncInitiator = syncInitiator;
        this.scheduler = scheduler;
        this.syncStateStorage = syncStateStorage;
    }

    protected Observable<List<ViewModel>> initialTimelineItems(final boolean syncCompleted) {
        return storage.timelineItems(PAGE_SIZE)
                      .subscribeOn(scheduler)
                      .toList()
                      .map(this::toViewModels)
                      .flatMap(viewModels -> handleLocalResult(viewModels, INITIAL_TIMESTAMP, syncCompleted));
    }

    protected Observable<List<ViewModel>> updatedTimelineItems() {
        return syncInitiator.sync(syncable, SyncInitiator.ACTION_HARD_REFRESH)
                            .flatMap(syncJobResult -> handleSyncResult(syncJobResult, INITIAL_TIMESTAMP));
    }

    protected abstract List<ViewModel> toViewModels(List<StorageModel> storageModels);

    private Observable<List<ViewModel>> handleLocalResult(List<ViewModel> result,
                                                          long timestamp,
                                                          boolean syncCompleted) {
        if (isEmptyResult(result)) {
            return handleEmptyLocalResult(timestamp, syncCompleted);
        } else {
            return Observable.just(result);
        }
    }

    protected abstract boolean isEmptyResult(List<ViewModel> result);

    private Observable<List<ViewModel>> handleEmptyLocalResult(final long timestamp, boolean syncCompleted) {
        if (syncCompleted) {
            Log.d(TAG, "No items after previous sync, return empty page");
            return Observable.just(noMorePagesSentinel);
        } else {
            if (timestamp == INITIAL_TIMESTAMP) {
                Log.d(TAG, "First page; triggering full sync");
                return syncInitiator.sync(syncable)
                                    .flatMap(syncJobResult -> handleSyncResult(syncJobResult, timestamp));
            } else {
                Log.d(TAG, "Not on first page; triggering backfill sync");
                return syncInitiator.sync(syncable, ApiSyncService.ACTION_APPEND)
                                    .flatMap(syncJobResult -> handleSyncResult(syncJobResult, timestamp));
            }
        }
    }

    private Observable<List<ViewModel>> handleSyncResult(SyncJobResult syncJobResult, long currentTimestamp) {
        Log.d(TAG, "Sync finished; new items? => " + syncJobResult);
        if (syncJobResult.wasChanged()) {
            if (currentTimestamp == INITIAL_TIMESTAMP) {
                // we're coming from page 1, just load from local storage
                return initialTimelineItems(true);
            } else {
                // we're coming from a paged request
                return pagedTimelineItems(currentTimestamp, true);
            }
        } else {
            return Observable.just(noMorePagesSentinel);
        }
    }

    private Observable<List<ViewModel>> pagedTimelineItems(final long timestamp, final boolean syncCompleted) {
        return storage
                .timelineItemsBefore(timestamp, PAGE_SIZE)
                .toList()
                .subscribeOn(scheduler)
                .map(this::toViewModels)
                .flatMap(viewModels -> handleLocalResult(viewModels, timestamp, syncCompleted));
    }

    public Pager.PagingFunction<List<ViewModel>> pagingFunction() {
        return result -> {
            // We use NO_MORE_PAGES as a finish token to signal that there really are no more items to be retrieved,
            // even after doing a backfill sync. This is different from list.isEmpty, since this may be true for
            // a local result set, but there are more items on the server.
            if (result == noMorePagesSentinel) {
                return Pager.finish();
            } else {
                // to implement paging, we move the timestamp down reverse chronologically
                final Optional<Date> lastTimestamp = getLastItemTimestamp(result);
                if (lastTimestamp.isPresent()) {
                    final long nextTimestamp = lastTimestamp.get().getTime();
                    Log.d(TAG, "Building next page observable for timestamp " + nextTimestamp);
                    return pagedTimelineItems(nextTimestamp, false);
                } else {
                    return Pager.finish();
                }
            }
        };
    }

    public abstract Optional<Date> getFirstItemTimestamp(List<ViewModel> items);

    protected abstract Optional<Date> getLastItemTimestamp(List<ViewModel> items);

    public Observable<Long> lastSyncTime() {
        return Observable.just(syncStateStorage.lastSyncTime(syncable));
    }

    private Observable<Boolean> hasSyncedBefore() {
        return Observable.just(syncStateStorage.hasSyncedBefore(syncable));
    }

    public Observable<List<ViewModel>> updatedTimelineItemsForStart() {
        return hasSyncedBefore()
                .filter(IS_TRUE)
                .flatMap(o -> updatedTimelineItems())
                .onErrorResumeNext(Observable.<List<ViewModel>>empty())
                .subscribeOn(scheduler);
    }

    public Observable<Integer> newItemsSince(long time) {
        return storage.timelineItemCountSince(time)
                      .subscribeOn(scheduler);
    }

}
