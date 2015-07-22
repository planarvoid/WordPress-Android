package com.soundcloud.android.stream;

import static com.google.common.collect.Iterables.getLast;

import com.google.common.annotations.VisibleForTesting;
import com.soundcloud.android.ApplicationModule;
import com.soundcloud.android.Consts;
import com.soundcloud.android.analytics.Screen;
import com.soundcloud.android.api.legacy.model.ContentStats;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PromotedTrackingEvent;
import com.soundcloud.android.model.EntityProperty;
import com.soundcloud.android.model.PlayableProperty;
import com.soundcloud.android.model.PromotedItemProperty;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playlists.PromotedPlaylistItem;
import com.soundcloud.android.presentation.PromotedListItem;
import com.soundcloud.android.rx.eventbus.EventBus;
import com.soundcloud.android.storage.provider.Content;
import com.soundcloud.android.sync.SyncInitiator;
import com.soundcloud.android.tracks.PromotedTrackItem;
import com.soundcloud.android.utils.Log;
import com.soundcloud.java.collections.PropertySet;
import com.soundcloud.rx.Pager;
import com.soundcloud.rx.Pager.PagingFunction;
import rx.Observable;
import rx.Scheduler;
import rx.functions.Action1;
import rx.functions.Func1;

import android.support.annotation.Nullable;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.Collections;
import java.util.List;

class SoundStreamOperations {

    private static final long INITIAL_TIMESTAMP = Long.MAX_VALUE;

    @VisibleForTesting
    static final int PAGE_SIZE = Consts.LIST_PAGE_SIZE;

    private static final String TAG = "SoundStream";
    private static final List<PropertySet> NO_MORE_PAGES = Collections.emptyList();

    private final SoundStreamStorage soundStreamStorage;
    private final SyncInitiator syncInitiator;
    private final ContentStats contentStats;
    private final EventBus eventBus;
    private final RemoveStalePromotedItemsCommand removeStalePromotedItemsCommand;
    private final MarkPromotedItemAsStaleCommand markPromotedItemAsStaleCommand;
    private final Scheduler scheduler;

