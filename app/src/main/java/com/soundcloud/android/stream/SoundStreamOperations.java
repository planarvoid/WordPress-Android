package com.soundcloud.android.stream;

import static com.soundcloud.android.sync.SyncInitiator.ResultReceiverAdapter;
import static rx.android.OperatorPaged.Page;
import static rx.android.OperatorPaged.Pager;
import static rx.android.OperatorPaged.pagedWith;

import com.soundcloud.android.Consts;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.storage.PropertySet;
import com.soundcloud.android.sync.SyncInitiator;
import com.soundcloud.android.utils.Log;
import rx.Observable;
import rx.Subscriber;
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
        return loadPagedStreamItems(currentUserUrn, Long.MAX_VALUE, 0);
    }

    private Observable<Page<List<PropertySet>>> loadPagedStreamItems(Urn userUrn, long timestamp, int offset) {
        Log.d(TAG, "Preparing next page with user=" + userUrn + "; timestamp=" + timestamp + "; offset=" + offset);
        Observable<PropertySet> source = soundStreamStorage.loadStreamItemsAsync(userUrn, timestamp, DEFAULT_LIMIT, offset);
        return source.toList().lift(pagedWith(streamItemPager(userUrn, timestamp, offset)));
    }

    private Pager<List<PropertySet>> streamItemPager(final Urn userUrn, final long currentTimestamp, final int offset) {
        return new Pager<List<PropertySet>>() {
            @Override
            public Observable<Page<List<PropertySet>>> call(final List<PropertySet> propertySets) {
                logPropertySet(propertySets);
                if (propertySets.size() == DEFAULT_LIMIT) {
                    return loadNextPage(userUrn, propertySets, offset, currentTimestamp);
                } else {
                    return backfillSoundStreamAndReload(userUrn, propertySets, currentTimestamp, offset);
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

    private Observable<Page<List<PropertySet>>> loadNextPage(
            Urn userUrn, List<PropertySet> propertySets, int currentOffset, long currentTimestamp) {
        final long timestampOfFirstItem = propertySets.get(0).get(StreamItemProperty.CREATED_AT).getTime();
        // we fix the timestamp to that of the first item so as to keep paging stable in case new
        // items gets synced in meanwhile
        final long timestamp = currentOffset == 0 ? timestampOfFirstItem : currentTimestamp;
        return loadPagedStreamItems(userUrn, timestamp, currentOffset + DEFAULT_LIMIT);
    }

    private Observable<Page<List<PropertySet>>> backfillSoundStreamAndReload(
            final Urn userUrn, final List<PropertySet> propertySets, final long currentTimestamp, final int currentOffset) {
        Log.d(TAG, "Not enough items; next page will trigger sync");
        return Observable.create(new Observable.OnSubscribe<Page<List<PropertySet>>>() {
            @Override
            public void call(Subscriber<? super Page<List<PropertySet>>> subscriber) {
                Log.d(TAG, "Backfilling sound stream");
                ResultReceiverAdapter<Page<List<PropertySet>>> receiverAdapter =
                        new ResultReceiverAdapter<Page<List<PropertySet>>>(subscriber, null);
                syncInitiator.backfillSoundStream(receiverAdapter);
            }
        }).mergeMap(new Func1<Page<List<PropertySet>>, Observable<Page<List<PropertySet>>>>() {
            @Override
            public Observable<Page<List<PropertySet>>> call(Page<List<PropertySet>> listPage) {
                return loadPagedStreamItems(userUrn, currentTimestamp, currentOffset + propertySets.size());
            }
        });
    }

}
