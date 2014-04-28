package com.soundcloud.android.stream;

import static rx.android.OperatorPaged.Page;
import static rx.android.OperatorPaged.Pager;
import static rx.android.OperatorPaged.pagedWith;

import com.soundcloud.android.Consts;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.storage.PropertySet;
import com.soundcloud.android.sync.SyncInitiator;
import com.soundcloud.android.utils.Log;
import rx.Observable;
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

    private Pager<List<PropertySet>> streamItemPager(final Urn userUrn, final long currentTimestamp) {
        return new Pager<List<PropertySet>>() {
            @Override
            public Observable<Page<List<PropertySet>>> call(final List<PropertySet> propertySets) {
                logPropertySet(propertySets);
                if (propertySets.size() == DEFAULT_LIMIT) {
                    return loadNextPage(userUrn, propertySets);
                } else {
                    return backfillSoundStreamAndReload(userUrn, currentTimestamp);
                }
            }

            // can remove this later, useful for debugging right now
            private void logPropertySet(List<PropertySet> propertySets) {
                Log.d(TAG, "Received " + propertySets.size() + " items");
                if (!propertySets.isEmpty()) {
                    Log.d(TAG, "First item = " + propertySets.get(0).get(StreamItemProperty.SOUND_URN));
                    Log.d(TAG, "Last item = " + propertySets.get(propertySets.size() - 1).get(StreamItemProperty.SOUND_URN));
                }
            }
        };
    }

    private Observable<Page<List<PropertySet>>> loadNextPage(Urn userUrn, List<PropertySet> propertySets) {
        final PropertySet lastItem = propertySets.get(propertySets.size() - 1);
        final long timestampOfLastItem = lastItem.get(StreamItemProperty.CREATED_AT).getTime();
        return loadPagedStreamItems(userUrn, timestampOfLastItem);
    }

    private Observable<Page<List<PropertySet>>> backfillSoundStreamAndReload(
            final Urn userUrn, final long currentTimestamp) {
        Log.d(TAG, "Not enough items; next page will trigger sync");
        return syncInitiator.backfillSoundStream().mergeMap(new Func1<Boolean, Observable<Page<List<PropertySet>>>>() {
            @Override
            public Observable<Page<List<PropertySet>>> call(Boolean streamUpdated) {
                return loadPagedStreamItems(userUrn, currentTimestamp);
            }
        });
    }
}
