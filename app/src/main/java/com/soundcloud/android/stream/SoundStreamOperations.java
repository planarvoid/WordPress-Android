package com.soundcloud.android.stream;

import static com.soundcloud.android.rx.RxUtils.continueWith;
import static com.soundcloud.android.presentation.TypedListItem.Kind.NOTIFICATION;
import static com.soundcloud.android.presentation.TypedListItem.Kind.PLAYABLE;
import static com.soundcloud.android.presentation.TypedListItem.Kind.PROMOTED;
import static com.soundcloud.android.presentation.TypedListItem.Kind.UPSELL;
import static com.soundcloud.android.tracks.TieredTracks.isHighTierPreview;

import com.soundcloud.android.ApplicationModule;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.FacebookInvitesEvent;
import com.soundcloud.android.events.PromotedTrackingEvent;
import com.soundcloud.android.facebookinvites.FacebookInvitesItem;
import com.soundcloud.android.facebookinvites.FacebookInvitesOperations;
import com.soundcloud.android.main.Screen;
import com.soundcloud.android.model.EntityProperty;
import com.soundcloud.android.presentation.PlayableItem;
import com.soundcloud.android.presentation.PromotedListItem;
import com.soundcloud.android.presentation.TypedListItem;
import com.soundcloud.android.upsell.UpsellListItem;
import com.soundcloud.android.stations.StationOnboardingStreamItem;
import com.soundcloud.android.stations.StationsOperations;
import com.soundcloud.android.sync.SyncInitiator;
import com.soundcloud.android.presentation.TypedListItem.Kind;
import com.soundcloud.android.sync.SyncStateStorage;
import com.soundcloud.android.sync.Syncable;
import com.soundcloud.android.sync.timeline.TimelineOperations;
import com.soundcloud.android.tracks.TieredTrack;
import com.soundcloud.android.upsell.InlineUpsellOperations;
import com.soundcloud.java.collections.PropertySet;
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

public class SoundStreamOperations extends TimelineOperations<TypedListItem> {

    private final SoundStreamStorage soundStreamStorage;
    private final EventBus eventBus;
    private final FacebookInvitesOperations facebookInvites;
    private final StationsOperations stationsOperations;
    private final InlineUpsellOperations upsellOperations;
    private final RemoveStalePromotedItemsCommand removeStalePromotedItemsCommand;
    private final MarkPromotedItemAsStaleCommand markPromotedItemAsStaleCommand;
    private final Scheduler scheduler;

    private static final Func1<List<PropertySet>, List<TypedListItem>> TO_STREAM_ITEMS =
            new Func1<List<PropertySet>, List<TypedListItem>>() {
                @Override
                public List<TypedListItem> call(List<PropertySet> bindings) {
                    final List<TypedListItem> items = new ArrayList<>(bindings.size());

                    for (PropertySet source : bindings) {
                        if (source.get(EntityProperty.URN).isPlayable()) {
                            items.add(PlayableItem.from(source));
                        }
                    }
                    return items;
                }
            };

    private final Action1<List<TypedListItem>> promotedImpressionAction = new Action1<List<TypedListItem>>() {
        @Override
        public void call(List<TypedListItem> streamItems) {
            Optional<TypedListItem> promotedListItemOpt = getFirstOfKind(streamItems, PROMOTED);

            if (promotedListItemOpt.isPresent()) {
                PromotedListItem promotedListItem = (PromotedListItem) promotedListItemOpt.get();
                markPromotedItemAsStaleCommand.call(promotedListItem);
                publishTrackingEvent(promotedListItem);
            }
        }

        private void publishTrackingEvent(PromotedListItem item) {
            eventBus.publish(EventQueue.TRACKING, PromotedTrackingEvent.forImpression(item, Screen.STREAM.get()));
        }
    };

    private final Func1<List<TypedListItem>, List<TypedListItem>> prependStationsOnboardingItem = new Func1<List<TypedListItem>, List<TypedListItem>>() {

        @Override
        public List<TypedListItem> call(List<TypedListItem> streamItems) {
            if (stationsOperations.shouldDisplayOnboardingStreamItem() && canAddDistinctItemOfKind(streamItems,
                                                                                                   NOTIFICATION)) {
                streamItems.add(0, new StationOnboardingStreamItem());
            }
            return streamItems;
        }
    };

    private final Func1<List<TypedListItem>, List<TypedListItem>> appendUpsellAfterSnippet = new Func1<List<TypedListItem>, List<TypedListItem>>() {
        @Override
        public List<TypedListItem> call(List<TypedListItem> streamItems) {
            if (upsellOperations.shouldDisplayInStream() && canAddDistinctItemOfKind(streamItems, UPSELL)) {
                Optional<TypedListItem> upsellable = getFirstUpsellable(streamItems);
                if (upsellable.isPresent()) {
                    streamItems.add(streamItems.indexOf(upsellable.get()) + 1, UpsellListItem.forStream());
                }
            }
            return streamItems;
        }
    };

