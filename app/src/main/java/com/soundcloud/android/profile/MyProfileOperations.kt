package com.soundcloud.android.profile

import android.support.annotation.VisibleForTesting
import com.soundcloud.android.ApplicationModule
import com.soundcloud.android.Consts
import com.soundcloud.android.model.Urn
import com.soundcloud.android.rx.RxJava
import com.soundcloud.android.sync.SyncInitiator
import com.soundcloud.android.sync.SyncInitiatorBridge
import com.soundcloud.android.tracks.TrackRepository
import com.soundcloud.android.users.UserAssociation
import com.soundcloud.android.users.UserAssociationStorage
import com.soundcloud.android.utils.OpenForTesting
import com.soundcloud.java.collections.Iterables.getLast
import com.soundcloud.rx.Pager
import io.reactivex.Scheduler
import io.reactivex.Single
import io.reactivex.functions.Function
import javax.inject.Inject
import javax.inject.Named

@OpenForTesting
class MyProfileOperations
@Inject
constructor(
        private val postsStorage: PostsStorage,
        private val syncInitiatorBridge: SyncInitiatorBridge,
        private val syncInitiator: SyncInitiator,
        private val userAssociationStorage: UserAssociationStorage,
        @param:Named(ApplicationModule.RX_HIGH_PRIORITY) private val scheduler: Scheduler,
        private val trackRepository: TrackRepository) {

    fun followings(): Single<List<Following>> {
        return pagedFollowingsFromPosition(Consts.NOT_SET.toLong())
                .subscribeOn(scheduler)
                .flatMap { list -> if (list.isEmpty()) updatedFollowings() else Single.just(list) }
    }

    fun followingsPagingFunction(): Pager.PagingFunction<List<Following>> {
        return Pager.PagingFunction { result ->
            if (result.size < PAGE_SIZE) {
                Pager.finish<List<Following>>()
            } else {
                RxJava.toV1Observable(pagedFollowingsFromPosition(getLast<Following>(result).userAssociation().position()).subscribeOn(scheduler))
            }
        }
    }

    fun updatedFollowings(): Single<List<Following>> {
        return syncInitiatorBridge.refreshFollowings()
                .flatMap { pagedFollowingsFromPosition(Consts.NOT_SET.toLong()).subscribeOn(scheduler) }
    }

    fun followingsUserAssociations(): Single<List<UserAssociation>> {
        return loadFollowingUserAssociationsFromStorage()
                .filter { list -> !list.isEmpty() }
                .switchIfEmpty(Single.defer {
                    syncInitiatorBridge.refreshFollowings()
                            .flatMap { o -> loadFollowingUserAssociationsFromStorage() }
                }
                                       .toMaybe())
                .toSingle(emptyList<UserAssociation>())
    }

    private fun loadFollowingUserAssociationsFromStorage(): Single<List<UserAssociation>> {
        return userAssociationStorage.followedUserAssociations().subscribeOn(scheduler)
    }

    private fun pagedFollowingsFromPosition(fromPosition: Long): Single<List<Following>> {
        return userAssociationStorage
                .followedUserUrns(PAGE_SIZE, fromPosition)
                .flatMap(syncAndReloadFollowings(PAGE_SIZE, fromPosition))
    }

    private fun syncAndReloadFollowings(pageSize: Int,
                                        fromPosition: Long): Function<List<Urn>, Single<List<Following>>> {
        return Function { urns ->
            if (urns.isEmpty()) {
                Single.just(emptyList<Following>())
            } else {
                syncInitiator.batchSyncUsers(urns).flatMap { userAssociationStorage.followedUsers(pageSize, fromPosition).subscribeOn(scheduler) }
            }
        }
    }

    fun lastPublicPostedTrack(): Single<LastPostedTrack> {
        return postsStorage.loadPostedTracksSortedByDateDesc().flatMap { post ->
            trackRepository.fromUrns(post.map { it.first })
                    .map { tracks ->
                        tracks.flatMap {
                            post.filter { tracks.containsKey(it.first) }
                                    .map { tracks[it.first]!! to it.second }
                        }
                                .filter { !it.first.isPrivate }
                                .map { LastPostedTrack.create(it.first.urn(), it.second, it.first.permalinkUrl()) }
                                .first()
                    }
        }
                .subscribeOn(scheduler)
    }

    companion object {

        @VisibleForTesting
        const val PAGE_SIZE = Consts.LIST_PAGE_SIZE
    }
}
