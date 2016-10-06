package com.soundcloud.android.stream;

import static com.soundcloud.android.rx.RxUtils.continueWith;
import static com.soundcloud.android.tracks.TieredTracks.isHighTierPreview;

import com.soundcloud.android.ApplicationModule;
import com.soundcloud.android.ads.AdsOperations;
import com.soundcloud.android.ads.AppInstallAd;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PromotedTrackingEvent;
import com.soundcloud.android.facebookinvites.FacebookInvitesOperations;
import com.soundcloud.android.main.Screen;
import com.soundcloud.android.presentation.ListItem;
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
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

public class SoundStreamOperations extends TimelineOperations<SoundStreamItem, StreamPlayable> {

    private final SoundStreamStorage soundStreamStorage;
    private final EventBus eventBus;
    private final FacebookInvitesOperations facebookInvites;
    private final AdsOperations adsOperations;
    private final StationsOperations stationsOperations;
    private final InlineUpsellOperations upsellOperations;
    private final SuggestedCreatorsOperations suggestedCreatorsOperations;
    private final RemoveStalePromotedItemsCommand removeStalePromotedItemsCommand;
    private final MarkPromotedItemAsStaleCommand markPromotedItemAsStaleCommand;
    private final Scheduler scheduler;

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

    private static final Func2<List<SoundStreamItem>, List<AppInstallAd>, List<SoundStreamItem>> addAdInlaysIntoStream = new Func2<List<SoundStreamItem>, List<AppInstallAd>, List<SoundStreamItem>>() {
        @Override
        public List<SoundStreamItem> call(List<SoundStreamItem> streamItems, List<AppInstallAd> ads) {
            if (ads.isEmpty()) {
                return streamItems;
            }

            final List<List<SoundStreamItem>> partitionedStream = Lists.partition(streamItems, 4);
            final Iterator<AppInstallAd> adIterator = ads.iterator();
            final List<SoundStreamItem> result = new ArrayList<>();

            for (List<SoundStreamItem> partition : partitionedStream) {
                result.addAll(partition);
                if (adIterator.hasNext()) {
                    result.add(SoundStreamItem.AppInstall.create(adIterator.next()));
                }
            }

            return result;
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
                          AdsOperations adsOperations,
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
        this.adsOperations = adsOperations;
        this.stationsOperations = stationsOperations;
        this.suggestedCreatorsOperations = suggestedCreatorsOperations;
        this.upsellOperations = upsellOperations;
    }

    @Override
    protected List<SoundStreamItem> toViewModels(List<StreamPlayable> streamPlayables) {
        final List<SoundStreamItem> items = new ArrayList<>(streamPlayables.size());

        for (StreamPlayable streamPlayable : streamPlayables) {
            items.add(SoundStreamItem.fromStreamPlayable(streamPlayable));
        }
        return items;
    }

    Observable<List<SoundStreamItem>> initialStreamItems() {
        return removeStalePromotedItemsCommand.toObservable(null)
                                              .subscribeOn(scheduler)
                                              .flatMap(continueWith(initialTimelineItems(false)))
                                              .zipWith(initialNotificationItem(), addNotificationItemToStream)
                                              .zipWith(adsOperations.inlaysAds(), addAdInlaysIntoStream)
                                              .map(appendUpsellAfterSnippet)
                                              .doOnNext(promotedImpressionAction);
    }

    private Observable<Optional<SoundStreamItem>> initialNotificationItem() {
        return suggestedCreatorsOperations.suggestedCreators()
                                          .switchIfEmpty(facebookInvites.creatorInvites())
                                          .switchIfEmpty(facebookInvites.listenerInvites())
                                          .switchIfEmpty(stationsOperations.onboardingStreamItem())
                                          .map(RxUtils.<SoundStreamItem>toOptional())
                                          .switchIfEmpty(Observable.just(Optional.<SoundStreamItem>absent()));
    }

    Observable<List<SoundStreamItem>> updatedStreamItems() {
        return super.updatedTimelineItems()
                    .subscribeOn(scheduler)
                    .zipWith(updatedNotificationItem(), addNotificationItemToStream)
                    .zipWith(adsOperations.inlaysAds(), addAdInlaysIntoStream)
                    .doOnNext(promotedImpressionAction);
    }

    private Observable<Optional<SoundStreamItem>> updatedNotificationItem() {
        return suggestedCreatorsOperations.suggestedCreators()
                                          .map(RxUtils.<SoundStreamItem>toOptional())
                                          .switchIfEmpty(Observable.just(Optional.<SoundStreamItem>absent()));
    }

    Observable<List<PropertySet>> urnsForPlayback() {
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
    private Optional<PromotedListItem> getFirstPromotedListItem(List<SoundStreamItem> streamItems) {
        for (SoundStreamItem streamItem : streamItems) {
            if (streamItem.isPromoted()) {
                return streamItem.getListItem().transform(listItemToPromoted);
            }
        }
        return Optional.absent();
    }

    @Override
    public Optional<Date> getFirstItemTimestamp(List<SoundStreamItem> items) {
        for (SoundStreamItem streamItem : items) {
            final Date createdAt = getCreatedAt(streamItem);
            if (createdAt != null) return Optional.of(createdAt);
        }
        return Optional.absent();
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
    protected Optional<Date> getLastItemTimestamp(List<SoundStreamItem> items) {
        final ListIterator<SoundStreamItem> iterator = items.listIterator(items.size());
        while (iterator.hasPrevious()) {
            final SoundStreamItem streamItem = iterator.previous();
            final Date createdAt = getCreatedAt(streamItem);
            if (createdAt != null) return Optional.of(createdAt);
        }
        return Optional.absent();
    }

    @Nullable
    private Date getCreatedAt(SoundStreamItem streamItem) {
        if (Kind.TRACK.equals(streamItem.kind())) {
            return ((SoundStreamItem.Track)streamItem).createdAt();
        } else if (Kind.PLAYLIST.equals(streamItem.kind())) {
            return ((SoundStreamItem.Playlist)streamItem).createdAt();
        }
        return null;
    }
}
