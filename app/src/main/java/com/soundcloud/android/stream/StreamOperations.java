package com.soundcloud.android.stream;

import static com.soundcloud.android.tracks.TieredTracks.isHighTierPreview;

import com.soundcloud.android.ApplicationModule;
import com.soundcloud.android.ads.StreamAdsController;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PromotedTrackingEvent;
import com.soundcloud.android.facebookinvites.FacebookInvitesOperations;
import com.soundcloud.android.main.Screen;
import com.soundcloud.android.playback.PlayableWithReposter;
import com.soundcloud.android.presentation.PromotedListItem;
import com.soundcloud.android.stations.StationsOperations;
import com.soundcloud.android.stream.StreamItem.Kind;
import com.soundcloud.android.suggestedcreators.SuggestedCreatorsOperations;
import com.soundcloud.android.sync.SyncInitiator;
import com.soundcloud.android.sync.SyncStateStorage;
import com.soundcloud.android.sync.Syncable;
import com.soundcloud.android.sync.timeline.TimelineOperations;
import com.soundcloud.android.upsell.InlineUpsellOperations;
import com.soundcloud.java.collections.Lists;
import com.soundcloud.java.optional.Optional;
import com.soundcloud.rx.eventbus.EventBus;
import rx.Observable;
import rx.Scheduler;
import rx.android.schedulers.AndroidSchedulers;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.ListIterator;

public class StreamOperations extends TimelineOperations<StreamItem, StreamPlayable> {

    private final StreamStorage streamStorage;
    private final EventBus eventBus;
    private final FacebookInvitesOperations facebookInvites;
    private final StreamAdsController streamAdsController;
    private final StationsOperations stationsOperations;
    private final InlineUpsellOperations upsellOperations;
    private final StreamHighlightsOperations streamHighlightsOperations;
    private final SuggestedCreatorsOperations suggestedCreatorsOperations;
    private final RemoveStalePromotedItemsCommand removeStalePromotedItemsCommand;
    private final MarkPromotedItemAsStaleCommand markPromotedItemAsStaleCommand;
    private final Scheduler scheduler;

    private static boolean isSuggestedCreatorsNotification(Optional<? extends StreamItem> notificationItemOptional) {
        return notificationItemOptional.isPresent() && notificationItemOptional.get().kind() == Kind.SUGGESTED_CREATORS;
    }

    @Inject
    StreamOperations(StreamStorage streamStorage,
                     SyncInitiator syncInitiator,
                     RemoveStalePromotedItemsCommand removeStalePromotedItemsCommand,
                     MarkPromotedItemAsStaleCommand markPromotedItemAsStaleCommand,
                     EventBus eventBus,
                     @Named(ApplicationModule.HIGH_PRIORITY) Scheduler scheduler,
                     FacebookInvitesOperations facebookInvites,
                     StreamAdsController streamAdsController,
                     StationsOperations stationsOperations,
                     InlineUpsellOperations upsellOperations,
                     SyncStateStorage syncStateStorage,
                     StreamHighlightsOperations streamHighlightsOperations,
                     SuggestedCreatorsOperations suggestedCreatorsOperations) {
        super(Syncable.SOUNDSTREAM,
              streamStorage,
              syncInitiator,
              scheduler,
              syncStateStorage);
        this.streamStorage = streamStorage;
        this.removeStalePromotedItemsCommand = removeStalePromotedItemsCommand;
        this.markPromotedItemAsStaleCommand = markPromotedItemAsStaleCommand;
        this.scheduler = scheduler;
        this.eventBus = eventBus;
        this.facebookInvites = facebookInvites;
        this.streamAdsController = streamAdsController;
        this.stationsOperations = stationsOperations;
        this.streamHighlightsOperations = streamHighlightsOperations;
        this.suggestedCreatorsOperations = suggestedCreatorsOperations;
        this.upsellOperations = upsellOperations;
    }

    @Override
    protected List<StreamItem> toViewModels(List<StreamPlayable> streamPlayables) {
        final List<StreamItem> items = new ArrayList<>(streamPlayables.size());

        for (StreamPlayable streamPlayable : streamPlayables) {
            items.add(StreamItem.fromStreamPlayable(streamPlayable));
        }
        return items;
    }

    Observable<List<StreamItem>> initialStreamItems() {
        return removeStalePromotedItemsCommand.toObservable(null)
                                              .subscribeOn(scheduler)
                                              .flatMap(o -> initialTimelineItems(false))
                                              .zipWith(streamHighlightsOperations.highlights().map(Optional::of)
                                                                                 .defaultIfEmpty(Optional.absent())
                                                                                 .onErrorReturn(throwable -> Optional.absent())
                                                      , StreamOperations::addNotificationItemToStream)
                                              .zipWith(initialNotificationItem(),
                                                       StreamOperations::addNotificationItemToStream)
                                              .map(this::addUpsellableItem)
                                              .doOnNext(this::promotedImpressionAction)
                                              // Temporary workaround for https://github.com/soundcloud/android-listeners/issues/6807. We should move the below
                                              // logic to the presenter
                                              .observeOn(AndroidSchedulers.mainThread())
                                              .doOnNext(streamItems -> streamAdsController.insertAds());
    }

