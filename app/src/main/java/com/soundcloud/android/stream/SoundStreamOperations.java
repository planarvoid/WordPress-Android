package com.soundcloud.android.stream;

import static com.soundcloud.android.rx.RxUtils.continueWith;
import static com.soundcloud.android.stream.StreamItem.Kind.NOTIFICATION;
import static com.soundcloud.android.stream.StreamItem.Kind.PLAYABLE;
import static com.soundcloud.android.stream.StreamItem.Kind.PROMOTED;

import com.soundcloud.android.ApplicationModule;
import com.soundcloud.android.api.legacy.model.ContentStats;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PromotedTrackingEvent;
import com.soundcloud.android.events.StreamNotificationEvent;
import com.soundcloud.android.facebookinvites.FacebookInvitesItem;
import com.soundcloud.android.facebookinvites.FacebookInvitesOperations;
import com.soundcloud.android.main.Screen;
import com.soundcloud.android.model.EntityProperty;
import com.soundcloud.android.presentation.PlayableItem;
import com.soundcloud.android.presentation.PromotedListItem;
import com.soundcloud.android.properties.FeatureFlags;
import com.soundcloud.android.properties.Flag;
import com.soundcloud.android.stations.StationOnboardingStreamItem;
import com.soundcloud.android.stations.StationsOperations;
import com.soundcloud.android.stream.StreamItem.Kind;
import com.soundcloud.android.sync.SyncContent;
import com.soundcloud.android.sync.SyncInitiator;
import com.soundcloud.android.sync.timeline.TimelineOperations;
import com.soundcloud.java.collections.PropertySet;
import com.soundcloud.java.optional.Optional;
import com.soundcloud.rx.eventbus.EventBus;
import rx.Observable;
import rx.Scheduler;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.functions.Func2;

import android.support.annotation.Nullable;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.ListIterator;

class SoundStreamOperations extends TimelineOperations<StreamItem> {

    private final SoundStreamStorage soundStreamStorage;
    private final EventBus eventBus;
    private final FacebookInvitesOperations facebookInvites;
    private final StationsOperations stationsOperations;
    private final FeatureFlags featureFlags;
    private final RemoveStalePromotedItemsCommand removeStalePromotedItemsCommand;
    private final MarkPromotedItemAsStaleCommand markPromotedItemAsStaleCommand;
    private final Scheduler scheduler;

    private static final Func1<List<PropertySet>, List<StreamItem>> TO_STREAM_ITEMS =
            new Func1<List<PropertySet>, List<StreamItem>>() {
                @Override
                public List<StreamItem> call(List<PropertySet> bindings) {
                    final List<StreamItem> items = new ArrayList<>(bindings.size());

                    for (PropertySet source : bindings) {
                        if (source.get(EntityProperty.URN).isPlayable()) {
                            items.add(PlayableItem.from(source));
                        }
                    }
                    return items;
                }
            };

    private final Action1<List<StreamItem>> promotedImpressionAction = new Action1<List<StreamItem>>() {
        @Override
        public void call(List<StreamItem> streamItems) {
            PromotedListItem promotedListItem = (PromotedListItem) getFirstOfKind(streamItems, PROMOTED);

            if (promotedListItem != null) {
                markPromotedItemAsStaleCommand.call(promotedListItem);
                publishTrackingEvent(promotedListItem);
            }
        }

        private void publishTrackingEvent(PromotedListItem item) {
            eventBus.publish(EventQueue.TRACKING, PromotedTrackingEvent.forImpression(item, Screen.STREAM.get()));
        }
    };

    private final Func1<List<StreamItem>, List<StreamItem>> prependStationsOnboardingItem = new Func1<List<StreamItem>, List<StreamItem>>() {

        @Override
        public List<StreamItem> call(List<StreamItem> streamItems) {
            if (featureFlags.isEnabled(Flag.STATIONS_SOFT_LAUNCH)) {
                if (stationsOperations.shouldDisplayOnboardingStreamItem() && canAddNotification(streamItems)) {
                    streamItems.add(0, new StationOnboardingStreamItem());
                }
            }
            return streamItems;
        }
    };

