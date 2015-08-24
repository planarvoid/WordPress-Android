package com.soundcloud.android.stream;

import static com.soundcloud.java.collections.Iterables.getLast;

import com.soundcloud.android.ApplicationModule;
import com.soundcloud.android.Consts;
import com.soundcloud.android.analytics.Screen;
import com.soundcloud.android.api.legacy.model.ContentStats;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PromotedTrackingEvent;
import com.soundcloud.android.model.EntityProperty;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playlists.PromotedPlaylistItem;
import com.soundcloud.android.presentation.PlayableItem;
import com.soundcloud.android.presentation.PromotedListItem;
import com.soundcloud.android.storage.provider.Content;
import com.soundcloud.android.sync.SyncInitiator;
import com.soundcloud.android.tracks.PromotedTrackItem;
import com.soundcloud.android.utils.Log;
import com.soundcloud.java.collections.PropertySet;
import com.soundcloud.rx.Pager;
import com.soundcloud.rx.Pager.PagingFunction;
import com.soundcloud.rx.eventbus.EventBus;
import rx.Observable;
import rx.Scheduler;
import rx.functions.Action1;
import rx.functions.Func1;

import android.support.annotation.Nullable;
import android.support.annotation.VisibleForTesting;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

class SoundStreamOperations {

    private static final long INITIAL_TIMESTAMP = Long.MAX_VALUE;

    @VisibleForTesting
    static final int PAGE_SIZE = Consts.LIST_PAGE_SIZE;

    private static final String TAG = "SoundStream";
    private static final List<PlayableItem> NO_MORE_PAGES = Collections.emptyList();

    private final SoundStreamStorage soundStreamStorage;
    private final SyncInitiator syncInitiator;
    private final ContentStats contentStats;
    private final EventBus eventBus;
    private final RemoveStalePromotedItemsCommand removeStalePromotedItemsCommand;
    private final MarkPromotedItemAsStaleCommand markPromotedItemAsStaleCommand;
    private final Scheduler scheduler;

    private static final Func1<List<PropertySet>, List<PlayableItem>> TO_PLAYABLE_ITEMS =
            new Func1<List<PropertySet>, List<PlayableItem>>() {
                @Override
                public List<PlayableItem> call(List<PropertySet> bindings) {
                    final List<PlayableItem> items = new ArrayList<>(bindings.size());

                    for (PropertySet source : bindings) {
                        final Urn urn = source.get(EntityProperty.URN);

                        if (urn.isTrack() || urn.isPlaylist()) {
                            items.add(PlayableItem.from(source));
                        }
                    }
                    return items;
                }
            };

    private final PagingFunction<List<PlayableItem>> pagingFunc = new PagingFunction<List<PlayableItem>>() {
        @Override
        @SuppressWarnings("PMD.CompareObjectsWithEquals") // No, PMD. I DO want to compare references.
        public Observable<List<PlayableItem>> call(List<PlayableItem> result) {
            // We use NO_MORE_PAGES as a finish token to signal that there really are no more items to be retrieved,
            // even after doing a backfill sync. This is different from list.isEmpty, since this may be true for
            // a local result set, but there are more items on the server.
            if (result == NO_MORE_PAGES) {
                return Pager.finish();
            } else {
                // to implement paging, we move the timestamp down reverse chronologically
                final long nextTimestamp = getLast(result).getCreatedAt().getTime();
                Log.d(TAG, "Building next page observable for timestamp " + nextTimestamp);
                return pagedStreamItems(nextTimestamp, false);
            }
        }
    };

