package com.soundcloud.android.stream;

import static com.soundcloud.android.stream.StreamItem.Kind.PLAYABLE;
import static com.soundcloud.android.stream.StreamItem.Kind.PROMOTED;

import com.soundcloud.android.ApplicationModule;
import com.soundcloud.android.Consts;
import com.soundcloud.android.analytics.Screen;
import com.soundcloud.android.api.legacy.model.ContentStats;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PromotedTrackingEvent;
import com.soundcloud.android.model.EntityProperty;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.presentation.PlayableItem;
import com.soundcloud.android.presentation.PromotedListItem;
import com.soundcloud.android.storage.provider.Content;
import com.soundcloud.android.stream.StreamItem.Kind;
import com.soundcloud.android.sync.SyncInitiator;
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
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

class SoundStreamOperations {

    private static final long INITIAL_TIMESTAMP = Long.MAX_VALUE;

    @VisibleForTesting
    static final int PAGE_SIZE = Consts.LIST_PAGE_SIZE;

    private static final String TAG = "SoundStream";
    private static final List<StreamItem> NO_MORE_PAGES = Collections.emptyList();

    private final SoundStreamStorage soundStreamStorage;
    private final SyncInitiator syncInitiator;
    private final ContentStats contentStats;
    private final EventBus eventBus;
    private final RemoveStalePromotedItemsCommand removeStalePromotedItemsCommand;
    private final MarkPromotedItemAsStaleCommand markPromotedItemAsStaleCommand;
    private final Scheduler scheduler;

    private static final Func1<List<PropertySet>, List<StreamItem>> TO_STREAM_ITEMS =
            new Func1<List<PropertySet>, List<StreamItem>>() {
                @Override
                public List<StreamItem> call(List<PropertySet> bindings) {
                    final List<StreamItem> items = new ArrayList<>(bindings.size());

                    for (PropertySet source : bindings) {
                        if (isPlayable(source.get(EntityProperty.URN))) {
                            items.add(PlayableItem.from(source));
                        }
                    }
                    return items;
                }

                private boolean isPlayable(Urn urn) {
                    return urn.isTrack() || urn.isPlaylist();
                }
            };

    private final PagingFunction<List<StreamItem>> pagingFunc = new PagingFunction<List<StreamItem>>() {
        @Override
        @SuppressWarnings("PMD.CompareObjectsWithEquals") // No, PMD. I DO want to compare references.
        public Observable<List<StreamItem>> call(List<StreamItem> result) {
            // We use NO_MORE_PAGES as a finish token to signal that there really are no more items to be retrieved,
            // even after doing a backfill sync. This is different from list.isEmpty, since this may be true for
            // a local result set, but there are more items on the server.
            if (result == NO_MORE_PAGES) {
                return Pager.finish();
            } else {
                // to implement paging, we move the timestamp down reverse chronologically
                PlayableItem lastPlayableItem = getLastPlayableItem(result);

                if (lastPlayableItem != null) {
                    final long nextTimestamp = lastPlayableItem.getCreatedAt().getTime();
                    Log.d(TAG, "Building next page observable for timestamp " + nextTimestamp);
                    return pagedStreamItems(nextTimestamp, false);
                } else {
                    return Pager.finish();
                }
            }
        }
    };

