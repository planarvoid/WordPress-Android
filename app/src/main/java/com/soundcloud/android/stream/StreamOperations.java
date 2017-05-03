package com.soundcloud.android.stream;

import static com.soundcloud.android.tracks.TieredTracks.isHighTierPreview;
import static com.soundcloud.java.collections.Lists.newArrayList;
import static io.reactivex.android.schedulers.AndroidSchedulers.mainThread;

import com.soundcloud.android.ApplicationModule;
import com.soundcloud.android.ads.StreamAdsController;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PromotedTrackingEvent;
import com.soundcloud.android.facebookinvites.FacebookInvitesOperations;
import com.soundcloud.android.main.Screen;
import com.soundcloud.android.playback.PlayableWithReposter;
import com.soundcloud.android.presentation.PlayableItem;
import com.soundcloud.android.stream.StreamItem.Kind;
import com.soundcloud.android.suggestedcreators.SuggestedCreatorsOperations;
import com.soundcloud.android.sync.SyncInitiator;
import com.soundcloud.android.sync.SyncStateStorage;
import com.soundcloud.android.sync.Syncable;
import com.soundcloud.android.sync.timeline.TimelineOperations;
import com.soundcloud.android.upsell.InlineUpsellOperations;
import com.soundcloud.java.optional.Optional;
import com.soundcloud.rx.eventbus.EventBus;
import io.reactivex.Scheduler;
import io.reactivex.Single;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.Date;
import java.util.List;
import java.util.ListIterator;

public class StreamOperations extends TimelineOperations<StreamEntity, StreamItem> {

    private final StreamStorage streamStorage;
    private final EventBus eventBus;
    private final FacebookInvitesOperations facebookInvites;
    private final StreamAdsController streamAdsController;
    private final InlineUpsellOperations upsellOperations;
    private final SuggestedCreatorsOperations suggestedCreatorsOperations;
    private final RemoveStalePromotedItemsCommand removeStalePromotedItemsCommand;
    private final MarkPromotedItemAsStaleCommand markPromotedItemAsStaleCommand;
    private final Scheduler scheduler;
    private final StreamEntityToItemTransformer streamEntityToItemTransformer;

    private static boolean isSuggestedCreatorsNotification(Optional<? extends StreamItem> notificationItemOptional) {
        return notificationItemOptional.isPresent() && notificationItemOptional.get().kind() == Kind.SUGGESTED_CREATORS;
    }

    @Inject
    StreamOperations(StreamStorage streamStorage,
                     SyncInitiator syncInitiator,
                     RemoveStalePromotedItemsCommand removeStalePromotedItemsCommand,
                     MarkPromotedItemAsStaleCommand markPromotedItemAsStaleCommand,
                     EventBus eventBus,
                     @Named(ApplicationModule.RX_HIGH_PRIORITY) Scheduler scheduler,
                     FacebookInvitesOperations facebookInvites,
                     StreamAdsController streamAdsController,
                     InlineUpsellOperations upsellOperations,
                     SyncStateStorage syncStateStorage,
                     SuggestedCreatorsOperations suggestedCreatorsOperations,
                     StreamEntityToItemTransformer streamEntityToItemTransformer) {
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
        this.suggestedCreatorsOperations = suggestedCreatorsOperations;
        this.upsellOperations = upsellOperations;
        this.streamEntityToItemTransformer = streamEntityToItemTransformer;
    }

    @Override
    protected Single<List<StreamItem>> toViewModels(List<StreamEntity> streamEntities) {
        return streamEntityToItemTransformer.apply(streamEntities);
    }

    Single<List<StreamItem>> initialStreamItems() {
        return removeStalePromotedItemsCommand.toSingle(null)
                                              .subscribeOn(scheduler)
                                              .flatMap(o -> initialTimelineItems(false))
                                              .zipWith(initialNotificationItem(),
                                                       StreamOperations::addNotificationItemToStream)
                                              .map(this::addUpsellableItem)
                                              .doOnSuccess(this::promotedImpressionAction)
                                              // Temporary workaround for https://github.com/soundcloud/android-listeners/issues/6807. We should move the below
                                              // logic to the presenter
                                              .observeOn(mainThread())
                                              .doOnSuccess(streamItems -> streamAdsController.insertAds());
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

    private Single<Optional<StreamItem>> initialNotificationItem() {
        return suggestedCreatorsOperations.suggestedCreatorsV2()
                                          .switchIfEmpty(facebookInvites.creatorInvites())
                                          .switchIfEmpty(facebookInvites.listenerInvites())
                                          .map(Optional::of)
                                          .toSingle(Optional.absent());
    }

    Single<List<StreamItem>> updatedStreamItems() {
        return super.updatedTimelineItems()
                    .subscribeOn(scheduler)
                    .zipWith(updatedNotificationItem(), StreamOperations::addNotificationItemToStream)
                    .doOnSuccess(this::promotedImpressionAction)
                    // Temporary workaround for https://github.com/soundcloud/android-listeners/issues/6807. We should move the below
                    // logic to the presenter
                    .observeOn(mainThread())
                    .doOnSuccess(streamItems -> streamAdsController.insertAds());
    }

    private Single<Optional<StreamItem>> updatedNotificationItem() {
        return suggestedCreatorsOperations.suggestedCreatorsV2()
                                          .map(Optional::of)
                                          .toSingle(Optional.absent());
    }

    Single<List<PlayableWithReposter>> urnsForPlayback() {
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
    private Optional<PlayableItem> getFirstPromotedListItem(List<StreamItem> streamItems) {
        for (StreamItem streamItem : streamItems) {
            final Optional<PlayableItem> playableItem = streamItem.getPlayableItem();
            if (playableItem.isPresent() && playableItem.get().isPromoted()) {
                return playableItem;
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
        final Optional<PlayableItem> promotedListItemOpt = getFirstPromotedListItem(streamItems);
        if (promotedListItemOpt.isPresent()) {
            PlayableItem promotedListItem = promotedListItemOpt.get();
            markPromotedItemAsStaleCommand.call(promotedListItem.adUrn());
            eventBus.publish(EventQueue.TRACKING, PromotedTrackingEvent.forImpression(promotedListItem, Screen.STREAM.get()));
        }
    }

    private static List<StreamItem> addNotificationItemToStream(List<StreamItem> streamItems,
                                                                Optional<? extends StreamItem> notificationItemOptional) {
        List<StreamItem> result = newArrayList(streamItems);
        if (isSuggestedCreatorsNotification(notificationItemOptional) || !streamItems.isEmpty()) {
            result.addAll(0, notificationItemOptional.asSet());
        }
        return result;
    }

}
