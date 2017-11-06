package com.soundcloud.android.stream

import com.soundcloud.android.ApplicationModule
import com.soundcloud.android.Consts
import com.soundcloud.android.ads.StreamAdsController
import com.soundcloud.android.facebookinvites.FacebookInvitesOperations
import com.soundcloud.android.sync.NewSyncOperations
import com.soundcloud.android.sync.Syncable
import com.soundcloud.android.upsell.InlineUpsellOperations
import io.reactivex.Completable
import io.reactivex.Maybe
import io.reactivex.Scheduler
import io.reactivex.Single
import java.util.Date
import javax.inject.Inject
import javax.inject.Named

class StreamUniflowOperations
@Inject
constructor(private val streamStorage: StreamStorage,
            private val syncOperations: NewSyncOperations,
            private val removeStalePromotedItemsCommand: RemoveStalePromotedItemsCommand,
            private val streamEntityToItemTransformer: StreamEntityToItemTransformer,
            private val upsellOperations: InlineUpsellOperations,
            private val streamAdsController: StreamAdsController,
            private val facebookInvitesOperations: FacebookInvitesOperations,
            @param:Named(ApplicationModule.RX_HIGH_PRIORITY) private val scheduler: Scheduler) {

    fun initialStreamItems(): Single<List<StreamItem>> {
        return Completable.mergeArray(removeStalePromotedItemsCommand.toCompletable(),
                                      syncOperations.lazySyncIfStale(Syncable.SOUNDSTREAM).toCompletable())
                .andThen(streamStorage.timelineItems(Consts.LIST_PAGE_SIZE).toList().flatMap { streamEntityToItemTransformer.apply(it) })
                .map { this.addUpsellableItem(it.toMutableList()) }
                .doOnSuccess { streamAdsController.insertAds() }
                .subscribeOn(scheduler)
    }

    fun updatedStreamItems(): Single<List<StreamItem>> {
        return syncOperations.failSafeSync(Syncable.SOUNDSTREAM)
                .flatMap { streamStorage.timelineItems(Consts.LIST_PAGE_SIZE).toList().flatMap { streamEntityToItemTransformer.apply(it) } }
                .doOnSuccess { streamAdsController.insertAds() }
                .subscribeOn(scheduler)
    }

    fun nextPageItems(currentPage: List<StreamItem>): Single<List<StreamItem>>? {
        return getLastItemTimestamp(currentPage)?.let { pagedTimelineItems(it.time) }?.subscribeOn(scheduler)
    }

    fun initialNotificationItem(): Maybe<StreamItem> {
        return facebookInvitesOperations.creatorInvites().switchIfEmpty(facebookInvitesOperations.listenerInvites())
                .subscribeOn(scheduler)
    }

    private fun pagedTimelineItems(nextTimestamp: Long): Single<List<StreamItem>> {
        return syncOperations.lazySyncIfStale(Syncable.SOUNDSTREAM).flatMap {
            streamStorage.timelineItemsBefore(nextTimestamp, Consts.LIST_PAGE_SIZE).toList().flatMap { streamEntityToItemTransformer.apply(it) }
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

    private fun getFirstUpsellable(streamItems: List<StreamItem>): StreamItem? {
        return streamItems.firstOrNull { it.isUpsellableTrack }
    }

    private fun getLastItemTimestamp(items: List<StreamItem>): Date? {
        return items.lastOrNull { it.createdAt != null }?.createdAt
    }
}