    private final Action1<List<StreamItem>> promotedImpressionAction = new Action1<List<StreamItem>>() {
        @Override
        public void call(List<StreamItem> streamItems) {
            PlayableItem first = getFirstPromotedItem(streamItems);

            if (first != null) {
                markPromotedItemAsStaleCommand.call((PromotedListItem) first);
                publishTrackingEvent((PromotedListItem) first);
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

    PagingFunction<List<StreamItem>> pagingFunction() {
        return pagingFunc;
    }

    /**
     * Will deliver any stream items already existing in local storage, but also fall back to a
     * backfill sync in case it didn't find enough.
     */
    public Observable<List<StreamItem>> initialStreamItems() {
        return initialStreamItems(false)
                .doOnNext(promotedImpressionAction);
    }

    private Observable<List<StreamItem>> initialStreamItems(final boolean syncCompleted) {
        return removeStalePromotedItemsCommand.toObservable(null)
                .flatMap(loadFirstPageOfStream(syncCompleted))
                .subscribeOn(scheduler);
    }

    private Func1<List<Long>, Observable<List<StreamItem>>> loadFirstPageOfStream(final boolean syncCompleted) {
        return new Func1<List<Long>, Observable<List<StreamItem>>>() {
            @Override
            public Observable<List<StreamItem>> call(List<Long> longs) {
                Log.d(TAG, "Removed stale promoted items: " + longs.size());
                return soundStreamStorage
                        .initialStreamItems(PAGE_SIZE).toList()
                        .map(TO_STREAM_ITEMS)
                        .flatMap(handleLocalResult(INITIAL_TIMESTAMP, syncCompleted));
            }
        };
    }

    public Observable<List<StreamItem>> updatedStreamItems() {
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

    private Observable<List<StreamItem>> pagedStreamItems(final long timestamp, boolean syncCompleted) {
        return soundStreamStorage
                .streamItemsBefore(timestamp, PAGE_SIZE).toList()
                .subscribeOn(scheduler)
                .map(TO_STREAM_ITEMS)
                .flatMap(handleLocalResult(timestamp, syncCompleted));
    }

    private Func1<List<StreamItem>, Observable<List<StreamItem>>> handleLocalResult(
            final long timestamp, final boolean syncCompleted) {
        return new Func1<List<StreamItem>, Observable<List<StreamItem>>>() {
            @Override
            public Observable<List<StreamItem>> call(List<StreamItem> result) {
                if (result.isEmpty() || containsOnlyPromotedTrack(result)) {
                    return handleEmptyLocalResult(timestamp, syncCompleted);
                } else {
                    updateLastSeen(result);
                    return Observable.just(result);
                }
            }
        };
    }

    private boolean containsOnlyPromotedTrack(List<StreamItem> result) {
        return result.size() == 1 && result.get(0).getKind() == PROMOTED;
    }

    private Observable<List<StreamItem>> handleEmptyLocalResult(long timestamp, boolean syncCompleted) {
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

    private Func1<Boolean, Observable<List<StreamItem>>> handleSyncResult(final long currentTimestamp) {
        return new Func1<Boolean, Observable<List<StreamItem>>>() {
            @Override
            public Observable<List<StreamItem>> call(Boolean syncSuccess) {
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

    private void updateLastSeen(List<StreamItem> result) {
        final PlayableItem playableItem = getFirstPlayableItem(result);

        if (playableItem != null) {
            contentStats.setLastSeen(Content.ME_SOUND_STREAM, playableItem.getCreatedAt().getTime());
        }
    }

    @Nullable
    private PlayableItem getFirstPlayableItem(List<StreamItem> streamItems) {
        return (PlayableItem) getFirst(streamItems, PLAYABLE);
    }

    @Nullable
    private PlayableItem getLastPlayableItem(List<StreamItem> streamItems) {
        return (PlayableItem) getLast(streamItems, PLAYABLE);
    }

    @Nullable
    private PlayableItem getFirstPromotedItem(List<StreamItem> streamItems) {
        return (PlayableItem) getFirst(streamItems, PROMOTED);
    }

    @Nullable
    private StreamItem getFirst(List<StreamItem> streamItems, Kind... kinds) {
        List<Kind> kindList = Arrays.asList(kinds);
        for (StreamItem streamItem : streamItems) {
            if (kindList.contains(streamItem.getKind())) {
                return streamItem;
            }
        }
        return null;
    }

    @Nullable
    private StreamItem getLast(List<StreamItem> streamItems, Kind... kinds) {
        List<Kind> kindList = Arrays.asList(kinds);
        for (int i = streamItems.size() - 1; i >= 0; i--) {
            StreamItem streamItem = streamItems.get(i);
            if (kindList.contains(streamItem.getKind())) {
                return streamItem;
            }
        }
        return null;
    }
}
