package com.soundcloud.android.sync;

import static com.soundcloud.android.ApplicationModule.LOW_PRIORITY;

import com.soundcloud.android.storage.provider.Content;
import com.soundcloud.android.utils.CurrentDateProvider;
import com.soundcloud.android.utils.DateProvider;
import com.soundcloud.android.utils.Log;
import rx.Observable;
import rx.Scheduler;
import rx.functions.Func0;

import android.net.Uri;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

public class SyncStateManager {
    private static final String TAG = ApiSyncService.LOG_TAG;

    private final SyncStateStorage syncStateStorage;
    private final DateProvider dateProvider;
    private final Scheduler scheduler;

    @Inject
    public SyncStateManager(SyncStateStorage syncStateStorage,
                            CurrentDateProvider dateProvider,
                            @Named(LOW_PRIORITY) Scheduler scheduler) {
        this.syncStateStorage = syncStateStorage;
        this.dateProvider = dateProvider;
        this.scheduler = scheduler;
    }

    public void clear() {
        syncStateStorage.clear();
    }

    public boolean forceToStale(final Content content) {
        return updateLastSyncAttempt(content.uri, 0)
                && updateLastSyncSuccess(content.uri, 0);
    }

    public boolean onSyncComplete(ApiSyncResult result, Uri collectionUri) {
        return result != null
                && result.synced_at > 0
                && updateLastSyncSuccess(collectionUri, dateProvider.getCurrentTime());
    }

    private boolean updateLastSyncSuccess(Uri collectionUri, long timestamp) {
        return syncStateStorage
                .legacyUpdateLastSyncSuccess(collectionUri, timestamp)
                .success();
    }

    public boolean updateLastSyncAttempt(Uri contentUri) {
        return updateLastSyncAttempt(contentUri, dateProvider.getCurrentTime());
    }

    public Observable<Boolean> updateLastSyncAttemptAsync(final Uri contentUri) {
        return Observable.defer(new Func0<Observable<Boolean>>() {
            @Override
            public Observable<Boolean> call() {
                return Observable.just(updateLastSyncAttempt(contentUri, dateProvider.getCurrentTime()));
            }
        }).subscribeOn(scheduler);
    }

    private boolean updateLastSyncAttempt(Uri contentUri, long timestamp) {
        return syncStateStorage
                .legacyUpdateLastSyncAttempt(contentUri, timestamp)
                .success();
    }

    public int incrementSyncMiss(Uri contentUri) {
        final int misses = syncStateStorage.legacyLoadSyncMisses(contentUri) + 1;
        if (syncStateStorage.legacyUpdateSyncMisses(contentUri, misses).success()) {
            Log.d(TAG, "Updating sync misses: " + misses);
            return misses;
        } else {
            Log.d(TAG, "Failed updating sync misses");
            return -1;
        }
    }

    public boolean resetSyncMisses(Uri contentUri) {
        return syncStateStorage.legacyUpdateSyncMisses(contentUri, 0).success();
    }

    public Observable<Boolean> resetSyncMissesAsync(final Uri contentUri) {
        return Observable.defer(new Func0<Observable<Boolean>>() {
            @Override
            public Observable<Boolean> call() {
                return Observable.just(syncStateStorage.legacyUpdateSyncMisses(contentUri, 0).success());
            }
        }).subscribeOn(scheduler);
    }

    /**
     * Returns a list of uris to be synced, based on recent changes. The idea is that collections which don't change
     * very often don't get synced as frequently as collections which do.
     *
     * @param syncContentEnumSet
     * @param force              force sync {@link android.content.ContentResolver#SYNC_EXTRAS_MANUAL}
     */
    public List<Uri> getCollectionsDueForSync(EnumSet<SyncContent> syncContentEnumSet, boolean force) {
        List<Uri> urisToSync = new ArrayList<>();
        for (SyncContent sc : syncContentEnumSet) {
            if (force || isContentDueForSync(sc)) {
                urisToSync.add(sc.content.uri);
            }
        }
        return urisToSync;
    }

    public boolean isContentDueForSync(SyncContent syncContent) {
        final int syncMisses = syncStateStorage.legacyLoadSyncMisses(syncContent.contentUri());
        final long lastSyncSuccess = syncStateStorage.legacyLoadLastSyncSuccess(syncContent.contentUri());
        final boolean shouldSync = syncContent.shouldSync(syncMisses, lastSyncSuccess);
        Log.d(TAG, "-> shouldSync? " + shouldSync + "; syncMisses = "
                + syncMisses + "; lastSyncSuccess = " + lastSyncSuccess);
        return shouldSync;
    }
}
