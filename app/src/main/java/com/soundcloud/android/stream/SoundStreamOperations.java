package com.soundcloud.android.stream;

import static com.google.common.collect.Iterables.getLast;
import static rx.android.OperatorPaged.Page;
import static rx.android.OperatorPaged.Pager;
import static rx.android.OperatorPaged.pagedWith;

import com.google.common.annotations.VisibleForTesting;
import com.soundcloud.android.Consts;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.storage.PropertySet;
import com.soundcloud.android.sync.SyncInitiator;
import com.soundcloud.android.utils.Log;
import rx.Observable;
import rx.android.OperatorPaged;
import rx.functions.Func1;

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

    @Inject
    SoundStreamOperations(SoundStreamStorage soundStreamStorage, SyncInitiator syncInitiator) {
        this.soundStreamStorage = soundStreamStorage;
        this.syncInitiator = syncInitiator;
    }

    public Observable<Page<List<PropertySet>>> getStreamItems() {
        final Urn currentUserUrn = Urn.forUser(123); // TODO
        return loadPagedStreamItems(currentUserUrn, INITIAL_TIMESTAMP);
    }

    private Observable<Page<List<PropertySet>>> loadPagedStreamItems(final Urn userUrn, final long timestamp) {
        Log.d(TAG, "Loading page with user=" + userUrn + "; timestamp=" + timestamp);
        return soundStreamStorage
                .loadStreamItemsAsync(userUrn, timestamp, PAGE_SIZE).toList()
                .mergeMap(handleLocalResult(userUrn, timestamp));
    }

    private Func1<List<PropertySet>, Observable<Page<List<PropertySet>>>> handleLocalResult(
            final Urn userUrn, final long timestamp) {
        return new Func1<List<PropertySet>, Observable<Page<List<PropertySet>>>>() {
            @Override
            public Observable<Page<List<PropertySet>>> call(List<PropertySet> result) {
                if (result.isEmpty()) {
                    Log.d(TAG, "Received empty set from local storage");
                    if (timestamp == INITIAL_TIMESTAMP) {
                        Log.d(TAG, "First page; triggering full sync");
                        return syncInitiator.syncSoundStream().mergeMap(handleSyncResult(userUrn, timestamp));
                    } else {
                        Log.d(TAG, "Not on first page; triggering backfill sync");
                        return syncInitiator.backfillSoundStream().mergeMap(handleSyncResult(userUrn, timestamp));
                    }
                } else {
                    logPropertySet(result);
                    return Observable.just(result).lift(pagedWith(streamItemPager(userUrn)));
                }
            }
        };
    }

    // can remove this later, useful for debugging right now
    private void logPropertySet(List<PropertySet> propertySets) {
        Log.d(TAG, "Received " + propertySets.size() + " items");
        if (!propertySets.isEmpty()) {
            Log.d(TAG, "First item = " + propertySets.get(0).get(StreamItemProperty.SOUND_URN) +
                    "; timestamp = " + propertySets.get(0).get(StreamItemProperty.CREATED_AT).getTime());
            Log.d(TAG, "Last item = " + getLast(propertySets).get(StreamItemProperty.SOUND_URN) +
                    "; timestamp = " + getLast(propertySets).get(StreamItemProperty.CREATED_AT).getTime());
        }
    }

    // TODO: we can simply fix the user URN and make this a final field instead of a method
    private Pager<List<PropertySet>> streamItemPager(final Urn userUrn) {
        return new Pager<List<PropertySet>>() {
            @Override
            public Observable<Page<List<PropertySet>>> call(final List<PropertySet> result) {
                // to implement paging, we move the timestamp down reverse chronologically
                final long nextTimestamp = getLast(result).get(StreamItemProperty.CREATED_AT).getTime();
                Log.d(TAG, "Building next page observable for timestamp " + nextTimestamp);
                return loadPagedStreamItems(userUrn, nextTimestamp);
            }
        };
    }

    private Func1<Boolean, Observable<Page<List<PropertySet>>>> handleSyncResult(
            final Urn userUrn, final long currentTimestamp) {
        return new Func1<Boolean, Observable<Page<List<PropertySet>>>>() {
            @Override
            public Observable<Page<List<PropertySet>>> call(Boolean streamUpdated) {
                Log.d(TAG, "Sync finished; new items added = " + streamUpdated);
                if (streamUpdated) {
                    return loadPagedStreamItems(userUrn, currentTimestamp);
                } else {
                    return OperatorPaged.emptyPageObservable();
                }
            }
        };
    }
}
