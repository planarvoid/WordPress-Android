package com.soundcloud.android.stream;

import static com.soundcloud.android.rx.RxUtils.continueWith;
import static com.soundcloud.android.tracks.TieredTracks.isHighTierPreview;

import com.soundcloud.android.ApplicationModule;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PromotedTrackingEvent;
import com.soundcloud.android.facebookinvites.FacebookInvitesOperations;
import com.soundcloud.android.main.Screen;
import com.soundcloud.android.model.EntityProperty;
import com.soundcloud.android.presentation.ListItem;
import com.soundcloud.android.presentation.PlayableItem;
import com.soundcloud.android.presentation.PromotedListItem;
import com.soundcloud.android.rx.RxUtils;
import com.soundcloud.android.stations.StationsOperations;
import com.soundcloud.android.stream.SoundStreamItem.Kind;
import com.soundcloud.android.suggestedcreators.SuggestedCreatorsOperations;
import com.soundcloud.android.sync.SyncInitiator;
import com.soundcloud.android.sync.SyncStateStorage;
import com.soundcloud.android.sync.Syncable;
import com.soundcloud.android.sync.timeline.TimelineOperations;
import com.soundcloud.android.upsell.InlineUpsellOperations;
import com.soundcloud.java.collections.Lists;
import com.soundcloud.java.collections.PropertySet;
import com.soundcloud.java.functions.Function;
import com.soundcloud.java.optional.Optional;
import com.soundcloud.rx.eventbus.EventBus;
import rx.Observable;
import rx.Scheduler;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.functions.Func2;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.ListIterator;

public class SoundStreamOperations extends TimelineOperations<SoundStreamItem> {

    private final SoundStreamStorage soundStreamStorage;
    private final EventBus eventBus;
    private final FacebookInvitesOperations facebookInvites;
    private final StationsOperations stationsOperations;
    private final InlineUpsellOperations upsellOperations;
    private final SuggestedCreatorsOperations suggestedCreatorsOperations;
    private final RemoveStalePromotedItemsCommand removeStalePromotedItemsCommand;
    private final MarkPromotedItemAsStaleCommand markPromotedItemAsStaleCommand;
    private final Scheduler scheduler;

    private static final Func1<List<PropertySet>, List<SoundStreamItem>> TO_STREAM_ITEMS =
            new Func1<List<PropertySet>, List<SoundStreamItem>>() {
                @Override
                public List<SoundStreamItem> call(List<PropertySet> bindings) {
                    final List<SoundStreamItem> items = new ArrayList<>(bindings.size());

                    for (PropertySet source : bindings) {
                        if (source.get(EntityProperty.URN).isPlayable()) {
                            items.add(SoundStreamItem.fromPlayableItem(PlayableItem.from(source)));
                        }
                    }
                    return items;
                }
            };

    private final Action1<List<SoundStreamItem>> promotedImpressionAction = new Action1<List<SoundStreamItem>>() {
        @Override
        public void call(List<SoundStreamItem> streamItems) {
            final Optional<PromotedListItem> promotedListItemOpt = getFirstPromotedListItem(streamItems);
            if (promotedListItemOpt.isPresent()) {
                handlePromotedItem(promotedListItemOpt.get());
            }
        }

        private void handlePromotedItem(PromotedListItem promotedListItem) {
            markPromotedItemAsStaleCommand.call(promotedListItem);
            eventBus.publish(EventQueue.TRACKING, PromotedTrackingEvent.forImpression(promotedListItem, Screen.STREAM.get()));
        }
    };

    private final Func1<List<SoundStreamItem>, List<SoundStreamItem>> appendUpsellAfterSnippet = new Func1<List<SoundStreamItem>, List<SoundStreamItem>>() {
        @Override
        public List<SoundStreamItem> call(List<SoundStreamItem> streamItems) {
            if (upsellOperations.shouldDisplayInStream()) {
                Optional<SoundStreamItem> upsellable = getFirstUpsellable(streamItems);
                if (upsellable.isPresent()) {
                    streamItems.add(streamItems.indexOf(upsellable.get()) + 1, SoundStreamItem.forUpsell());
                }
            }
            return streamItems;
        }
    };

    private static final Func2<List<SoundStreamItem>, Optional<SoundStreamItem>, List<SoundStreamItem>> addNotificationItemToStream = new Func2<List<SoundStreamItem>, Optional<SoundStreamItem>, List<SoundStreamItem>>() {
        @Override
        public List<SoundStreamItem> call(List<SoundStreamItem> SoundStreamItems, Optional<SoundStreamItem> notificationItemOptional) {
            List<SoundStreamItem> result = Lists.newArrayList(SoundStreamItems);
            if (isSuggestedCreatorsNotification(notificationItemOptional) || !SoundStreamItems.isEmpty()) {
                result.addAll(0, notificationItemOptional.asSet());
            }
            return result;
        }
    };

    private static final Function<ListItem, PromotedListItem> listItemToPromoted = new Function<ListItem, PromotedListItem>() {
        public PromotedListItem apply(ListItem input) {
            return (PromotedListItem) input;
        }
    };

    private static boolean isSuggestedCreatorsNotification(Optional<SoundStreamItem> notificationItemOptional) {
        return notificationItemOptional.isPresent() && notificationItemOptional.get().kind() == Kind.SUGGESTED_CREATORS;
    }

