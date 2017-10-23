package com.soundcloud.android.stream

import com.soundcloud.android.ApplicationModule
import com.soundcloud.android.ads.StreamAdsController
import com.soundcloud.android.events.EventQueue
import com.soundcloud.android.events.PromotedTrackingEvent
import com.soundcloud.android.events.TrackingEvent
import com.soundcloud.android.facebookinvites.FacebookInvitesOperations
import com.soundcloud.android.main.Screen
import com.soundcloud.android.playback.PlayableWithReposter
import com.soundcloud.android.presentation.PlayableItem
import com.soundcloud.android.rx.observers.DefaultDisposableCompletableObserver.fireAndForget
import com.soundcloud.android.suggestedcreators.SuggestedCreatorsOperations
import com.soundcloud.android.sync.SyncInitiator
import com.soundcloud.android.sync.SyncStateStorage
import com.soundcloud.android.sync.Syncable
import com.soundcloud.android.sync.timeline.TimelineOperations
import com.soundcloud.android.tracks.TieredTracks.isHighTierPreview
import com.soundcloud.android.upsell.InlineUpsellOperations
import com.soundcloud.android.utils.OpenForTesting
import com.soundcloud.android.utils.extensions.zipWith
import com.soundcloud.java.collections.Lists.newArrayList
import com.soundcloud.java.optional.Optional
import com.soundcloud.rx.eventbus.EventBusV2
import io.reactivex.Observable
import io.reactivex.Scheduler
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers.mainThread
import java.util.Date
import javax.inject.Inject
import javax.inject.Named

