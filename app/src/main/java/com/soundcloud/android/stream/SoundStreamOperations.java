package com.soundcloud.android.stream;

import static com.google.common.collect.Iterables.getLast;
import static rx.android.OperatorPaged.Page;
import static rx.android.OperatorPaged.Pager;
import static rx.android.OperatorPaged.pagedWith;

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

    private static final int DEFAULT_LIMIT = Consts.LIST_PAGE_SIZE;
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
        return loadPagedStreamItems(currentUserUrn, Long.MAX_VALUE);
    }

    private Observable<Page<List<PropertySet>>> loadPagedStreamItems(Urn userUrn, long timestamp) {
        Log.d(TAG, "Preparing next page with user=" + userUrn + "; timestamp=" + timestamp);
        Observable<PropertySet> source = soundStreamStorage.loadStreamItemsAsync(userUrn, timestamp, DEFAULT_LIMIT);
        return source.toList().lift(pagedWith(streamItemPager(userUrn, timestamp)));
    }

    private Pager<List<PropertySet>> streamItemPager(final Urn userUrn, final long lastTimestamp) {
        return new Pager<List<PropertySet>>() {
            @Override
            public Observable<Page<List<PropertySet>>> call(final List<PropertySet> propertySets) {
                logPropertySet(propertySets);
                // to implement paging, we move the timestamp down reverse chronologically
                final long nextTimestamp = propertySets.isEmpty() ? lastTimestamp : timestampOfLastItem(propertySets);
                if (propertySets.size() == DEFAULT_LIMIT) {
                    // if we're able to fill a full page, we assume there will be more data stored locally
                    return loadPagedStreamItems(userUrn, nextTimestamp);
                } else {
                    // otherwise we assume there might be more data server side, so trigger a backfill sync
                    return backfillSoundStreamAndReload(userUrn, nextTimestamp);
                }
            }

            // can remove this later, useful for debugging right now
            private void logPropertySet(List<PropertySet> propertySets) {
                Log.d(TAG, "Received " + propertySets.size() + " items");
                if (!propertySets.isEmpty()) {
                    Log.d(TAG, "First item = " + propertySets.get(0).get(StreamItemProperty.SOUND_URN) +
                            "; timestamp = " + propertySets.get(0).get(StreamItemProperty.CREATED_AT));
                    Log.d(TAG, "Last item = " + getLast(propertySets).get(StreamItemProperty.SOUND_URN) +
                            "; timestamp = " + timestampOfLastItem(propertySets));
                }
            }
        };
    }

    private long timestampOfLastItem(List<PropertySet> propertySets) {
        return getLast(propertySets).get(StreamItemProperty.CREATED_AT).getTime();
    }

    private Observable<Page<List<PropertySet>>> backfillSoundStreamAndReload(
            final Urn userUrn, final long currentTimestamp) {
        Log.d(TAG, "Not enough items; next page will trigger sync");
        return syncInitiator.backfillSoundStream().mergeMap(new Func1<Boolean, Observable<Page<List<PropertySet>>>>() {
            @Override
            public Observable<Page<List<PropertySet>>> call(Boolean streamUpdated) {
                if (streamUpdated) {
                    return loadPagedStreamItems(userUrn, currentTimestamp);
                } else {
                    return OperatorPaged.emptyPageObservable();
                }
            }
        });
    }
}
