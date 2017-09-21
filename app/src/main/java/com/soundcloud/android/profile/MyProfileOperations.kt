package com.soundcloud.android.profile

import android.support.annotation.VisibleForTesting
import com.soundcloud.android.ApplicationModule
import com.soundcloud.android.Consts
import com.soundcloud.android.model.Association
import com.soundcloud.android.sync.SyncInitiatorBridge
import com.soundcloud.android.tracks.Track
import com.soundcloud.android.tracks.TrackRepository
import com.soundcloud.android.users.UserAssociation
import com.soundcloud.android.users.UserAssociationStorage
import com.soundcloud.android.utils.OpenForTesting
import com.soundcloud.android.utils.enrichItemsWithProperties
import io.reactivex.Scheduler
import io.reactivex.Single
import io.reactivex.functions.BiFunction
import javax.inject.Inject
import javax.inject.Named

@OpenForTesting
class MyProfileOperations
@Inject
constructor(
        private val postsStorage: PostsStorage,
        private val syncInitiatorBridge: SyncInitiatorBridge,
        private val userAssociationStorage: UserAssociationStorage,
        @param:Named(ApplicationModule.RX_HIGH_PRIORITY) private val scheduler: Scheduler,
        private val trackRepository: TrackRepository) {

    fun followingsUserAssociations(): Single<List<UserAssociation>> {
        return loadFollowingUserAssociationsFromStorage()
                .filter { list -> !list.isEmpty() }
                .switchIfEmpty(
                        Single
                                .defer {
                                    syncInitiatorBridge
                                            .refreshFollowings()
                                            .flatMap { loadFollowingUserAssociationsFromStorage() }
                                }
                                .toMaybe())
                .toSingle(emptyList())
    }

    private fun loadFollowingUserAssociationsFromStorage(): Single<List<UserAssociation>> = userAssociationStorage.followedUserAssociations().subscribeOn(scheduler)

    fun lastPublicPostedTrack(): Single<LastPostedTrack> {
        return postsStorage
                .loadPostedTracksSortedByDateDesc()
                .flatMap { posts ->
                    enrichItemsWithProperties(posts,
                                              trackRepository.fromUrns(posts.map { it.urn }),
                                              BiFunction { track: Track, association: Association -> track to association })
                }
                .map { it.first { !it.first.isPrivate } }
                .map { LastPostedTrack.create(it.first.urn(), it.second.createdAt, it.first.permalinkUrl()) }
    }

    companion object {

        @VisibleForTesting
        const val PAGE_SIZE = Consts.LIST_PAGE_SIZE
    }
}