    @Inject
    SoundStreamOperations(SoundStreamStorage soundStreamStorage, SyncInitiator syncInitiator,
                          ContentStats contentStats, RemoveStalePromotedItemsCommand removeStalePromotedItemsCommand,
                          MarkPromotedItemAsStaleCommand markPromotedItemAsStaleCommand, EventBus eventBus,
                          @Named(ApplicationModule.HIGH_PRIORITY) Scheduler scheduler,
                          FacebookInvitesOperations facebookInvites,
                          StationsOperations stationsOperations, FeatureFlags featureFlags) {
        super(SyncContent.MySoundStream, soundStreamStorage, syncInitiator, contentStats, scheduler);
        this.soundStreamStorage = soundStreamStorage;
        this.removeStalePromotedItemsCommand = removeStalePromotedItemsCommand;
        this.markPromotedItemAsStaleCommand = markPromotedItemAsStaleCommand;
        this.scheduler = scheduler;
        this.eventBus = eventBus;
        this.facebookInvites = facebookInvites;
        this.stationsOperations = stationsOperations;
        this.featureFlags = featureFlags;
    }

    @Override
    protected Func1<List<PropertySet>, List<StreamItem>> toViewModels() {
        return TO_STREAM_ITEMS;
    }

    public Observable<List<StreamItem>> initialStreamItems() {
        return initialTimelineItems(false).doOnNext(promotedImpressionAction);
    }

    @Override
    protected Observable<List<StreamItem>> initialTimelineItems(boolean syncCompleted) {
        return removeStalePromotedItemsCommand.toObservable(null)
                .subscribeOn(scheduler)
                .flatMap(continueWith(super.initialTimelineItems(syncCompleted)))
                .zipWith(facebookInvites.loadWithPictures(), prependFacebookInvites())
                .map(prependStationsOnboardingItem);
    }

    private Func2<List<StreamItem>, Optional<FacebookInvitesItem>, List<StreamItem>> prependFacebookInvites() {
        return new Func2<List<StreamItem>, Optional<FacebookInvitesItem>, List<StreamItem>>() {
            @Override
            public List<StreamItem> call(List<StreamItem> streamItems, Optional<FacebookInvitesItem> notification) {
                if (notification.isPresent() && canAddNotification(streamItems)) {
                    streamItems.add(0, notification.get());
                    publishFacebookInvitesShown(notification.get());
                }
                return streamItems;
            }
        };
    }

    private boolean canAddNotification(List<StreamItem> streamItems) {
        return streamItems.size() > 0 && getFirstOfKind(streamItems, NOTIFICATION) == null;
    }

    public Observable<List<StreamItem>> updatedStreamItems() {
        return super.updatedTimelineItems()
                .subscribeOn(scheduler)
                .doOnNext(promotedImpressionAction);
    }

    public Observable<List<PropertySet>> trackUrnsForPlayback() {
        return soundStreamStorage
                .tracksForPlayback()
                .subscribeOn(scheduler)
                .toList();
    }

    @Override
    protected boolean isEmptyResult(List<StreamItem> result) {
        return result.isEmpty() || containsOnlyPromotedTrack(result);
    }

    private boolean containsOnlyPromotedTrack(List<StreamItem> result) {
        return result.size() == 1 && result.get(0).getKind() == PROMOTED;
    }

    private void publishFacebookInvitesShown(FacebookInvitesItem notification) {
        eventBus.publish(EventQueue.TRACKING, StreamNotificationEvent.forFacebookInviteShown(notification));
    }

    @Nullable
    private StreamItem getFirstOfKind(List<StreamItem> streamItems, Kind kind) {
        for (StreamItem streamItem : streamItems) {
            if (kind.equals(streamItem.getKind())) {
                return streamItem;
            }
        }
        return null;
    }

    @Nullable
    private StreamItem getLastOfKind(List<StreamItem> streamItems, Kind kind) {
        final ListIterator<StreamItem> iterator = streamItems.listIterator(streamItems.size());
        while (iterator.hasPrevious()) {
            final StreamItem streamItem = iterator.previous();
            if (kind.equals(streamItem.getKind())) {
                return streamItem;
            }
        }
        return null;
    }

    @Nullable
    @Override
    protected Date getFirstItemTimestamp(List<StreamItem> items) {
        final StreamItem streamItem = getFirstOfKind(items, PLAYABLE);
        if (streamItem != null) {
            return streamItem.getCreatedAt();
        }
        return null;
    }

    @Nullable
    @Override
    protected Date getLastItemTimestamp(List<StreamItem> items) {
        final StreamItem streamItem = getLastOfKind(items, PLAYABLE);
        if (streamItem != null) {
            return streamItem.getCreatedAt();
        }
        return null;
    }
}