@OpenForTesting
class StreamOperations
@Inject
internal constructor(private val streamStorage: StreamStorage,
                     syncInitiator: SyncInitiator,
                     private val removeStalePromotedItemsCommand: RemoveStalePromotedItemsCommand,
                     private val markPromotedItemAsStaleCommand: MarkPromotedItemAsStaleCommand,
                     private val eventBus: EventBusV2,
                     @param:Named(ApplicationModule.RX_HIGH_PRIORITY) private val scheduler: Scheduler,
                     private val facebookInvites: FacebookInvitesOperations,
                     private val streamAdsController: StreamAdsController,
                     private val upsellOperations: InlineUpsellOperations,
                     syncStateStorage: SyncStateStorage,
                     private val suggestedCreatorsOperations: SuggestedCreatorsOperations,
                     private val streamEntityToItemTransformer: StreamEntityToItemTransformer) : TimelineOperations<StreamEntity, StreamItem>(Syncable.SOUNDSTREAM,
                                                                                                                                              streamStorage,
                                                                                                                                              syncInitiator,
                                                                                                                                              scheduler,
                                                                                                                                              syncStateStorage) {

    private fun isSuggestedCreatorsNotification(notificationItemOptional: Optional<out StreamItem>): Boolean {
        return notificationItemOptional.isPresent && notificationItemOptional.get() is StreamItem.SuggestedCreators
    }

    override fun toViewModels(streamEntities: List<StreamEntity>): Single<List<StreamItem>> {
        return streamEntityToItemTransformer.apply(streamEntities)
    }

    fun initialStreamItems(): Single<List<StreamItem>> {
        return removeStalePromotedItemsCommand.toSingle()
                .subscribeOn(scheduler)
                .flatMap { initialTimelineItems(false) }
                .zipWith(initialNotificationItem(),
                         { streamItems, notificationItemOptional ->
                             addNotificationItemToStream(streamItems, notificationItemOptional)
                         })
                .map { this.addUpsellableItem(it.toMutableList()) }
                // Temporary workaround for https://github.com/soundcloud/android-listeners/issues/6807. We should move the below
                // logic to the presenter
                .observeOn(mainThread())
                .doOnSuccess { streamAdsController.insertAds() }
    }

    fun nextPageItems(currentPage: List<StreamItem>): Optional<Observable<List<StreamItem>>> {
        val lastTimestamp = getLastItemTimestamp(currentPage)
        return if (lastTimestamp.isPresent) {
            val nextTimestamp = lastTimestamp.get().time
            Optional.of(pagedTimelineItems(nextTimestamp, false).toObservable())
        } else {
            Optional.absent()
        }
    }

    private fun addUpsellableItem(streamItems: MutableList<StreamItem>): List<StreamItem> {
        if (upsellOperations.shouldDisplayInStream()) {
            getFirstUpsellable(streamItems)?.let {
                streamItems.add(streamItems.indexOf(it) + 1, StreamItem.Upsell)
            }
        }
        return streamItems
    }

    private fun initialNotificationItem(): Single<Optional<StreamItem>> {
        return suggestedCreatorsOperations.suggestedCreators()
                .switchIfEmpty(facebookInvites.creatorInvites())
                .switchIfEmpty(facebookInvites.listenerInvites())
                .map { Optional.of(it) }
                .toSingle(Optional.absent())
    }

    fun updatedStreamItems(): Single<List<StreamItem>> {
        return super.updatedTimelineItems()
                .subscribeOn(scheduler)
                .zipWith(updatedNotificationItem(),
                         { streamItems, notificationItemOptional ->
                             addNotificationItemToStream(streamItems, notificationItemOptional)
                         })
                // Temporary workaround for https://github.com/soundcloud/android-listeners/issues/6807. We should move the below
                // logic to the presenter
                .observeOn(mainThread())
                .doOnSuccess { streamAdsController.insertAds() }
    }

    private fun updatedNotificationItem(): Single<Optional<StreamItem>> {
        return suggestedCreatorsOperations.suggestedCreators()
                .map { Optional.of(it) }
                .toSingle(Optional.absent())
    }

    fun urnsForPlayback(): Single<List<PlayableWithReposter>> {
        return streamStorage.playbackItems().subscribeOn(scheduler).toList()
    }

    fun disableUpsell() {
        upsellOperations.disableInStream()
    }

    fun clearData() {
        upsellOperations.clearData()
    }

    override fun isEmptyResult(result: List<StreamItem>): Boolean {
        return result.isEmpty() || containsOnlyPromotedTrack(result)
    }

    private fun containsOnlyPromotedTrack(result: List<StreamItem>): Boolean {
        return result.size == 1 && result.first().isPromoted
    }

    private fun getFirstPromotedListItem(streamItems: List<StreamItem>): Optional<PlayableItem> {
        return streamItems.firstOrNull { streamItem -> streamItem.playableItem.isPresent && streamItem.playableItem.get().isPromoted }?.playableItem
                ?: Optional.absent()
    }

    override fun getFirstItemTimestamp(items: List<StreamItem>): Optional<Date> {
        return Optional.fromNullable(items.firstOrNull { hasCreatedAt(it) }?.getCreatedAt())
    }

    private fun getFirstUpsellable(streamItems: List<StreamItem>): StreamItem? {
        return streamItems.firstOrNull { it is TrackStreamItem && isHighTierPreview(it.trackItem) }
    }

    override fun getLastItemTimestamp(items: List<StreamItem>): Optional<Date> {
        return Optional.fromNullable(items.lastOrNull { hasCreatedAt(it) }?.getCreatedAt())
    }

    private fun hasCreatedAt(streamItem: StreamItem): Boolean {
        return streamItem is TrackStreamItem || streamItem is PlaylistStreamItem
    }

    private fun StreamItem.getCreatedAt(): Date? {
        return when (this) {
            is TrackStreamItem -> this.createdAt
            is PlaylistStreamItem -> this.createdAt
            else -> null
        }
    }

    fun publishPromotedImpression(streamItems: List<StreamItem>) {
        getFirstPromotedListItem(streamItems).ifPresent { this.publishPromotedImpressionIfNecessary(it) }
    }

    private fun publishPromotedImpressionIfNecessary(promotedItem: PlayableItem) {
        promotedItem.promotedProperties().ifPresent { properties ->
            if (properties.shouldFireImpression()) {
                fireAndForget(markPromotedItemAsStaleCommand.toSingle(promotedItem.adUrn()).subscribeOn(scheduler).toObservable())
                eventBus.publish<TrackingEvent>(EventQueue.TRACKING, PromotedTrackingEvent.forImpression(promotedItem, Screen.STREAM.get()))
                properties.markImpressionFired()
            }
        }
    }

    private fun addNotificationItemToStream(streamItems: List<StreamItem>,
                                            notificationItemOptional: Optional<out StreamItem>): List<StreamItem> {
        val result = newArrayList(streamItems)
        if (isSuggestedCreatorsNotification(notificationItemOptional) || !streamItems.isEmpty()) {
            result.addAll(0, notificationItemOptional.asSet())
        }
        return result
    }

}
