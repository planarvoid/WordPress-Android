package com.soundcloud.android.stream;

import static com.google.common.collect.Iterables.getLast;
import static rx.android.OperatorPaged.Page;
import static rx.android.OperatorPaged.Pager;
import static rx.android.OperatorPaged.pagedWith;

import com.google.common.annotations.VisibleForTesting;
import com.soundcloud.android.Consts;
import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.api.legacy.model.ContentStats;
import com.soundcloud.android.model.PlayableProperty;
import com.soundcloud.android.tracks.TrackUrn;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.storage.provider.Content;
import com.soundcloud.android.sync.SyncInitiator;
import com.soundcloud.android.utils.Log;
import com.soundcloud.propeller.PropertySet;
import rx.Observable;
import rx.android.OperatorPaged;
import rx.functions.Func1;

import android.content.Context;

import javax.inject.Inject;
import java.util.List;

class SoundStreamOperations {

    @VisibleForTesting
    static final long INITIAL_TIMESTAMP = Long.MAX_VALUE;
    @VisibleForTesting
    static final int PAGE_SIZE = Consts.LIST_PAGE_SIZE;

    private static final String TAG = "SoundStream";

    private final SoundStreamStorage soundStreamStorage;
    private final SyncInitiator syncInitiator;
    private final AccountOperations accountOperations;
    private final Context appContext;

    @Inject
    SoundStreamOperations(SoundStreamStorage soundStreamStorage, SyncInitiator syncInitiator,
                          AccountOperations accountOperations, Context appContext) {
        this.soundStreamStorage = soundStreamStorage;
        this.syncInitiator = syncInitiator;
        this.accountOperations = accountOperations;
        this.appContext = appContext;
    }

    public Observable<Page<List<PropertySet>>> updatedStreamItems() {
        return syncInitiator.refreshSoundStream().mergeMap(handleSyncResult(
                accountOperations.getLoggedInUserUrn(), INITIAL_TIMESTAMP));
    }

    /**
     * Will deliver any stream items already existing in local storage, but also fall back to a
     * backfill sync in case it didn't find enough.
     */
    public Observable<Page<List<PropertySet>>> existingStreamItems() {
        return pagedStreamItems(accountOperations.getLoggedInUserUrn(), INITIAL_TIMESTAMP, false);
    }

    public Observable<TrackUrn> trackUrnsForPlayback() {
        return soundStreamStorage.trackUrns();
    }

    public void updateLastSeen() {
        ContentStats.updateCount(appContext, Content.ME_SOUND_STREAM, 0);
        ContentStats.setLastSeen(appContext, Content.ME_SOUND_STREAM, System.currentTimeMillis());
    }

    private Observable<Page<List<PropertySet>>> pagedStreamItems(final Urn userUrn, final long timestamp, boolean syncCompleted) {
        Log.d(TAG, "Loading page with user=" + userUrn + "; timestamp=" + timestamp);
        return soundStreamStorage
                .streamItemsBefore(timestamp, userUrn, PAGE_SIZE).toList()
                .mergeMap(handleLocalResult(userUrn, timestamp, syncCompleted));
    }

    private Func1<List<PropertySet>, Observable<Page<List<PropertySet>>>> handleLocalResult(
            final Urn userUrn, final long timestamp, final boolean syncCompleted) {
        return new Func1<List<PropertySet>, Observable<Page<List<PropertySet>>>>() {
            @Override
            public Observable<Page<List<PropertySet>>> call(List<PropertySet> result) {
                if (result.isEmpty()) {
                    return handleEmptyResult(timestamp, userUrn, syncCompleted);
                } else {
                    logPropertySet(result);
                    return Observable.just(result).lift(pagedWith(streamItemPager(userUrn)));
                }
            }
        };
    }

    private Observable<Page<List<PropertySet>>> handleEmptyResult(long timestamp, Urn userUrn, boolean syncCompleted) {
        Log.d(TAG, "Received empty set from local storage");
        if (syncCompleted) {
            Log.d(TAG, "No items after previous sync, return empty page");
            return OperatorPaged.emptyObservable();
        } else {
            if (timestamp == INITIAL_TIMESTAMP) {
                Log.d(TAG, "First page; triggering full sync");
                return syncInitiator.refreshSoundStream().mergeMap(handleSyncResult(userUrn, timestamp));
            } else {
                Log.d(TAG, "Not on first page; triggering backfill sync");
                return syncInitiator.backfillSoundStream().mergeMap(handleSyncResult(userUrn, timestamp));
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

    // TODO: we can simply fix the user URN and make this a final field instead of a method
    private Pager<List<PropertySet>> streamItemPager(final Urn userUrn) {
        return new Pager<List<PropertySet>>() {
            @Override
            public Observable<Page<List<PropertySet>>> call(final List<PropertySet> result) {
                // to implement paging, we move the timestamp down reverse chronologically
                final long nextTimestamp = getLast(result).get(PlayableProperty.CREATED_AT).getTime();
                Log.d(TAG, "Building next page observable for timestamp " + nextTimestamp);
                return pagedStreamItems(userUrn, nextTimestamp, false);
            }
        };
    }

    private Func1<Boolean, Observable<Page<List<PropertySet>>>> handleSyncResult(
            final Urn userUrn, final long currentTimestamp) {
        return new Func1<Boolean, Observable<Page<List<PropertySet>>>>() {
            @Override
            public Observable<Page<List<PropertySet>>> call(Boolean syncSuccess) {
                Log.d(TAG, "Sync finished; new items added = " + syncSuccess);
                if (syncSuccess) {
                    return pagedStreamItems(userUrn, currentTimestamp, true);
                } else {
                    return OperatorPaged.emptyPageObservable();
                }
            }
        };
    }
}
