package com.soundcloud.android.sync.timeline;

import static com.soundcloud.android.rx.RxUtils.IS_TRUE;
import static com.soundcloud.android.rx.RxUtils.continueWith;

import com.soundcloud.android.ApplicationModule;
import com.soundcloud.android.Consts;
import com.soundcloud.android.api.model.Timestamped;
import com.soundcloud.android.sync.ApiSyncService;
import com.soundcloud.android.sync.SyncInitiator;
import com.soundcloud.android.sync.SyncJobResult;
import com.soundcloud.android.sync.SyncStateStorage;
import com.soundcloud.android.sync.Syncable;
import com.soundcloud.android.utils.Log;
import com.soundcloud.java.collections.PropertySet;
import com.soundcloud.rx.Pager;
import rx.Observable;
import rx.Scheduler;
import rx.functions.Func1;

import android.support.annotation.Nullable;
import android.support.annotation.VisibleForTesting;

import javax.inject.Named;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.ListIterator;

public abstract class TimelineOperations<ItemT extends Timestamped> {

    private static final long INITIAL_TIMESTAMP = Long.MAX_VALUE;
    private static final String TAG = "Timeline";

    @VisibleForTesting
    static final int PAGE_SIZE = Consts.LIST_PAGE_SIZE;

    private final Syncable syncable;
    private final TimelineStorage storage;
    private final SyncInitiator syncInitiator;
    private final Scheduler scheduler;
    private final SyncStateStorage syncStateStorage;
    private final List<ItemT> noMorePagesSentinel = Collections.emptyList();

    public TimelineOperations(Syncable syncable,
                              TimelineStorage storage,
                              SyncInitiator syncInitiator,
                              @Named(ApplicationModule.HIGH_PRIORITY) Scheduler scheduler,
                              SyncStateStorage syncStateStorage) {
        this.syncable = syncable;
        this.storage = storage;
        this.syncInitiator = syncInitiator;
        this.scheduler = scheduler;
        this.syncStateStorage = syncStateStorage;
    }

    protected Observable<List<ItemT>> initialTimelineItems(boolean syncCompleted) {
        return storage.timelineItems(PAGE_SIZE)
                      .subscribeOn(scheduler)
                      .toList()
                      .map(toViewModels())
                      .flatMap(handleLocalResult(INITIAL_TIMESTAMP, syncCompleted));
    }

    protected Observable<List<ItemT>> updatedTimelineItems() {
        return syncInitiator.sync(syncable, SyncInitiator.ACTION_HARD_REFRESH)
                            .flatMap(handleSyncResult(INITIAL_TIMESTAMP));
    }

    protected abstract Func1<List<PropertySet>, List<ItemT>> toViewModels();

    private Func1<List<ItemT>, Observable<List<ItemT>>> handleLocalResult(
            final long timestamp, final boolean syncCompleted) {
        return new Func1<List<ItemT>, Observable<List<ItemT>>>() {
            @Override
            public Observable<List<ItemT>> call(List<ItemT> result) {
                if (isEmptyResult(result)) {
                    return handleEmptyLocalResult(timestamp, syncCompleted);
                } else {
                    return Observable.just(result);
                }
            }
        };
    }

    protected abstract boolean isEmptyResult(List<ItemT> result);

    private Observable<List<ItemT>> handleEmptyLocalResult(long timestamp, boolean syncCompleted) {
        if (syncCompleted) {
            Log.d(TAG, "No items after previous sync, return empty page");
            return Observable.just(noMorePagesSentinel);
        } else {
            if (timestamp == INITIAL_TIMESTAMP) {
                Log.d(TAG, "First page; triggering full sync");
                return syncInitiator.sync(syncable).flatMap(handleSyncResult(timestamp));
            } else {
                Log.d(TAG, "Not on first page; triggering backfill sync");
                return syncInitiator.sync(syncable, ApiSyncService.ACTION_APPEND).flatMap(handleSyncResult(timestamp));
            }
        }
    }

    private Func1<SyncJobResult, Observable<List<ItemT>>> handleSyncResult(final long currentTimestamp) {
        return new Func1<SyncJobResult, Observable<List<ItemT>>>() {
            @Override
            public Observable<List<ItemT>> call(SyncJobResult syncJobResult) {
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
        };
    }

    private Observable<List<ItemT>> pagedTimelineItems(final long timestamp, boolean syncCompleted) {
        return storage
                .timelineItemsBefore(timestamp, PAGE_SIZE).toList()
                .subscribeOn(scheduler)
                .map(toViewModels())
                .flatMap(handleLocalResult(timestamp, syncCompleted));
    }

    public Pager.PagingFunction<List<ItemT>> pagingFunction() {
        return new Pager.PagingFunction<List<ItemT>>() {
            @Override
            public Observable<List<ItemT>> call(List<ItemT> result) {
                // We use NO_MORE_PAGES as a finish token to signal that there really are no more items to be retrieved,
                // even after doing a backfill sync. This is different from list.isEmpty, since this may be true for
                // a local result set, but there are more items on the server.
                if (result == noMorePagesSentinel) {
                    return Pager.finish();
                } else {
                    // to implement paging, we move the timestamp down reverse chronologically
                    final Date lastTimestamp = getLastItemTimestamp(result);
                    if (lastTimestamp != null) {
                        final long nextTimestamp = lastTimestamp.getTime();
                        Log.d(TAG, "Building next page observable for timestamp " + nextTimestamp);
                        return pagedTimelineItems(nextTimestamp, false);
                    } else {
                        return Pager.finish();
                    }
                }
            }
        };
    }

    @Nullable
    public Date getFirstItemTimestamp(List<ItemT> items) {
        final ListIterator<ItemT> iterator = items.listIterator();
        if (iterator.hasNext()) {
            return iterator.next().getCreatedAt();
        }
        return null;
    }

    @Nullable
    protected Date getLastItemTimestamp(List<ItemT> items) {
        final ListIterator<ItemT> iterator = items.listIterator(items.size());
        if (iterator.hasPrevious()) {
            return iterator.previous().getCreatedAt();
        }
        return null;
    }

    public Observable<Long> lastSyncTime() {
        return Observable.just(syncStateStorage.lastSyncTime(syncable));
    }

    private Observable<Boolean> hasSyncedBefore() {
        return Observable.just(syncStateStorage.hasSyncedBefore(syncable));
    }

    public Observable<List<ItemT>> updatedTimelineItemsForStart() {
        return hasSyncedBefore()
                .filter(IS_TRUE)
                .flatMap(continueWith(updatedTimelineItems()))
                .onErrorResumeNext(Observable.<List<ItemT>>empty())
                .subscribeOn(scheduler);
    }

    public Observable<Integer> newItemsSince(long time) {
        return storage.timelineItemCountSince(time)
                      .subscribeOn(scheduler);
    }

}