    private final PagingFunction<List<PropertySet>> pagingFunc = new PagingFunction<List<PropertySet>>() {
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

    private final Action1<List<PropertySet>> promotedImpressionAction = new Action1<List<PropertySet>>() {
        @Override
        public void call(List<PropertySet> propertySets) {
            if (!propertySets.isEmpty()) {
                PropertySet first = propertySets.get(0);
                if (first.contains(PromotedItemProperty.AD_URN)) {
                    // seen the item once, don't see it again until we refresh the stream
                    markPromotedItemAsStaleCommand.call(first);
                    Urn urn = first.get(EntityProperty.URN);
                    if (urn.isTrack()) {
                        publishTrackingEvent(PromotedTrackItem.from(first));
                    } else if (urn.isPlaylist()) {
                        publishTrackingEvent(PromotedPlaylistItem.from(first));
                    }
                }
            }
        }

        private void publishTrackingEvent(PromotedListItem item) {
            eventBus.publish(EventQueue.TRACKING, PromotedTrackingEvent.forImpression(item, Screen.SIDE_MENU_STREAM.get()));
        }
    };

    @Inject
    SoundStreamOperations(SoundStreamStorage soundStreamStorage, SyncInitiator syncInitiator,
                          ContentStats contentStats, RemoveStalePromotedItemsCommand removeStalePromotedItemsCommand,
                          MarkPromotedItemAsStaleCommand markPromotedItemAsStaleCommand, EventBus eventBus,
                          @Named(ApplicationModule.HIGH_PRIORITY) Scheduler scheduler) {
        this.soundStreamStorage = soundStreamStorage;
        this.syncInitiator = syncInitiator;
        this.contentStats = contentStats;
        this.removeStalePromotedItemsCommand = removeStalePromotedItemsCommand;
        this.markPromotedItemAsStaleCommand = markPromotedItemAsStaleCommand;
        this.scheduler = scheduler;
        this.eventBus = eventBus;
    }

    PagingFunction<List<PropertySet>> pagingFunction() {
        return pagingFunc;
    }

    /**
     * Will deliver any stream items already existing in local storage, but also fall back to a
     * backfill sync in case it didn't find enough.
     */
    public Observable<List<PropertySet>> initialStreamItems() {
        return initialStreamItems(false)
                .doOnNext(promotedImpressionAction);
    }

    private Observable<List<PropertySet>> initialStreamItems(final boolean syncCompleted) {
        Log.d(TAG, "Preparing page; initial page");
        return removeStalePromotedItemsCommand.toObservable(null)
                .flatMap(loadFirstPageOfStream(syncCompleted))
                .subscribeOn(scheduler);
    }

    private Func1<List<Long>, Observable<List<PropertySet>>> loadFirstPageOfStream(final boolean syncCompleted) {
        return new Func1<List<Long>, Observable<List<PropertySet>>>() {
            @Override
            public Observable<List<PropertySet>> call(List<Long> longs) {
                Log.d(TAG, "Removed stale promoted items: " + longs.size());
                return soundStreamStorage
                        .initialStreamItems(PAGE_SIZE).toList()
                        .flatMap(handleLocalResult(INITIAL_TIMESTAMP, syncCompleted));
            }
        };
    }

    public Observable<List<PropertySet>> updatedStreamItems() {
        return syncInitiator.refreshSoundStream()
                .flatMap(handleSyncResult(INITIAL_TIMESTAMP))
                .doOnNext(promotedImpressionAction);
    }

    public Observable<List<Urn>> trackUrnsForPlayback() {
        return soundStreamStorage
                .trackUrns()
                .subscribeOn(scheduler)
                .toList();
    }

    private Observable<List<PropertySet>> pagedStreamItems(final long timestamp, boolean syncCompleted) {
        Log.d(TAG, "Preparing page; timestamp=" + timestamp);
        return soundStreamStorage
                .streamItemsBefore(timestamp, PAGE_SIZE).toList()
                .subscribeOn(scheduler)
                .flatMap(handleLocalResult(timestamp, syncCompleted));
    }

    private Func1<List<PropertySet>, Observable<List<PropertySet>>> handleLocalResult(
            final long timestamp, final boolean syncCompleted) {
        return new Func1<List<PropertySet>, Observable<List<PropertySet>>>() {
            @Override
            public Observable<List<PropertySet>> call(List<PropertySet> result) {
                if (result.isEmpty() || containsOnlyPromotedTrack(result)) {
                    return handleEmptyLocalResult(timestamp, syncCompleted);
                } else {
                    updateLastSeen(result);
                    logPropertySet(result);
                    return Observable.just(result);
                }
            }
        };
    }

    private boolean containsOnlyPromotedTrack(List<PropertySet> result) {
        return result.size() == 1 && result.get(0).contains(PromotedItemProperty.AD_URN);
    }

    private Observable<List<PropertySet>> handleEmptyLocalResult(long timestamp, boolean syncCompleted) {
        Log.d(TAG, "Received empty set from local storage");
        if (syncCompleted) {
            Log.d(TAG, "No items after previous sync, return empty page");
            return Observable.just(NO_MORE_PAGES);
        } else {
            if (timestamp == INITIAL_TIMESTAMP) {
                Log.d(TAG, "First page; triggering full sync");
                return syncInitiator.initialSoundStream().flatMap(handleSyncResult(timestamp));
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
                    if (currentTimestamp == INITIAL_TIMESTAMP) {
                        return initialStreamItems(true);
                    } else {
                        return pagedStreamItems(currentTimestamp, true);
                    }
                } else {
                    return Observable.just(NO_MORE_PAGES);
                }
            }
        };
    }

    private void updateLastSeen(List<PropertySet> result) {
        final PropertySet firstNonPromotedItem = getFirstNonPromotedItem(result);
        if (firstNonPromotedItem != null) {
            contentStats.setLastSeen(Content.ME_SOUND_STREAM,
                    firstNonPromotedItem.get(PlayableProperty.CREATED_AT).getTime());
        }
    }

    @Nullable
    private PropertySet getFirstNonPromotedItem(List<PropertySet> result) {
        for (PropertySet propertySet : result) {
            if (!propertySet.contains(PromotedItemProperty.AD_URN)) {
                return propertySet;
            }
        }
        return null;
    }
}