    private final Action1<List<PlayableItem>> promotedImpressionAction = new Action1<List<PlayableItem>>() {
        @Override
        public void call(List<PlayableItem> playableItems) {
            if (!playableItems.isEmpty()) {
                PlayableItem first = playableItems.get(0);
                if (first.hasAdUrn()) {
                    // seen the item once, don't see it again until we refresh the stream
                    markPromotedItemAsStaleCommand.call(first.getPropertySet());
                    Urn urn = first.getEntityUrn();
                    if (urn.isTrack()) {
                        publishTrackingEvent((PromotedTrackItem) first);
                    } else if (urn.isPlaylist()) {
                        publishTrackingEvent((PromotedPlaylistItem) first);
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

    PagingFunction<List<PlayableItem>> pagingFunction() {
        return pagingFunc;
    }

    /**
     * Will deliver any stream items already existing in local storage, but also fall back to a
     * backfill sync in case it didn't find enough.
     */
    public Observable<List<PlayableItem>> initialStreamItems() {
        return initialStreamItems(false)
                .doOnNext(promotedImpressionAction);
    }

    private Observable<List<PlayableItem>> initialStreamItems(final boolean syncCompleted) {
        Log.d(TAG, "Preparing page; initial page");
        return removeStalePromotedItemsCommand.toObservable(null)
                .flatMap(loadFirstPageOfStream(syncCompleted))
                .subscribeOn(scheduler);
    }

    private Func1<List<Long>, Observable<List<PlayableItem>>> loadFirstPageOfStream(final boolean syncCompleted) {
        return new Func1<List<Long>, Observable<List<PlayableItem>>>() {
            @Override
            public Observable<List<PlayableItem>> call(List<Long> longs) {
                Log.d(TAG, "Removed stale promoted items: " + longs.size());
                return soundStreamStorage
                        .initialStreamItems(PAGE_SIZE).toList()
                        .map(TO_PLAYABLE_ITEMS)
                        .flatMap(handleLocalResult(INITIAL_TIMESTAMP, syncCompleted));
            }
        };
    }

    public Observable<List<PlayableItem>> updatedStreamItems() {
        return syncInitiator.refreshSoundStream()
                .flatMap(handleSyncResult(INITIAL_TIMESTAMP))
                .doOnNext(promotedImpressionAction);
    }

    public Observable<List<PropertySet>> trackUrnsForPlayback() {
        return soundStreamStorage
                .tracksForPlayback()
                .subscribeOn(scheduler)
                .toList();
    }

    private Observable<List<PlayableItem>> pagedStreamItems(final long timestamp, boolean syncCompleted) {
        Log.d(TAG, "Preparing page; timestamp=" + timestamp);
        return soundStreamStorage
                .streamItemsBefore(timestamp, PAGE_SIZE).toList()
                .subscribeOn(scheduler)
                .map(TO_PLAYABLE_ITEMS)
                .flatMap(handleLocalResult(timestamp, syncCompleted));
    }

    private Func1<List<PlayableItem>, Observable<List<PlayableItem>>> handleLocalResult(
            final long timestamp, final boolean syncCompleted) {
        return new Func1<List<PlayableItem>, Observable<List<PlayableItem>>>() {
            @Override
            public Observable<List<PlayableItem>> call(List<PlayableItem> result) {
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

    private boolean containsOnlyPromotedTrack(List<PlayableItem> result) {
        return result.size() == 1 && result.get(0).hasAdUrn();
    }

    private Observable<List<PlayableItem>> handleEmptyLocalResult(long timestamp, boolean syncCompleted) {
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
    private void logPropertySet(List<PlayableItem> playableItems) {
        Log.d(TAG, "Received " + playableItems.size() + " items");
        if (!playableItems.isEmpty()) {
            Log.d(TAG, "First item = " + playableItems.get(0).getEntityUrn() +
                    "; timestamp = " + playableItems.get(0).getCreatedAt().getTime());
            Log.d(TAG, "Last item = " + getLast(playableItems).getEntityUrn() +
                    "; timestamp = " + getLast(playableItems).getCreatedAt().getTime());
        }
    }

    private Func1<Boolean, Observable<List<PlayableItem>>> handleSyncResult(final long currentTimestamp) {
        return new Func1<Boolean, Observable<List<PlayableItem>>>() {
            @Override
            public Observable<List<PlayableItem>> call(Boolean syncSuccess) {
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

    private void updateLastSeen(List<PlayableItem> result) {
        final PlayableItem firstNonPromotedItem = getFirstNonPromotedItem(result);
        if (firstNonPromotedItem != null) {
            contentStats.setLastSeen(Content.ME_SOUND_STREAM,
                    firstNonPromotedItem.getCreatedAt().getTime());
        }
    }

    @Nullable
    private PlayableItem getFirstNonPromotedItem(List<PlayableItem> result) {
        for (PlayableItem playableItem : result) {
            if (!playableItem.hasAdUrn()) {
                return playableItem;
            }
        }
        return null;
    }
}