    private List<StreamItem> addUpsellableItem(List<StreamItem> streamItems) {
        if (upsellOperations.shouldDisplayInStream()) {
            Optional<StreamItem> upsellable = getFirstUpsellable(streamItems);
            if (upsellable.isPresent()) {
                streamItems.add(streamItems.indexOf(upsellable.get()) + 1, StreamItem.forUpsell());
            }
        }
        return streamItems;
    }

    private Observable<Optional<StreamItem>> initialNotificationItem() {
        return suggestedCreatorsOperations.suggestedCreators()
                                          .switchIfEmpty(facebookInvites.creatorInvites())
                                          .switchIfEmpty(facebookInvites.listenerInvites())
                                          .switchIfEmpty(stationsOperations.onboardingStreamItem())
                                          .map(Optional::of)
                                          .switchIfEmpty(Observable.just(Optional.absent()));
    }

    Observable<List<StreamItem>> updatedStreamItems() {
        return super.updatedTimelineItems()
                    .subscribeOn(scheduler)
                    .zipWith(streamHighlightsOperations.highlights().map(Optional::of).defaultIfEmpty(Optional.absent())
                            , StreamOperations::addNotificationItemToStream)
                    .zipWith(updatedNotificationItem(), StreamOperations::addNotificationItemToStream)
                    .doOnNext(this::promotedImpressionAction)
                    // Temporary workaround for https://github.com/soundcloud/android-listeners/issues/6807. We should move the below
                    // logic to the presenter
                    .observeOn(AndroidSchedulers.mainThread())
                    .doOnNext(streamItems -> streamAdsController.insertAds());
    }

    private Observable<Optional<StreamItem>> updatedNotificationItem() {
        return suggestedCreatorsOperations.suggestedCreators()
                                          .map(Optional::of)
                                          .switchIfEmpty(Observable.just(Optional.absent()));
    }

    Observable<List<PlayableWithReposter>> urnsForPlayback() {
        return streamStorage.playbackItems().subscribeOn(scheduler).toList();
    }

    void disableUpsell() {
        upsellOperations.disableInStream();
    }

    public void clearData() {
        upsellOperations.clearData();
    }

    @Override
    protected boolean isEmptyResult(List<StreamItem> result) {
        return result.isEmpty() || containsOnlyPromotedTrack(result);
    }

    private boolean containsOnlyPromotedTrack(List<StreamItem> result) {
        return result.size() == 1 && result.get(0).isPromoted();
    }

    @NonNull
    private Optional<PromotedListItem> getFirstPromotedListItem(List<StreamItem> streamItems) {
        for (StreamItem streamItem : streamItems) {
            if (streamItem.isPromoted()) {
                return streamItem.getListItem().transform(input -> (PromotedListItem) input);
            }
        }
        return Optional.absent();
    }

    @Override
    public Optional<Date> getFirstItemTimestamp(List<StreamItem> items) {
        for (StreamItem streamItem : items) {
            final Date createdAt = getCreatedAt(streamItem);
            if (createdAt != null) return Optional.of(createdAt);
        }
        return Optional.absent();
    }

    private Optional<StreamItem> getFirstUpsellable(List<StreamItem> streamItems) {
        for (StreamItem streamItem : streamItems) {
            if (streamItem.kind() == Kind.TRACK
                    && isHighTierPreview(((TrackStreamItem) streamItem).trackItem())) {
                return Optional.of(streamItem);
            }

        }
        return Optional.absent();
    }

    @Nullable
    @Override
    protected Optional<Date> getLastItemTimestamp(List<StreamItem> items) {
        final ListIterator<StreamItem> iterator = items.listIterator(items.size());
        while (iterator.hasPrevious()) {
            final StreamItem streamItem = iterator.previous();
            final Date createdAt = getCreatedAt(streamItem);
            if (createdAt != null) return Optional.of(createdAt);
        }
        return Optional.absent();
    }

    @Nullable
    private Date getCreatedAt(StreamItem streamItem) {
        if (Kind.TRACK.equals(streamItem.kind())) {
            return ((TrackStreamItem) streamItem).createdAt();
        } else if (Kind.PLAYLIST.equals(streamItem.kind())) {
            return ((PlaylistStreamItem) streamItem).createdAt();
        }
        return null;
    }

    private void promotedImpressionAction(List<StreamItem> streamItems) {
        final Optional<PromotedListItem> promotedListItemOpt = getFirstPromotedListItem(streamItems);
        if (promotedListItemOpt.isPresent()) {
            PromotedListItem promotedListItem = promotedListItemOpt.get();
            markPromotedItemAsStaleCommand.call(promotedListItem);
            eventBus.publish(EventQueue.TRACKING,
                             PromotedTrackingEvent.forImpression(promotedListItem, Screen.STREAM.get()));
        }
    }

    private static List<StreamItem> addNotificationItemToStream(List<StreamItem> streamItems,
                                                                Optional<? extends StreamItem> notificationItemOptional) {
        List<StreamItem> result = Lists.newArrayList(streamItems);
        if (isSuggestedCreatorsNotification(notificationItemOptional) || !streamItems.isEmpty()) {
            result.addAll(0, notificationItemOptional.asSet());
        }
        return result;
    }

}
