package com.soundcloud.android.stream;

import static com.google.common.collect.Iterables.getLast;

import com.google.common.annotations.VisibleForTesting;
import com.soundcloud.android.Consts;
import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.api.legacy.model.ContentStats;
import com.soundcloud.android.model.PlayableProperty;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.storage.provider.Content;
import com.soundcloud.android.sync.SyncInitiator;
import com.soundcloud.android.utils.Log;
import com.soundcloud.propeller.PropertySet;
import rx.Observable;
import rx.android.Pager;
import rx.functions.Func1;

import android.content.Context;

import javax.inject.Inject;
import java.util.Collections;
import java.util.List;

class SoundStreamOperations {

    @VisibleForTesting
    static final long INITIAL_TIMESTAMP = Long.MAX_VALUE;
    @VisibleForTesting
    static final int PAGE_SIZE = Consts.LIST_PAGE_SIZE;

    private static final String TAG = "SoundStream";
    private static final List<PropertySet> NO_MORE_PAGES = Collections.emptyList();

    private final ISoundStreamStorage soundStreamStorage;
    private final SyncInitiator syncInitiator;
    private final Context appContext;
    private final Urn currentUserUrn;

    private final Pager<List<PropertySet>> pager = new Pager<List<PropertySet>>() {
        @Override
        @SuppressWarnings("PMD.CompareObjectsWithEquals") // No, PMD. I DO want to compare references.
        public Observable<List<PropertySet>> call(List<PropertySet> result) {
            // We use NO_MORE_PAGES as a finish token to signal that there really are no more items to be retrieved,
            // even after doing a backfill sync. This is different from list.isEmpty, since this may be true for
            // a local result set, but there are more items on the server.
            if (result == NO_MORE_PAGES) {
                return Pager.finish();
            } else {
                // to implement paging, we move the timestamp down reverse chronologically
                final long nextTimestamp = getLast(result).get(PlayableProperty.CREATED_AT).getTime();
                Log.d(TAG, "Building next page observable for timestamp " + nextTimestamp);
                return pagedStreamItems(nextTimestamp, false);
            }
        }
    };

    @Inject
    SoundStreamOperations(ISoundStreamStorage soundStreamStorage, SyncInitiator syncInitiator,
                          AccountOperations accountOperations, Context appContext) {
        this.soundStreamStorage = soundStreamStorage;
        this.syncInitiator = syncInitiator;
        this.appContext = appContext;
        this.currentUserUrn = accountOperations.getLoggedInUserUrn();
    }

    Pager<List<PropertySet>> pager() {
        return pager;
    }

    public Observable<List<PropertySet>> updatedStreamItems() {
        return syncInitiator.refreshSoundStream().flatMap(handleSyncResult(INITIAL_TIMESTAMP));
    }

    /**
     * Will deliver any stream items already existing in local storage, but also fall back to a
     * backfill sync in case it didn't find enough.
     */
    public Observable<List<PropertySet>> existingStreamItems() {
        return pagedStreamItems(INITIAL_TIMESTAMP, false);
    }

    public Observable<List<Urn>> trackUrnsForPlayback() {
        return soundStreamStorage.trackUrns().toList();
    }

    public void updateLastSeen() {
        ContentStats.setLastSeen(appContext, Content.ME_SOUND_STREAM, System.currentTimeMillis());
    }

    private Observable<List<PropertySet>> pagedStreamItems(final long timestamp, boolean syncCompleted) {
        Log.d(TAG, "Preparing page with user=" + currentUserUrn + "; timestamp=" + timestamp);
        return soundStreamStorage
                .streamItemsBefore(timestamp, currentUserUrn, PAGE_SIZE).toList()
                .flatMap(handleLocalResult(timestamp, syncCompleted));
    }

    private Func1<List<PropertySet>, Observable<List<PropertySet>>> handleLocalResult(
            final long timestamp, final boolean syncCompleted) {
        return new Func1<List<PropertySet>, Observable<List<PropertySet>>>() {
            @Override
            public Observable<List<PropertySet>> call(List<PropertySet> result) {
                if (result.isEmpty()) {
                    return handleEmptyLocalResult(timestamp, syncCompleted);
                } else {
                    logPropertySet(result);
                    return Observable.just(result);
                }
            }
        };
    }

    private Observable<List<PropertySet>> handleEmptyLocalResult(long timestamp, boolean syncCompleted) {
        Log.d(TAG, "Received empty set from local storage");
        if (syncCompleted) {
            Log.d(TAG, "No items after previous sync, return empty page");
            return Observable.just(NO_MORE_PAGES);
        } else {
            if (timestamp == INITIAL_TIMESTAMP) {
                Log.d(TAG, "First page; triggering full sync");
                return syncInitiator.refreshSoundStream().flatMap(handleSyncResult(timestamp));
            } else {
                Log.d(TAG, "Not on first page; triggering backfill sync");
                return syncInitiator.backfillSoundStream().flatMap(handleSyncResult(timestamp));
            }
        }
    }

    // can remove this later, useful for debugging right now
    private void logPropertySet(List<PropertySet> propertySets) {
        Log.d(TAG, "Received " + propertySets.size() + " items");
        if (!propertySets.isEmpty()) {
            Log.d(TAG, "First item = " + propertySets.get(0).get(PlayableProperty.URN) +
                    "; timestamp = " + propertySets.get(0).get(PlayableProperty.CREATED_AT).getTime());
            Log.d(TAG, "Last item = " + getLast(propertySets).get(PlayableProperty.URN) +
                    "; timestamp = " + getLast(propertySets).get(PlayableProperty.CREATED_AT).getTime());
        }
    }

    private Func1<Boolean, Observable<List<PropertySet>>> handleSyncResult(final long currentTimestamp) {
        return new Func1<Boolean, Observable<List<PropertySet>>>() {
            @Override
            public Observable<List<PropertySet>> call(Boolean syncSuccess) {
                Log.d(TAG, "Sync finished; success = " + syncSuccess);
                if (syncSuccess) {
                    return pagedStreamItems(currentTimestamp, true);
                } else {
                    return Observable.just(NO_MORE_PAGES);
                }
            }
        };
    }
}