    private final Func1<List<TypedListItem>, List<TypedListItem>> prependFacebookListenerInvites = new Func1<List<TypedListItem>, List<TypedListItem>>() {
        @Override
        public List<TypedListItem> call(List<TypedListItem> streamItems) {
            if (facebookInvites.canShowForListeners() && canAddDistinctItemOfKind(streamItems, NOTIFICATION)) {
                streamItems.add(0, new FacebookInvitesItem(FacebookInvitesItem.LISTENER_URN));
            }
            return streamItems;
        }
    };

    private final Func2<List<TypedListItem>, Optional<FacebookInvitesItem>, List<TypedListItem>> prependFacebookCreatorInvites = new Func2<List<TypedListItem>, Optional<FacebookInvitesItem>, List<TypedListItem>>() {
        @Override
        public List<TypedListItem> call(List<TypedListItem> streamItems, Optional<FacebookInvitesItem> notification) {
            if (notification.isPresent() && canAddDistinctItemOfKind(streamItems, NOTIFICATION)) {
                streamItems.add(0, notification.get());
                publishCreatorFacebookInvitesShown();
            }
            return streamItems;
        }

        private void publishCreatorFacebookInvitesShown() {
            eventBus.publish(EventQueue.TRACKING, FacebookInvitesEvent.forCreatorShown());
        }
    };

    @Inject
    SoundStreamOperations(SoundStreamStorage soundStreamStorage, SyncInitiator syncInitiator,
                          RemoveStalePromotedItemsCommand removeStalePromotedItemsCommand,
                          MarkPromotedItemAsStaleCommand markPromotedItemAsStaleCommand, EventBus eventBus,
                          @Named(ApplicationModule.HIGH_PRIORITY) Scheduler scheduler,
                          FacebookInvitesOperations facebookInvites,
                          StationsOperations stationsOperations, InlineUpsellOperations upsellOperations,
                          SyncStateStorage syncStateStorage) {
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
        this.upsellOperations = upsellOperations;
    }

    @Override
    protected Func1<List<PropertySet>, List<TypedListItem>> toViewModels() {
        return TO_STREAM_ITEMS;
    }

    public Observable<List<TypedListItem>> initialStreamItems() {
        return initialTimelineItems(false).doOnNext(promotedImpressionAction);
    }

    @Override
    protected Observable<List<TypedListItem>> initialTimelineItems(boolean syncCompleted) {
        return removeStalePromotedItemsCommand.toObservable(null)
                                              .subscribeOn(scheduler)
                                              .flatMap(continueWith(super.initialTimelineItems(syncCompleted)))
                                              .zipWith(facebookInvites.creatorInvites(), prependFacebookCreatorInvites)
                                              .map(appendUpsellAfterSnippet)
                                              .map(prependFacebookListenerInvites)
                                              .map(prependStationsOnboardingItem);
    }

    private boolean canAddDistinctItemOfKind(List<TypedListItem> streamItems, Kind kind) {
        return streamItems.size() > 0 && !getFirstOfKind(streamItems, kind).isPresent();
    }

    public Observable<List<TypedListItem>> updatedStreamItems() {
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
    protected boolean isEmptyResult(List<TypedListItem> result) {
        return result.isEmpty() || containsOnlyPromotedTrack(result);
    }

    private boolean containsOnlyPromotedTrack(List<TypedListItem> result) {
        return result.size() == 1 && result.get(0).getKind() == PROMOTED;
    }

    @NonNull
    private Optional<TypedListItem> getFirstOfKind(List<TypedListItem> streamItems, Kind kind) {
        for (TypedListItem streamItem : streamItems) {
            if (kind.equals(streamItem.getKind())) {
                return Optional.of(streamItem);
            }
        }
        return Optional.absent();
    }

    @NonNull
    private Optional<TypedListItem> getLastOfKind(List<TypedListItem> streamItems, Kind kind) {
        final ListIterator<TypedListItem> iterator = streamItems.listIterator(streamItems.size());
        while (iterator.hasPrevious()) {
            final TypedListItem streamItem = iterator.previous();
            if (kind.equals(streamItem.getKind())) {
                return Optional.of(streamItem);
            }
        }
        return Optional.absent();
    }

    @Nullable
    @Override
    public Date getFirstItemTimestamp(List<TypedListItem> items) {
        final Optional<TypedListItem> streamItem = getFirstOfKind(items, PLAYABLE);
        if (streamItem.isPresent()) {
            return streamItem.get().getCreatedAt();
        }
        return null;
    }

    private Optional<TypedListItem> getFirstUpsellable(List<TypedListItem> streamItems) {
        for (TypedListItem streamItem : streamItems) {
            if (streamItem instanceof TieredTrack && isHighTierPreview((TieredTrack) streamItem)) {
                return Optional.of(streamItem);
            }
        }
        return Optional.absent();
    }

    @Nullable
    @Override
    protected Date getLastItemTimestamp(List<TypedListItem> items) {
        final Optional<TypedListItem> streamItem = getLastOfKind(items, PLAYABLE);
        if (streamItem.isPresent()) {
            return streamItem.get().getCreatedAt();
        }
        return null;
    }

}