    @Inject
    SoundStreamOperations(SoundStreamStorage soundStreamStorage, SyncInitiator syncInitiator,
                          RemoveStalePromotedItemsCommand removeStalePromotedItemsCommand,
                          MarkPromotedItemAsStaleCommand markPromotedItemAsStaleCommand, EventBus eventBus,
                          @Named(ApplicationModule.HIGH_PRIORITY) Scheduler scheduler,
                          FacebookInvitesOperations facebookInvites,
                          StationsOperations stationsOperations, InlineUpsellOperations upsellOperations,
                          SyncStateStorage syncStateStorage,
                          SuggestedCreatorsOperations suggestedCreatorsOperations) {
        super(Syncable.SOUNDSTREAM,
              soundStreamStorage,
              syncInitiator,
              scheduler,
              syncStateStorage);
        this.soundStreamStorage = soundStreamStorage;
        this.removeStalePromotedItemsCommand = removeStalePromotedItemsCommand;
        this.markPromotedItemAsStaleCommand = markPromotedItemAsStaleCommand;
        this.scheduler = scheduler;
        this.eventBus = eventBus;
        this.facebookInvites = facebookInvites;
        this.stationsOperations = stationsOperations;
        this.suggestedCreatorsOperations = suggestedCreatorsOperations;
        this.upsellOperations = upsellOperations;
    }

    @Override
    protected Func1<List<PropertySet>, List<SoundStreamItem>> toViewModels() {
        return TO_STREAM_ITEMS;
    }

    public Observable<List<SoundStreamItem>> initialStreamItems() {
        return removeStalePromotedItemsCommand.toObservable(null)
                                              .subscribeOn(scheduler)
                                              .flatMap(continueWith(initialTimelineItems(false)))
                                              .zipWith(notificationItem(), addNotificationItemToStream)
                                              .map(appendUpsellAfterSnippet)
                                              .doOnNext(promotedImpressionAction);
    }

    private Observable<Optional<SoundStreamItem>> notificationItem() {
        return suggestedCreatorsOperations.suggestedCreators()
                                          .switchIfEmpty(facebookInvites.creatorInvites())
                                          .switchIfEmpty(facebookInvites.listenerInvites())
                                          .switchIfEmpty(stationsOperations.onboardingStreamItem())
                                          .map(RxUtils.<SoundStreamItem>toOptional())
                                          .switchIfEmpty(Observable.just(Optional.<SoundStreamItem>absent()));
    }

    public Observable<List<SoundStreamItem>> updatedStreamItems() {
        return super.updatedTimelineItems()
                    .subscribeOn(scheduler)
                    .doOnNext(promotedImpressionAction);
    }

    public Observable<List<PropertySet>> urnsForPlayback() {
        return soundStreamStorage
                .playbackItems()
                .subscribeOn(scheduler)
                .toList();
    }

    void disableUpsell() {
        upsellOperations.disableInStream();
    }

    public void clearData() {
        upsellOperations.clearData();
    }

    @Override
    protected boolean isEmptyResult(List<SoundStreamItem> result) {
        return result.isEmpty() || containsOnlyPromotedTrack(result);
    }

    private boolean containsOnlyPromotedTrack(List<SoundStreamItem> result) {
        return result.size() == 1 && result.get(0).isPromoted();
    }

    @NonNull
    private Optional<SoundStreamItem> getFirstOfKind(List<SoundStreamItem> streamItems, Kind... kinds) {
        for (SoundStreamItem streamItem : streamItems) {
            for (Kind kind : kinds) {
                if (kind.equals(streamItem.kind())) {
                    return Optional.of(streamItem);
                }
            }
        }
        return Optional.absent();
    }

    @NonNull
    private Optional<PromotedListItem> getFirstPromotedListItem(List<SoundStreamItem> streamItems) {
        for (SoundStreamItem streamItem : streamItems) {
            if (streamItem.isPromoted()) {
                return streamItem.getListItem().transform(listItemToPromoted);
            }
        }
        return Optional.absent();
    }

    @NonNull
    private Optional<SoundStreamItem> getLastOfKind(List<SoundStreamItem> streamItems, Kind... kinds) {
        final ListIterator<SoundStreamItem> iterator = streamItems.listIterator(streamItems.size());
        while (iterator.hasPrevious()) {
            final SoundStreamItem streamItem = iterator.previous();
            for (Kind kind : kinds) {
                if (kind.equals(streamItem.kind())) {
                    return Optional.of(streamItem);
                }
            }
        }
        return Optional.absent();
    }

    @Nullable
    @Override
    public Date getFirstItemTimestamp(List<SoundStreamItem> items) {
        final Optional<SoundStreamItem> streamItem = getFirstOfKind(items, Kind.TRACK, Kind.PLAYLIST);
        if (streamItem.isPresent()) {
            return streamItem.get().getCreatedAt();
        }
        return null;
    }

    private Optional<SoundStreamItem> getFirstUpsellable(List<SoundStreamItem> streamItems) {
        for (SoundStreamItem streamItem : streamItems) {
            if (streamItem.kind() == Kind.TRACK
                    && isHighTierPreview(((SoundStreamItem.Track) streamItem).trackItem())) {
                return Optional.of(streamItem);
            }

        }
        return Optional.absent();
    }

    @Nullable
    @Override
    protected Date getLastItemTimestamp(List<SoundStreamItem> items) {
        final Optional<SoundStreamItem> streamItem = getLastOfKind(items, Kind.TRACK, Kind.PLAYLIST);
        if (streamItem.isPresent()) {
            return streamItem.get().getCreatedAt();
        }
        return null;
    }

}
